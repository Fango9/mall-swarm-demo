package cn.fango.mall.admin.messaging;

import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import cn.fango.mall.admin.config.HotStockReservationProperties;
import cn.fango.mall.admin.redis.HotStockReservationRelayStateLuaExecutor;
import cn.fango.mall.common.messaging.HotStockReservationMessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis Stream 到 RabbitMQ 的可靠热点库存预占用 Relay。
 *
 * <p>本 Relay 遵循“先 Publisher Confirm，后 XACK”顺序。若进程在 Confirm
 * 成功后、XACK 前崩溃，Stream 记录会被再次发布；因此消息按至少一次投递，
 * 下游必须以 reservationNo 幂等。</p>
 */
@Component
public class HotStockReservationRelay {

    /**
     * Relay 日志记录器。
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(HotStockReservationRelay.class);

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * RabbitMQ 消息模板。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 热点库存预占用运行配置。
     */
    private final HotStockReservationProperties properties;

    /** Redis Relay 状态条件转移执行器。 */
    private final HotStockReservationRelayStateLuaExecutor relayStateExecutor;

    /**
     * 创建热点库存预占用 Relay。
     *
     * @param stringRedisTemplate Redis 字符串操作模板
     * @param rabbitTemplate RabbitMQ 消息模板
     * @param properties 热点库存预占用运行配置
     * @param relayStateExecutor Redis Relay 状态条件转移执行器
     */
    public HotStockReservationRelay(
            StringRedisTemplate stringRedisTemplate,
            RabbitTemplate rabbitTemplate,
            HotStockReservationProperties properties,
            HotStockReservationRelayStateLuaExecutor relayStateExecutor
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.relayStateExecutor = relayStateExecutor;
    }

    /**
     * 扫描当前 Relay 消费者的 pending 事件和新事件，并可靠投递到 RabbitMQ。
     */
    @Scheduled(
            fixedDelayString =
                    "${mall.hot-stock-reservation.relay-fixed-delay-millis:200}"
    )
    public void relay() {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            ensureConsumerGroup();
            relayStalePendingRecords();
            relayPendingRecords();
            relayNewRecords();
        } catch (RuntimeException exception) {
            LOGGER.debug("热点库存预约 Relay 暂不可用，后续扫描会重试", exception);
        }
    }

    /**
     * 创建 Redis Stream 消费者组。
     *
     * <p>Stream 尚未生成首条预占用事件时创建可能失败；下一轮扫描会重试。
     * 消费者组已经存在时 Redis 也会拒绝重复创建，此时同样可安全继续。</p>
     */
    private void ensureConsumerGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(
                    HotSkuStockCacheKeys.reservationStreamKey(),
                    // 新组必须从已有第一条记录开始消费；从 latest 建组会遗漏
                    // “Lua 已写 Stream、Relay 尚未首次启动”窗口中的预占用事件。
                    ReadOffset.from("0-0"),
                    properties.getRelayConsumerGroup()
            );
        } catch (RuntimeException ignored) {
            // 组已存在或 Stream 尚未创建；后续 read 会决定是否可继续处理。
        }
    }

    /**
     * 接管其他 Relay 实例遗留且空闲超阈值的 pending 记录。
     *
     * <p>Spring Data Redis 当前版本没有直接暴露 XAUTOCLAIM，因此使用
     * XPENDING 查询加 XCLAIM 接管的等价实现。接管后仍由
     * {@link #publishThenAcknowledge(MapRecord)} 遵循 Confirm 后 XACK 的规则。</p>
     */
    private void relayStalePendingRecords() {
        PendingMessages pendingMessages =
                stringRedisTemplate.opsForStream().pending(
                        HotSkuStockCacheKeys.reservationStreamKey(),
                        properties.getRelayConsumerGroup(),
                        Range.unbounded(),
                        properties.getRelayBatchSize()
                );

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        List<RecordId> staleRecordIds = new ArrayList<>();
        Duration minimumIdleTime = Duration.ofMillis(
                properties.getRelayClaimMinIdleMillis()
        );

        for (PendingMessage pendingMessage : pendingMessages) {
            if (properties.getRelayConsumerName().equals(pendingMessage.getConsumerName())) {
                continue;
            }

            if (pendingMessage.getElapsedTimeSinceLastDelivery()
                    .compareTo(minimumIdleTime) >= 0) {
                staleRecordIds.add(pendingMessage.getId());
            }
        }

        if (staleRecordIds.isEmpty()) {
            return;
        }

        List<MapRecord<String, Object, Object>> claimedRecords =
                stringRedisTemplate.opsForStream().claim(
                        HotSkuStockCacheKeys.reservationStreamKey(),
                        properties.getRelayConsumerGroup(),
                        properties.getRelayConsumerName(),
                        minimumIdleTime,
                        staleRecordIds.toArray(new RecordId[0])
                );

        relayRecords(claimedRecords);
    }

    /**
     * 读取当前消费者在崩溃边界留下的 pending 记录。
     */
    private void relayPendingRecords() {
        List<MapRecord<String, Object, Object>> records =
                stringRedisTemplate.opsForStream().read(
                        Consumer.from(
                                properties.getRelayConsumerGroup(),
                                properties.getRelayConsumerName()
                        ),
                        StreamReadOptions.empty().count(properties.getRelayBatchSize()),
                        StreamOffset.create(
                                HotSkuStockCacheKeys
                                        .reservationStreamKey(),
                                ReadOffset.from("0-0")
                        )
                );

        relayRecords(records);
    }

    /**
     * 读取消费者组中此前从未分配的新记录。
     */
    private void relayNewRecords() {
        List<MapRecord<String, Object, Object>> records =
                stringRedisTemplate.opsForStream().read(
                        Consumer.from(
                                properties.getRelayConsumerGroup(),
                                properties.getRelayConsumerName()
                        ),
                        StreamReadOptions.empty()
                                .count(properties.getRelayBatchSize()),
                        StreamOffset.create(
                                HotSkuStockCacheKeys
                                        .reservationStreamKey(),
                                ReadOffset.lastConsumed()
                        )
                );

        relayRecords(records);
    }

    /**
     * 逐条发布 Redis Stream 记录。
     *
     * @param records 本次读取到的 Stream 记录；可能为 {@code null}
     */
    private void relayRecords(
            List<MapRecord<String, Object, Object>> records
    ) {
        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            publishThenAcknowledge(record);
        }
    }

    /**
     * 发布一条预占用事件，并且仅在 Confirm 成功且未被退回后 XACK。
     *
     * @param record Redis Stream 预占用事件
     */
    void publishThenAcknowledge(MapRecord<String, Object, Object> record) {
        Object reservationNoValue = record.getValue().get("reservationNo");
        Object eventJsonValue = record.getValue().get("eventJson");

        if (reservationNoValue == null || eventJsonValue == null) {
            LOGGER.error("热点库存预约 Stream 记录缺少必要字段，recordId={}", record.getId());
            return;
        }

        String reservationNo = reservationNoValue.toString();
        String eventJson = eventJsonValue.toString();
        CorrelationData correlationData = new CorrelationData(
                reservationNo + ":" + record.getId().getValue()
        );

        try {
            rabbitTemplate.send(
                    properties.getRelayExchange(),
                    properties.getRelayRoutingKey(),
                    createPersistentMessage(reservationNo, eventJson),
                    correlationData
            );

            CorrelationData.Confirm confirm = correlationData.getFuture().get(properties.getRelayConfirmTimeoutMillis(), TimeUnit.MILLISECONDS);

            if (confirm == null
                    || !confirm.isAck()
                    || correlationData.getReturned() != null) {
                return;
            }

            Long relayStateResult = relayStateExecutor.markRelayed(reservationNo);
            if (relayStateResult == null || relayStateResult < 0) {
                LOGGER.warn("热点库存预约 Relay 状态未推进，保留 Stream pending 记录，reservationNo={}, luaResult={}",
                        reservationNo, relayStateResult);
                return;
            }
            stringRedisTemplate.opsForStream().acknowledge(
                    HotSkuStockCacheKeys.reservationStreamKey(),
                    properties.getRelayConsumerGroup(),
                    record.getId()
            );
        } catch (Exception exception) {
            LOGGER.warn(
                    "热点库存预约 Relay 发布失败，保留 Stream pending 记录等待重试，reservationNo={}",
                    reservationNo,
                    exception
            );
        }
    }

    /**
     * 创建持久化 RabbitMQ 消息。
     *
     * @param reservationNo 热点库存预占用幂等键
     * @param eventJson 预占用事件 JSON
     * @return RabbitMQ 持久化消息
     */
    private Message createPersistentMessage(
            String reservationNo,
            String eventJson
    ) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(
                MessageProperties.CONTENT_TYPE_JSON
        );
        messageProperties.setContentEncoding(
                StandardCharsets.UTF_8.name()
        );
        messageProperties.setDeliveryMode(
                MessageDeliveryMode.PERSISTENT
        );
        messageProperties.setMessageId(reservationNo);
        messageProperties.setCorrelationId(reservationNo);

        return new Message(
                eventJson.getBytes(StandardCharsets.UTF_8),
                messageProperties
        );
    }
}

package cn.fango.mall.admin.messaging;

import cn.fango.mall.admin.api.HotStockReservationStatus;
import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import cn.fango.mall.admin.service.HotStockReservationPersistenceService;
import cn.fango.mall.common.event.HotStockReservationEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.messaging.HotStockReservationMessageConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ 热点库存预占用消费者。
 *
 * <p>监听器正常返回后，Spring AMQP 才确认 RabbitMQ 消息。MySQL 落库或 Redis
 * 状态更新失败时抛出异常，消息保持可重试，不能被提前确认。</p>
 */
@Component
public class HotStockReservationMessageListener {

    /**
     * JSON 序列化与反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 热点库存预占用 MySQL 持久化服务。
     */
    private final HotStockReservationPersistenceService
            persistenceService;

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建热点库存预占用 RabbitMQ 消费者。
     *
     * @param objectMapper JSON 序列化与反序列化工具
     * @param persistenceService 热点库存预占用 MySQL 持久化服务
     * @param stringRedisTemplate Redis 字符串操作模板
     */
    public HotStockReservationMessageListener(
            ObjectMapper objectMapper,
            HotStockReservationPersistenceService persistenceService,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.objectMapper = objectMapper;
        this.persistenceService = persistenceService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 批量幂等落库热点库存预占用，并在 MySQL 提交后更新 Redis 预占用状态。
     *
     * <p>任何一条事件落库失败都会让整个批次抛出异常。RabbitMQ 不确认该批次，
     * 由 listener retry 或 DLQ 统一恢复，避免批内成功和失败消息被错误拆分。</p>
     *
     * @param messages Relay 发布的一批热点库存预占用消息
     */
    @RabbitListener(
            queues = HotStockReservationMessageConstants.QUEUE,
            containerFactory =
                    "hotStockReservationBatchListenerContainerFactory"
    )
    public void consume(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new ApiException(StockReservationErrorCode.HOT_STOCK_RESERVATION_EVENT_INVALID);
        }

        List<HotStockReservationEvent> events = new ArrayList<>();
        for (Message message : messages) {
            events.add(deserializeEvent(message.getBody()));
        }

        persistenceService.persistBatch(events);

        // persistBatch 返回时 @Transactional 已提交；状态写入失败会触发整批消息重试。
        for (HotStockReservationEvent event : events) {
            stringRedisTemplate.opsForHash().put(
                    HotSkuStockCacheKeys.reservationKey(
                            event.reservationNo()
                    ),
                    "status",
                    HotStockReservationStatus.PERSISTED.name()
            );
        }
    }

    /**
     * 将 RabbitMQ JSON 负载还原为热点库存预占用事件。
     *
     * @param payload RabbitMQ 消息体
     * @return 已反序列化的热点库存预占用事件
     */
    private HotStockReservationEvent deserializeEvent(byte[] payload) {
        try {
            return objectMapper.readValue(
                    new String(payload, StandardCharsets.UTF_8),
                    HotStockReservationEvent.class
            );
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_RESERVATION_EVENT_INVALID,
                    exception
            );
        }
    }
}

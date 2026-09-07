package cn.fango.mall.admin.messaging;

import cn.fango.mall.admin.api.HotStockReservationStatus;
import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import cn.fango.mall.admin.mapper.PmsProductOutboxMapper;
import cn.fango.mall.common.event.HotStockOrderConfirmedEvent;
import cn.fango.mall.common.event.HotStockOrderFailedEvent;
import cn.fango.mall.common.messaging.HotStockOrderMessageConstants;
import cn.fango.mall.common.messaging.ProductCacheMessageConstants;
import cn.fango.mall.mbg.model.PmsOutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Admin 商品变更 Outbox 事件的 RabbitMQ 发布器。
 *
 * <p>事件先随商品本地事务写入数据库；发布器定时扫描待发布事件，
 * 仅在 RabbitMQ Confirm 成功后将其标记为已发布。</p>
 */
@Component
public class ProductOutboxEventPublisher {

    /**
     * 当前发布器支持的商品变更事件类型。
     */
    private static final String PRODUCT_CHANGED_EVENT_TYPE = "PRODUCT_CHANGED";

    private static final String HOT_STOCK_ORDER_CONFIRMED_EVENT_TYPE = "HOT_STOCK_ORDER_CONFIRMED";

    /**
     * 热点库存预占用失败后，通知 Portal 将订单改为库存失败的 Outbox 事件类型。
     */
    private static final String HOT_STOCK_ORDER_FAILED_EVENT_TYPE = "HOT_STOCK_ORDER_FAILED";

    /**
     * 商品 Outbox 发布状态数据访问对象。
     */
    private final PmsProductOutboxMapper pmsProductOutboxMapper;

    /**
     * RabbitMQ 消息模板。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * JSON 序列化与反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 单次扫描最大的发布事件数量。
     */
    private final int publishBatchSize;

    /**
     * 发布失败后的重试等待毫秒数。
     */
    private final long retryDelayMillis;

    /**
     * 创建商品 Outbox 事件发布器。
     *
     * @param pmsProductOutboxMapper 商品 Outbox 发布状态数据访问对象
     * @param rabbitTemplate RabbitMQ 消息模板
     * @param publishBatchSize 单次扫描最大的发布事件数量
     * @param retryDelayMillis 发布失败后的重试等待毫秒数
     */
    public ProductOutboxEventPublisher(
            PmsProductOutboxMapper pmsProductOutboxMapper,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate,
            @Value("${mall.outbox.publish-batch-size}")
            int publishBatchSize,
            @Value("${mall.outbox.retry-delay-ms}")
            long retryDelayMillis
    ) {        if (publishBatchSize <= 0 || retryDelayMillis <= 0) {
            throw new IllegalArgumentException("mall.outbox 发布参数必须大于 0");
        }

        this.pmsProductOutboxMapper = pmsProductOutboxMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.publishBatchSize = publishBatchSize;
        this.retryDelayMillis = retryDelayMillis;

        this.rabbitTemplate.setConfirmCallback(this::handleConfirm);
        this.rabbitTemplate.setReturnsCallback(this::handleReturnedMessage);
    }

    /**
     * 定时扫描并发布当前允许重试的待发布商品变更事件。
     */
    @Scheduled(fixedDelayString = "${mall.outbox.publish-fixed-delay-ms}")
    public void publishPendingEvents() {
        List<PmsOutboxEvent> events = pmsProductOutboxMapper.selectPendingForPublish(
                        new Date(),
                        publishBatchSize
                );

        for (PmsOutboxEvent event : events) {
            publishEvent(event);
        }
    }

    /**
     * 向商品变更交换机发送一条持久化消息。
     *
     * @param event 待发布的商品 Outbox 事件
     */
    private void publishEvent(PmsOutboxEvent event) {
        if (!isSupportedEvent(event)) {
            markPublishFailed(event == null ? null : event.getEventId(), "不支持的 Outbox 事件类型");
            return;
        }

        // 仅针对热点 sku 的成功或失败事件
        if (!isReadyToPublish(event)) {
            return;
        }

        try {
            Message message = createMessage(event);

            rabbitTemplate.send(
                    resolveExchange(event),
                    resolveRoutingKey(event),
                    message,
                    new CorrelationData(event.getEventId())
            );
        } catch (RuntimeException exception) {
            markPublishFailed(event.getEventId(), exception.getMessage());
        }
    }

    /**
     * 创建持久化的商品变更事件消息。
     *
     * @param event 待发布的商品 Outbox 事件
     * @return RabbitMQ 持久化消息
     */
    private Message createMessage(PmsOutboxEvent event) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.getEventId());
        properties.setCorrelationId(event.getEventId());

        byte[] body = event.getPayload().getBytes(StandardCharsets.UTF_8);

        return new Message(body, properties);
    }

    /**
     * 处理 RabbitMQ Publisher Confirm 回调。
     *
     * @param correlationData 本次发送的关联数据
     * @param ack Broker 是否确认接收
     * @param cause Broker 未确认时的失败原因
     */
    private void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null || !StringUtils.hasText(correlationData.getId())) {
            return;
        }

        if (ack && correlationData.getReturned() == null) {
            pmsProductOutboxMapper.markPublished(
                    correlationData.getId(),
                    new Date()
            );
            return;
        }

        if (!ack) {
            markPublishFailed(correlationData.getId(), cause);
        }
    }

    /**
     * 处理消息无法路由到任何队列时的回调。
     *
     * @param returnedMessage 未路由消息的描述
     */
    private void handleReturnedMessage(ReturnedMessage returnedMessage) {
        String eventId = returnedMessage.getMessage()
                .getMessageProperties()
                .getCorrelationId();

        markPublishFailed(eventId, "消息无法路由：" + returnedMessage.getReplyText());
    }

    /**
     * 记录发布失败并安排后续重试。
     *
     * @param eventId Outbox 事件唯一标识
     * @param errorMessage 发布失败原因
     */
    private void markPublishFailed(String eventId, String errorMessage) {
        if (!StringUtils.hasText(eventId)) {
            return;
        }

        Date nextRetryAt = new Date(
                System.currentTimeMillis() + retryDelayMillis
        );

        pmsProductOutboxMapper.markPublishFailed(
                eventId,
                nextRetryAt,
                truncateError(errorMessage)
        );
    }

    /**
     * 判断事件是否为当前发布器支持的类型。
     *
     * @param event 待判断的 Admin Outbox 事件
     * @return 支持发布时返回 {@code true}
     */
    private boolean isSupportedEvent(PmsOutboxEvent event) {
        return event != null
                && StringUtils.hasText(event.getEventId())
                && StringUtils.hasText(event.getPayload())
                && (PRODUCT_CHANGED_EVENT_TYPE.equals(
                event.getEventType()
        )
                || HOT_STOCK_ORDER_CONFIRMED_EVENT_TYPE.equals(
                event.getEventType()
        )
                || HOT_STOCK_ORDER_FAILED_EVENT_TYPE.equals(
                event.getEventType()
        ));
    }

    /**
     * 根据事件类型确定目标交换机。
     *
     * @param event 已校验的 Admin Outbox 事件
     * @return RabbitMQ 目标交换机
     */
    private String resolveExchange(PmsOutboxEvent event) {
        if (HOT_STOCK_ORDER_CONFIRMED_EVENT_TYPE.equals(event.getEventType())
                || HOT_STOCK_ORDER_FAILED_EVENT_TYPE.equals(event.getEventType())) {
            return HotStockOrderMessageConstants.EXCHANGE;
        }

        return ProductCacheMessageConstants.PRODUCT_CACHE_EXCHANGE;
    }

    /**
     * 根据事件类型确定目标路由键。
     *
     * @param event 已校验的 Admin Outbox 事件
     * @return RabbitMQ 目标路由键
     */
    private String resolveRoutingKey(PmsOutboxEvent event) {
        if (HOT_STOCK_ORDER_CONFIRMED_EVENT_TYPE.equals(event.getEventType())) {

            return HotStockOrderMessageConstants.CONFIRMED_ROUTING_KEY;
        }

        if (HOT_STOCK_ORDER_FAILED_EVENT_TYPE.equals(event.getEventType())) {
            return HotStockOrderMessageConstants.FAILED_ROUTING_KEY;
        }

        return ProductCacheMessageConstants.PRODUCT_CHANGED_ROUTING_KEY;
    }

    /**
     * 将发布失败原因限制在数据库字段允许的长度内。
     *
     * @param errorMessage 原始失败原因
     * @return 最长 500 个字符的失败原因
     */
    private String truncateError(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return "RabbitMQ 发布失败";
        }

        if (errorMessage.length() <= 500) {
            return errorMessage;
        }

        return errorMessage.substring(0, 500);
    }

    /**
     * 判断 Outbox 事件是否已满足发布前置条件。
     *
     * <p>热点订单的成功或失败通知，都必须等待 Redis 中的预占用状态已终结。
     * 这样 Portal 不会在 Redis 状态尚未确定时提前修改订单状态。</p>
     *
     * @param event 已校验的 Admin Outbox 事件
     * @return 可以发送 RabbitMQ 时返回 {@code true}
     */
    private boolean isReadyToPublish(PmsOutboxEvent event) {
        String expectedStatus;
        String orderSn;

        try {
            if (HOT_STOCK_ORDER_CONFIRMED_EVENT_TYPE.equals(event.getEventType())) {
                HotStockOrderConfirmedEvent confirmedEvent = objectMapper.readValue(
                                event.getPayload(),
                                HotStockOrderConfirmedEvent.class
                        );
                expectedStatus = HotStockReservationStatus.CONFIRMED.name();
                orderSn = confirmedEvent.orderSn();
            } else if (HOT_STOCK_ORDER_FAILED_EVENT_TYPE.equals(event.getEventType())) {
                HotStockOrderFailedEvent failedEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                HotStockOrderFailedEvent.class
                        );
                expectedStatus = HotStockReservationStatus.FAILED.name();
                orderSn = failedEvent.orderSn();
            } else {
                return true;
            }

            Object actualStatus = stringRedisTemplate.opsForHash().get(
                    HotSkuStockCacheKeys.reservationKey(orderSn),
                    "status"
            );

            if (expectedStatus.equals(actualStatus)) {
                return true;
            }

            markPublishFailed(
                    event.getEventId(),
                    "热点库存预占用尚未进入最终状态：" + expectedStatus
            );
            return false;
        } catch (JsonProcessingException | RuntimeException exception) {
            markPublishFailed(
                    event.getEventId(),
                    "无法确认热点库存 Redis 状态：" + exception.getMessage()
            );
            return false;
        }
    }

}

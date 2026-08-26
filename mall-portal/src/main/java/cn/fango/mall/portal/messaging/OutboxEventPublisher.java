package cn.fango.mall.portal.messaging;

import cn.fango.mall.common.messaging.OrderMessageConstants;
import cn.fango.mall.mbg.mapper.OmsOutboxEventMapper;
import cn.fango.mall.mbg.model.OmsOutboxEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * 事务外盒事件 RabbitMQ 发布器。
 */
@Component
public class OutboxEventPublisher {

    private static final String ORDER_CREATED_EVENT_TYPE =
            "ORDER_CREATED";

    /**
     * 事务外盒事件数据访问对象。
     */
    private final OmsOutboxEventMapper omsOutboxEventMapper;

    /**
     * RabbitMQ 消息模板。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 单次扫描最大的发布事件数量。
     */
    private final int publishBatchSize;

    /**
     * 发布失败后的重试等待毫秒数。
     */
    private final long retryDelayMillis;

    /**
     * 创建 Outbox 发布器。
     *
     * @param omsOutboxEventMapper 事务外盒事件数据访问对象
     * @param rabbitTemplate RabbitMQ 消息模板
     * @param publishBatchSize 单次扫描最大的发布事件数量
     * @param retryDelayMillis 发布失败后的重试等待毫秒数
     */
    public OutboxEventPublisher(
            OmsOutboxEventMapper omsOutboxEventMapper,
            RabbitTemplate rabbitTemplate,
            @Value("${mall.outbox.publish-batch-size}")
            int publishBatchSize,
            @Value("${mall.outbox.retry-delay-ms}")
            long retryDelayMillis
    ) {
        if (publishBatchSize <= 0 || retryDelayMillis <= 0) {
            throw new IllegalArgumentException(
                    "mall.outbox 发布参数必须大于 0"
            );
        }

        this.omsOutboxEventMapper = omsOutboxEventMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.publishBatchSize = publishBatchSize;
        this.retryDelayMillis = retryDelayMillis;

        this.rabbitTemplate.setConfirmCallback(this::handleConfirm);
        this.rabbitTemplate.setReturnsCallback(this::handleReturnedMessage);
    }

    /**
     * 定时扫描并发布当前允许重试的待发布事件。
     */
    @Scheduled(
            fixedDelayString = "${mall.outbox.publish-fixed-delay-ms}"
    )
    public void publishPendingEvents() {
        List<OmsOutboxEvent> events =
                omsOutboxEventMapper.selectPendingForPublish(
                        new Date(),
                        publishBatchSize
                );

        for (OmsOutboxEvent event : events) {
            publishEvent(event);
        }
    }

    /**
     * 向订单事件交换机发送一条持久化消息。
     *
     * @param event 待发布的 Outbox 事件
     */
    private void publishEvent(OmsOutboxEvent event) {
        if (!isOrderCreatedEvent(event)) {
            markPublishFailed(
                    event.getEventId(),
                    "不支持的 Outbox 事件类型"
            );
            return;
        }

        try {
            Message message = createMessage(event);

            rabbitTemplate.send(
                    OrderMessageConstants.ORDER_EXCHANGE,
                    OrderMessageConstants.ORDER_CREATED_ROUTING_KEY,
                    message,
                    new CorrelationData(event.getEventId())
            );
        } catch (RuntimeException exception) {
            markPublishFailed(
                    event.getEventId(),
                    exception.getMessage()
            );
        }
    }

    /**
     * 创建持久化的订单创建事件消息。
     *
     * @param event 待发布的 Outbox 事件
     * @return RabbitMQ 消息
     */
    private Message createMessage(OmsOutboxEvent event) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.getEventId());
        properties.setCorrelationId(event.getEventId());

        byte[] body = event.getPayload()
                .getBytes(StandardCharsets.UTF_8);

        return new Message(body, properties);
    }

    /**
     * 处理 RabbitMQ Publisher Confirm 回调。
     *
     * @param correlationData 本次发送的关联数据
     * @param ack Broker 是否确认接收
     * @param cause Broker 未确认时的失败原因
     */
    private void handleConfirm(
            CorrelationData correlationData,
            boolean ack,
            String cause
    ) {
        if (correlationData == null
                || !StringUtils.hasText(correlationData.getId())) {
            return;
        }

        if (ack && correlationData.getReturned() == null) {
            omsOutboxEventMapper.markPublished(
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
    private void handleReturnedMessage(
            ReturnedMessage returnedMessage
    ) {
        String eventId = returnedMessage.getMessage()
                .getMessageProperties()
                .getCorrelationId();

        markPublishFailed(
                eventId,
                "消息无法路由："
                        + returnedMessage.getReplyText()
        );
    }

    /**
     * 记录失败并设置下一次重试时间。
     *
     * @param eventId Outbox 事件 UUID
     * @param errorMessage 失败原因
     */
    private void markPublishFailed(
            String eventId,
            String errorMessage
    ) {
        if (!StringUtils.hasText(eventId)) {
            return;
        }

        Date nextRetryAt = new Date(
                System.currentTimeMillis() + retryDelayMillis
        );

        omsOutboxEventMapper.markPublishFailed(
                eventId,
                nextRetryAt,
                truncateError(errorMessage)
        );
    }

    /**
     * 判断事件是否是当前发布器支持的订单创建事件。
     *
     * @param event 待判断的 Outbox 事件
     * @return 是订单创建事件时返回 true
     */
    private boolean isOrderCreatedEvent(OmsOutboxEvent event) {
        return event != null
                && StringUtils.hasText(event.getEventId())
                && StringUtils.hasText(event.getPayload())
                && ORDER_CREATED_EVENT_TYPE.equals(
                event.getEventType()
        );
    }

    /**
     * 将错误信息限制在数据库字段允许的长度内。
     *
     * @param errorMessage 原始错误信息
     * @return 最长 500 字符的错误信息
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
}
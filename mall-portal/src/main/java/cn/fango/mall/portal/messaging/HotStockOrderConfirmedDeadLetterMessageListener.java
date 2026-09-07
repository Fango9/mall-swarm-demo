package cn.fango.mall.portal.messaging;

import cn.fango.mall.common.event.HotStockOrderConfirmedEvent;
import cn.fango.mall.common.messaging.HotStockOrderMessageConstants;
import cn.fango.mall.portal.service.HotStockOrderConfirmedEventConsumerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 热点库存预占用已确认事件 Portal 死信消息监听器。
 */
@Component
public class HotStockOrderConfirmedDeadLetterMessageListener {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HotStockOrderConfirmedDeadLetterMessageListener.class
    );

    /**
     * JSON 序列化与反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 热点库存确认事件消费服务。
     */
    private final HotStockOrderConfirmedEventConsumerService
            consumerService;

    /**
     * 创建热点库存确认事件 Portal 死信消息监听器。
     *
     * @param objectMapper JSON 序列化与反序列化工具
     * @param consumerService 热点库存确认事件消费服务
     */
    public HotStockOrderConfirmedDeadLetterMessageListener(
            ObjectMapper objectMapper,
            HotStockOrderConfirmedEventConsumerService consumerService
    ) {
        this.objectMapper = objectMapper;
        this.consumerService = consumerService;
    }

    /**
     * 手动确认或延迟重试一条热点库存确认死信消息。
     *
     * @param message RabbitMQ 死信消息
     * @param channel RabbitMQ 手动确认通道
     * @throws IOException RabbitMQ ACK 或 NACK 失败时抛出
     */
    @RabbitListener(
            queues = HotStockOrderMessageConstants
                    .CONFIRMED_DEAD_LETTER_QUEUE,
            containerFactory =
                    "hotStockOrderConfirmedDeadLetterListenerContainerFactory"
    )
    public void promoteOrder(Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            HotStockOrderConfirmedEvent event =
                    objectMapper.readValue(
                            new String(
                                    message.getBody(),
                                    StandardCharsets.UTF_8
                            ),
                            HotStockOrderConfirmedEvent.class
                    );

            consumerService.promoteOrderToPendingPayment(event);

            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            LOGGER.error(
                    "热点库存确认事件无法推进 Portal 订单，进入延迟重试",
                    exception
            );
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
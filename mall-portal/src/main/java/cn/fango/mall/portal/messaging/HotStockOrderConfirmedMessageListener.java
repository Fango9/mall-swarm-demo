package cn.fango.mall.portal.messaging;

import cn.fango.mall.common.event.HotStockOrderConfirmedEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.messaging.HotStockOrderMessageConstants;
import cn.fango.mall.portal.api.OrderErrorCode;
import cn.fango.mall.portal.service.HotStockOrderConfirmedEventConsumerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 热点库存预占用已确认事件 RabbitMQ 监听器。
 *
 * <p>监听器正常返回后，Spring AMQP 才确认消息。订单状态推进失败时抛出异常，
 * 消息会先按 listener retry 配置重试，随后进入确认事件 DLQ。</p>
 */
@Component
public class HotStockOrderConfirmedMessageListener {

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
     * 创建热点库存确认事件监听器。
     *
     * @param objectMapper JSON 序列化与反序列化工具
     * @param consumerService 热点库存确认事件消费服务
     */
    public HotStockOrderConfirmedMessageListener(
            ObjectMapper objectMapper,
            HotStockOrderConfirmedEventConsumerService consumerService
    ) {
        this.objectMapper = objectMapper;
        this.consumerService = consumerService;
    }

    /**
     * 推进一笔已完成热点库存确认的订单。
     *
     * @param payload Admin Outbox 发布的确认事件 JSON
     */
    @RabbitListener(
            queues = HotStockOrderMessageConstants.CONFIRMED_QUEUE
    )
    public void promoteOrder(byte[] payload) {
        HotStockOrderConfirmedEvent event = deserializeEvent(payload);

        consumerService.promoteOrderToPendingPayment(event);
    }

    /**
     * 将 RabbitMQ 消息反序列化为热点库存确认事件。
     *
     * @param payload RabbitMQ 消息体
     * @return 热点库存确认事件
     */
    private HotStockOrderConfirmedEvent deserializeEvent(
            byte[] payload
    ) {
        try {
            return objectMapper.readValue(
                    new String(payload, StandardCharsets.UTF_8),
                    HotStockOrderConfirmedEvent.class
            );
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    OrderErrorCode.HOT_STOCK_ORDER_CONFIRM_FAILED,
                    exception
            );
        }
    }
}
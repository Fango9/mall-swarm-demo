package cn.fango.mall.admin.messaging;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.service.OrderCreatedEventConsumerService;
import cn.fango.mall.common.event.OrderCreatedEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.messaging.OrderMessageConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单创建事件 RabbitMQ 监听器。
 */
@Component
public class OrderCreatedMessageListener {

    /**
     * JSON 序列化与反序列化对象。
     */
    private final ObjectMapper objectMapper;

    /**
     * 订单创建事件的库存预占确认服务。
     */
    private final OrderCreatedEventConsumerService
            orderCreatedEventConsumerService;

    /**
     * 创建订单创建事件监听器。
     *
     * @param objectMapper JSON 序列化与反序列化对象
     * @param orderCreatedEventConsumerService
     *        订单创建事件的库存预占确认服务
     */
    public OrderCreatedMessageListener(
            ObjectMapper objectMapper,
            OrderCreatedEventConsumerService
                    orderCreatedEventConsumerService
    ) {
        this.objectMapper = objectMapper;
        this.orderCreatedEventConsumerService =
                orderCreatedEventConsumerService;
    }

    /**
     * 处理订单创建事件，并确认订单对应的库存预占。
     *
     * <p>任何异常都会使当前消息拒绝且不重新入队，随后由 RabbitMQ
     * 转发到订单创建事件死信队列。</p>
     *
     * @param payload Portal 发送的订单创建事件 JSON 字节数组
     */
    @RabbitListener(
            queues = OrderMessageConstants.ORDER_CREATED_QUEUE
    )
    public void handleOrderCreated(byte[] payload) {
        String eventJson = new String(payload, StandardCharsets.UTF_8);
        OrderCreatedEvent event = deserializeEvent(eventJson);

        orderCreatedEventConsumerService.confirmStockReservation(event);
    }

    /**
     * 将消息 JSON 反序列化为订单创建事件。
     *
     * @param payload Portal 发送的订单创建事件 JSON
     * @return 已反序列化的订单创建事件
     */
    private OrderCreatedEvent deserializeEvent(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    OrderCreatedEvent.class
            );
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    StockReservationErrorCode.ORDER_CREATED_EVENT_INVALID,
                    exception
            );
        }
    }
}
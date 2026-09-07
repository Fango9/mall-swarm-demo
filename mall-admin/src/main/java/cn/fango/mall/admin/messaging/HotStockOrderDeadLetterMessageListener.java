package cn.fango.mall.admin.messaging;

import cn.fango.mall.admin.api.HotStockReservationConfirmLuaResult;
import cn.fango.mall.admin.redis.HotStockReservationConfirmLuaExecutor;
import cn.fango.mall.admin.service.HotStockOrderCreatedEventConsumerService;
import cn.fango.mall.common.event.HotStockOrderCreatedEvent;
import cn.fango.mall.common.messaging.HotStockOrderMessageConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
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
 * 热点订单确认死信消息监听器。
 *
 * <p>仅当 MySQL 与 Redis 均确认成功时 ACK。其他情况一律 NACK 到延迟重试队列，
 * 保证乱序到达或短暂基础设施故障不会导致订单永久停在 PENDING_STOCK。</p>
 */
@Component
public class HotStockOrderDeadLetterMessageListener {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HotStockOrderDeadLetterMessageListener.class
    );

    /**
     * JSON 序列化与反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 热点订单确认 MySQL 消费服务。
     */
    private final HotStockOrderCreatedEventConsumerService
            consumerService;

    /**
     * Redis 原子确认预占用 Lua 执行器。
     */
    private final HotStockReservationConfirmLuaExecutor
            confirmLuaExecutor;

    /**
     * 创建热点订单确认死信消息监听器。
     *
     * @param objectMapper JSON 序列化与反序列化工具
     * @param consumerService 热点订单确认 MySQL 消费服务
     * @param confirmLuaExecutor Redis 原子确认预占用 Lua 执行器
     */
    public HotStockOrderDeadLetterMessageListener(
            ObjectMapper objectMapper,
            HotStockOrderCreatedEventConsumerService consumerService,
            HotStockReservationConfirmLuaExecutor confirmLuaExecutor
    ) {
        this.objectMapper = objectMapper;
        this.consumerService = consumerService;
        this.confirmLuaExecutor = confirmLuaExecutor;
    }

    /**
     * 手动确认或延迟重试一条热点订单确认死信消息。
     *
     * @param message RabbitMQ 死信消息
     * @param channel RabbitMQ 手动确认通道
     * @throws IOException RabbitMQ ACK 或 NACK 失败时抛出
     */
    @RabbitListener(
            queues = HotStockOrderMessageConstants.DEAD_LETTER_QUEUE,
            containerFactory =
                    "hotStockOrderDeadLetterListenerContainerFactory"
    )
    public void confirmOrder(Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            HotStockOrderCreatedEvent event = deserializeEvent(
                    message.getBody()
            );

            consumerService.confirmHotStockReservation(event);

            Long luaResult = confirmLuaExecutor.confirm(event.orderSn());

            if (HotStockReservationConfirmLuaResult.CONFIRMED
                    == luaResult
                    || HotStockReservationConfirmLuaResult.ALREADY_CONFIRMED
                    == luaResult) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            LOGGER.warn(
                    "热点订单确认条件暂未满足，进入延迟重试，orderSn={}, luaResult={}",
                    event.orderSn(),
                    luaResult
            );
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception exception) {
            LOGGER.error(
                    "热点订单确认死信处理异常，进入延迟重试",
                    exception
            );
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 将 RabbitMQ 消息反序列化为热点订单确认事件。
     *
     * @param payload RabbitMQ 消息体
     * @return 热点订单确认事件
     * @throws JsonProcessingException JSON 内容非法时抛出
     */
    private HotStockOrderCreatedEvent deserializeEvent(byte[] payload)
            throws JsonProcessingException {
        return objectMapper.readValue(
                new String(payload, StandardCharsets.UTF_8),
                HotStockOrderCreatedEvent.class
        );
    }
}
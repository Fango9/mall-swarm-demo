package cn.fango.mall.admin.messaging;

import cn.fango.mall.admin.api.HotStockReservationConfirmLuaResult;
import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.redis.HotStockReservationConfirmLuaExecutor;
import cn.fango.mall.admin.service.HotStockOrderCreatedEventConsumerService;
import cn.fango.mall.common.event.HotStockOrderCreatedEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.messaging.HotStockOrderMessageConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Portal 热点订单确认事件 RabbitMQ 监听器。
 *
 * <p>监听器正常返回时，表示 MySQL 与 Redis 均已确认；Spring AMQP 才会 ACK。
 * 任何异常都会触发主队列重试，重试耗尽后进入热点订单确认 DLQ。</p>
 */
@Component
public class HotStockOrderCreatedMessageListener {

    /**
     * JSON 序列化与反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 热点订单确认 MySQL 消费服务。
     */
    private final HotStockOrderCreatedEventConsumerService consumerService;

    /**
     * Redis 原子确认预占用 Lua 执行器。
     */
    private final HotStockReservationConfirmLuaExecutor confirmLuaExecutor;

    /**
     * 创建热点订单确认事件监听器。
     *
     * @param objectMapper JSON 序列化与反序列化工具
     * @param consumerService 热点订单确认 MySQL 消费服务
     * @param confirmLuaExecutor Redis 原子确认预占用 Lua 执行器
     */
    public HotStockOrderCreatedMessageListener(
            ObjectMapper objectMapper,
            HotStockOrderCreatedEventConsumerService consumerService,
            HotStockReservationConfirmLuaExecutor confirmLuaExecutor
    ) {
        this.objectMapper = objectMapper;
        this.consumerService = consumerService;
        this.confirmLuaExecutor = confirmLuaExecutor;
    }

    /**
     * 确认 Portal 已提交的热点订单及对应库存预占用。
     *
     * @param payload Portal Outbox 发布的热点订单确认事件 JSON
     */
    @RabbitListener(
            queues = HotStockOrderMessageConstants.QUEUE
    )
    public void confirmOrder(byte[] payload) {
        HotStockOrderCreatedEvent event = deserializeEvent(payload);

        // 确认订单预占记录，插入预占成功事件到 outbox
        consumerService.confirmHotStockReservation(event);

        // 更新 redis hash 为 confirm
        Long luaResult = confirmLuaExecutor.confirm(event.orderSn());

        if (HotStockReservationConfirmLuaResult.CONFIRMED
                == luaResult
                || HotStockReservationConfirmLuaResult.ALREADY_CONFIRMED
                == luaResult) {
            return;
        }

        throw new ApiException(StockReservationErrorCode.HOT_STOCK_ORDER_CONFIRM_PENDING);
    }

    /**
     * 将 RabbitMQ 消息反序列化为热点订单确认事件。
     *
     * @param payload RabbitMQ 消息体
     * @return 热点订单确认事件
     */
    private HotStockOrderCreatedEvent deserializeEvent(byte[] payload) {
        try {
            return objectMapper.readValue(
                    new String(payload, StandardCharsets.UTF_8),
                    HotStockOrderCreatedEvent.class
            );
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    StockReservationErrorCode.ORDER_CREATED_EVENT_INVALID,
                    exception
            );
        }
    }
}
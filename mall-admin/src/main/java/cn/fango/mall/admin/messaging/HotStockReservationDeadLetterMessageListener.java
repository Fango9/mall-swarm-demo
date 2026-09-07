package cn.fango.mall.admin.messaging;

import cn.fango.mall.admin.api.HotStockReservationReleaseLuaResult;
import cn.fango.mall.admin.api.HotStockReservationStatus;
import cn.fango.mall.admin.redis.HotStockReservationReleaseLuaExecutor;
import cn.fango.mall.admin.service.HotStockOrderFailedOutboxService;
import cn.fango.mall.common.event.HotStockReservationEvent;
import cn.fango.mall.common.messaging.HotStockReservationMessageConstants;
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
 * 热点库存预占用死信补偿消费者。
 *
 * <p>此消费者只处理主消费者重试耗尽后的消息。只有 Lua 已确认库存无需回补、
 * 已成功回补，或此前已回补时，才 ACK RabbitMQ 消息。</p>
 */
@Component
public class HotStockReservationDeadLetterMessageListener {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HotStockReservationDeadLetterMessageListener.class
    );

    /**
     * JSON 序列化与反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * Redis 原子释放预占用 Lua 执行器。
     */
    private final HotStockReservationReleaseLuaExecutor releaseLuaExecutor;

    /**
     * 热点库存最终失败 Outbox 写入服务。
     */
    private final HotStockOrderFailedOutboxService failedOutboxService;

    /**
     * 创建热点库存预占用死信补偿消费者。
     *
     * @param objectMapper JSON 序列化与反序列化工具
     * @param releaseLuaExecutor Redis 原子释放预占用 Lua 执行器
     */
    public HotStockReservationDeadLetterMessageListener(
            ObjectMapper objectMapper,
            HotStockReservationReleaseLuaExecutor releaseLuaExecutor,
            HotStockOrderFailedOutboxService failedOutboxService
    ) {
        this.objectMapper = objectMapper;
        this.releaseLuaExecutor = releaseLuaExecutor;
        this.failedOutboxService = failedOutboxService;
    }

    /**
     * 补偿一条 MySQL 最终落库失败的热点库存预占用。
     *
     * @param message RabbitMQ 死信消息
     * @param channel RabbitMQ 手动确认通道
     * @throws IOException RabbitMQ 确认或拒绝消息失败时抛出
     */
    @RabbitListener(
            queues = HotStockReservationMessageConstants.DEAD_LETTER_QUEUE,
            containerFactory =
                    "hotStockReservationDeadLetterListenerContainerFactory"
    )
    public void compensate(Message message, Channel channel)
            throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            HotStockReservationEvent event = deserializeEvent(message.getBody());
            Long luaResult = releaseLuaExecutor.release(
                    event,
                    HotStockReservationStatus.FAILED
            );

            if (isReleased(luaResult)) {
                // Redis 已确认库存回补后，必须先可靠通知 Portal。
                failedOutboxService.recordFailure(event);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (HotStockReservationReleaseLuaResult.NOT_RELEASEABLE
                    == luaResult) {
                // MySQL 已成功落库，不能再回补，也不能标记 Portal 订单失败。
                channel.basicAck(deliveryTag, false);
                return;
            }

            LOGGER.error(
                    "热点库存预占用死信补偿暂不能安全完成，"
                            + "reservationNo={}, luaResult={}",
                    event.reservationNo(),
                    luaResult
            );
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception exception) {
            LOGGER.error(
                    "热点库存预占用死信补偿异常，消息将重新入队",
                    exception
            );
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 判断 Lua 返回结果是否表示 Redis 库存已完成回补。
     *
     * @param luaResult Redis Lua 原始返回码
     * @return Redis 库存已回补或此前已回补时返回 {@code true}
     */
    private boolean isReleased(Long luaResult) {
        return HotStockReservationReleaseLuaResult.RELEASED == luaResult
                || HotStockReservationReleaseLuaResult.ALREADY_RELEASED
                == luaResult;
    }

    /**
     * 将死信消息反序列化为热点库存预占用事件。
     *
     * @param payload RabbitMQ 死信消息体
     * @return 反序列化后的热点库存预占用事件
     * @throws JsonProcessingException JSON 内容非法时抛出
     */
    private HotStockReservationEvent deserializeEvent(byte[] payload)
            throws JsonProcessingException {
        return objectMapper.readValue(
                new String(payload, StandardCharsets.UTF_8),
                HotStockReservationEvent.class
        );
    }
}
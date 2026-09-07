package cn.fango.mall.portal.config;

import cn.fango.mall.common.messaging.HotStockOrderMessageConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 热点库存预占用已确认事件的 Portal RabbitMQ 拓扑配置。
 */
@Configuration
public class HotStockOrderConfirmedMessageTopologyConfig {

    /**
     * 确认事件延迟重试等待时长，来自 Nacos 的 Outbox 重试配置。
     */
    private final long retryDelayMillis;

    /**
     * 创建热点库存确认事件 Portal 拓扑配置。
     *
     * @param retryDelayMillis 确认事件延迟重试等待时长，单位毫秒
     */
    public HotStockOrderConfirmedMessageTopologyConfig(
            @Value("${mall.outbox.retry-delay-ms}")
            long retryDelayMillis
    ) {
        if (retryDelayMillis <= 0
                || retryDelayMillis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "mall.outbox.retry-delay-ms 必须在 1 到 "
                            + Integer.MAX_VALUE
                            + " 之间"
            );
        }

        this.retryDelayMillis = retryDelayMillis;
    }

    /**
     * 声明热点订单事件直连交换机。
     *
     * @return 热点订单事件交换机
     */
    @Bean
    public DirectExchange hotStockOrderConfirmedExchange() {
        return new DirectExchange(
                HotStockOrderMessageConstants.EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点库存预占用已确认事件队列。
     *
     * @return 热点库存预占用已确认事件队列
     */
    @Bean
    public Queue hotStockOrderConfirmedQueue() {
        return QueueBuilder.durable(
                        HotStockOrderMessageConstants.CONFIRMED_QUEUE
                )
                .deadLetterExchange(
                        HotStockOrderMessageConstants
                                .CONFIRMED_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockOrderMessageConstants
                                .CONFIRMED_DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点库存预占用已确认事件队列。
     *
     * @param hotStockOrderConfirmedQueue 已确认事件队列
     * @param hotStockOrderConfirmedExchange 热点订单事件交换机
     * @return 已确认事件队列绑定
     */
    @Bean
    public Binding hotStockOrderConfirmedBinding(
            Queue hotStockOrderConfirmedQueue,
            DirectExchange hotStockOrderConfirmedExchange
    ) {
        return BindingBuilder.bind(hotStockOrderConfirmedQueue)
                .to(hotStockOrderConfirmedExchange)
                .with(
                        HotStockOrderMessageConstants
                                .CONFIRMED_ROUTING_KEY
                );
    }

    /**
     * 声明热点库存预占用已确认事件死信交换机。
     *
     * @return 已确认事件死信交换机
     */
    @Bean
    public DirectExchange hotStockOrderConfirmedDeadLetterExchange() {
        return new DirectExchange(
                HotStockOrderMessageConstants
                        .CONFIRMED_DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点库存预占用已确认事件死信队列。
     *
     * <p>Portal 暂时无法推进订单状态时，消息会进入延迟重试队列，
     * 而不是被确认后永久丢失。</p>
     *
     * @return 已确认事件死信队列
     */
    @Bean
    public Queue hotStockOrderConfirmedDeadLetterQueue() {
        return QueueBuilder.durable(
                        HotStockOrderMessageConstants
                                .CONFIRMED_DEAD_LETTER_QUEUE
                )
                .deadLetterExchange(
                        HotStockOrderMessageConstants
                                .CONFIRMED_RETRY_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockOrderMessageConstants
                                .CONFIRMED_RETRY_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点库存预占用已确认事件死信队列。
     *
     * @param hotStockOrderConfirmedDeadLetterQueue 已确认事件死信队列
     * @param hotStockOrderConfirmedDeadLetterExchange 已确认事件死信交换机
     * @return 已确认事件死信队列绑定
     */
    @Bean
    public Binding hotStockOrderConfirmedDeadLetterBinding(
            Queue hotStockOrderConfirmedDeadLetterQueue,
            DirectExchange hotStockOrderConfirmedDeadLetterExchange
    ) {
        return BindingBuilder.bind(
                        hotStockOrderConfirmedDeadLetterQueue
                )
                .to(hotStockOrderConfirmedDeadLetterExchange)
                .with(
                        HotStockOrderMessageConstants
                                .CONFIRMED_DEAD_LETTER_ROUTING_KEY
                );
    }

    /**
     * 声明热点库存预占用已确认事件延迟重试交换机。
     *
     * @return 已确认事件延迟重试交换机
     */
    @Bean
    public DirectExchange hotStockOrderConfirmedRetryExchange() {
        return new DirectExchange(
                HotStockOrderMessageConstants
                        .CONFIRMED_RETRY_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点库存预占用已确认事件延迟重试队列。
     *
     * @return 已确认事件延迟重试队列
     */
    @Bean
    public Queue hotStockOrderConfirmedRetryQueue() {
        return QueueBuilder.durable(
                        HotStockOrderMessageConstants
                                .CONFIRMED_RETRY_QUEUE
                )
                .ttl((int) retryDelayMillis)
                .deadLetterExchange(
                        HotStockOrderMessageConstants
                                .CONFIRMED_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockOrderMessageConstants
                                .CONFIRMED_DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点库存预占用已确认事件延迟重试队列。
     *
     * @param hotStockOrderConfirmedRetryQueue 已确认事件延迟重试队列
     * @param hotStockOrderConfirmedRetryExchange 已确认事件延迟重试交换机
     * @return 已确认事件延迟重试队列绑定
     */
    @Bean
    public Binding hotStockOrderConfirmedRetryBinding(
            Queue hotStockOrderConfirmedRetryQueue,
            DirectExchange hotStockOrderConfirmedRetryExchange
    ) {
        return BindingBuilder.bind(
                        hotStockOrderConfirmedRetryQueue
                )
                .to(hotStockOrderConfirmedRetryExchange)
                .with(
                        HotStockOrderMessageConstants
                                .CONFIRMED_RETRY_ROUTING_KEY
                );
    }
}
package cn.fango.mall.admin.config;

import cn.fango.mall.common.messaging.HotStockOrderMessageConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 热点库存预占用订单确认事件的 RabbitMQ 拓扑配置。
 */
@Configuration
public class HotStockOrderMessageTopologyConfig {

    /**
     * 热点库存预占用运行配置。
     */
    private final HotStockReservationProperties
            hotStockReservationProperties;

    /**
     * 创建热点订单确认事件拓扑配置。
     *
     * @param hotStockReservationProperties 热点库存预占用运行配置
     */
    public HotStockOrderMessageTopologyConfig(
            HotStockReservationProperties hotStockReservationProperties
    ) {
        this.hotStockReservationProperties =
                hotStockReservationProperties;
    }

    /**
     * 声明热点订单确认事件交换机。
     *
     * @return 热点订单确认事件交换机
     */
    @Bean
    public DirectExchange hotStockOrderExchange() {
        return new DirectExchange(
                HotStockOrderMessageConstants.EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点订单确认主队列。
     *
     * @return 热点订单确认主队列
     */
    @Bean
    public Queue hotStockOrderQueue() {
        return QueueBuilder.durable(
                        HotStockOrderMessageConstants.QUEUE
                )
                .deadLetterExchange(
                        HotStockOrderMessageConstants
                                .DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockOrderMessageConstants
                                .DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点订单确认主队列。
     *
     * @param hotStockOrderQueue 热点订单确认主队列
     * @param hotStockOrderExchange 热点订单确认事件交换机
     * @return 热点订单确认主队列绑定
     */
    @Bean
    public Binding hotStockOrderBinding(
            Queue hotStockOrderQueue,
            DirectExchange hotStockOrderExchange
    ) {
        return BindingBuilder.bind(hotStockOrderQueue)
                .to(hotStockOrderExchange)
                .with(HotStockOrderMessageConstants.ROUTING_KEY);
    }

    /**
     * 声明热点订单确认死信交换机。
     *
     * @return 热点订单确认死信交换机
     */
    @Bean
    public DirectExchange hotStockOrderDeadLetterExchange() {
        return new DirectExchange(
                HotStockOrderMessageConstants.DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点订单确认死信队列。
     *
     * <p>确认条件暂未满足或基础设施故障时，消费者拒绝消息；RabbitMQ 将消息
     * 路由到延迟重试交换机，避免过早确认导致订单永久停留在 PENDING_STOCK。</p>
     *
     * @return 热点订单确认死信队列
     */
    @Bean
    public Queue hotStockOrderDeadLetterQueue() {
        return QueueBuilder.durable(
                        HotStockOrderMessageConstants
                                .DEAD_LETTER_QUEUE
                )
                .deadLetterExchange(
                        HotStockOrderMessageConstants.RETRY_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockOrderMessageConstants.RETRY_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点订单确认死信队列。
     *
     * @param hotStockOrderDeadLetterQueue 热点订单确认死信队列
     * @param hotStockOrderDeadLetterExchange 热点订单确认死信交换机
     * @return 热点订单确认死信队列绑定
     */
    @Bean
    public Binding hotStockOrderDeadLetterBinding(
            Queue hotStockOrderDeadLetterQueue,
            DirectExchange hotStockOrderDeadLetterExchange
    ) {
        return BindingBuilder.bind(hotStockOrderDeadLetterQueue)
                .to(hotStockOrderDeadLetterExchange)
                .with(
                        HotStockOrderMessageConstants
                                .DEAD_LETTER_ROUTING_KEY
                );
    }

    /**
     * 声明热点订单确认延迟重试交换机。
     *
     * @return 热点订单确认延迟重试交换机
     */
    @Bean
    public DirectExchange hotStockOrderRetryExchange() {
        return new DirectExchange(
                HotStockOrderMessageConstants.RETRY_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点订单确认延迟重试队列。
     *
     * @return 热点订单确认延迟重试队列
     */
    @Bean
    public Queue hotStockOrderRetryQueue() {
        long retryDelayMillis = hotStockReservationProperties
                .getDeadLetterRetryDelayMillis();

        if (retryDelayMillis <= 0
                || retryDelayMillis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "mall.hot-stock-reservation.dead-letter-retry-delay-millis "
                            + "必须在 1 到 "
                            + Integer.MAX_VALUE
                            + " 之间"
            );
        }

        return QueueBuilder.durable(
                        HotStockOrderMessageConstants.RETRY_QUEUE
                )
                .ttl((int) retryDelayMillis)
                .deadLetterExchange(
                        HotStockOrderMessageConstants
                                .DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockOrderMessageConstants
                                .DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点订单确认延迟重试队列。
     *
     * @param hotStockOrderRetryQueue 热点订单确认延迟重试队列
     * @param hotStockOrderRetryExchange 热点订单确认延迟重试交换机
     * @return 热点订单确认延迟重试队列绑定
     */
    @Bean
    public Binding hotStockOrderRetryBinding(
            Queue hotStockOrderRetryQueue,
            DirectExchange hotStockOrderRetryExchange
    ) {
        return BindingBuilder.bind(hotStockOrderRetryQueue)
                .to(hotStockOrderRetryExchange)
                .with(HotStockOrderMessageConstants.RETRY_ROUTING_KEY);
    }

}
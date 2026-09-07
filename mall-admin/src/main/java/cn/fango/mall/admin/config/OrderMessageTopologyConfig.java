package cn.fango.mall.admin.config;

import cn.fango.mall.common.messaging.HotStockReservationMessageConstants;
import cn.fango.mall.common.messaging.OrderMessageConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

/**
 * 订单创建事件 RabbitMQ 拓扑配置。
 *
 * <p>Admin 是库存确认事件的消费者，因此由它拥有并声明主队列与死信队列。</p>
 */
@Configuration
public class OrderMessageTopologyConfig {

    /**
     * 热点库存预占用运行配置。
     */
    private final HotStockReservationProperties
            hotStockReservationProperties;

    /**
     * 创建 RabbitMQ 消息拓扑配置。
     *
     * @param hotStockReservationProperties 热点库存预占用运行配置
     */
    public OrderMessageTopologyConfig(
            HotStockReservationProperties hotStockReservationProperties
    ) {
        this.hotStockReservationProperties =
                hotStockReservationProperties;
    }

    /**
     * 创建 RabbitMQ 管理对象，并在应用启动时声明本配置类中的交换机、队列和绑定关系。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @return RabbitMQ 管理对象
     */
    @Bean
    public AmqpAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);

        return rabbitAdmin;
    }

    /**
     * 声明订单事件直连交换机。
     *
     * @return 订单事件交换机
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(
                OrderMessageConstants.ORDER_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点库存预占用直连交换机。
     *
     * @return 热点库存预占用交换机
     */
    @Bean
    public DirectExchange hotStockReservationExchange() {
        return new DirectExchange(
                HotStockReservationMessageConstants.EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点库存预占用主队列。
     *
     * <p>消费者在不可恢复异常时拒绝消息，RabbitMQ 将消息路由到此队列
     * 专属的死信交换机；不能与旧订单事件队列共用死信路径。</p>
     *
     * @return 热点库存预占用主队列
     */
    @Bean
    public Queue hotStockReservationQueue() {
        return QueueBuilder.durable(
                        HotStockReservationMessageConstants.QUEUE
                )
                .deadLetterExchange(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点库存预占用主队列。
     *
     * @param hotStockReservationQueue 热点库存预占用主队列
     * @param hotStockReservationExchange 热点库存预占用交换机
     * @return 热点库存预占用主队列绑定
     */
    @Bean
    public Binding hotStockReservationBinding(
            Queue hotStockReservationQueue,
            DirectExchange hotStockReservationExchange
    ) {
        return BindingBuilder.bind(hotStockReservationQueue)
                .to(hotStockReservationExchange)
                .with(HotStockReservationMessageConstants.ROUTING_KEY);
    }

    /**
     * 声明热点库存预占用死信交换机。
     *
     * @return 热点库存预占用死信交换机
     */
    @Bean
    public DirectExchange hotStockReservationDeadLetterExchange() {
        return new DirectExchange(
                HotStockReservationMessageConstants
                        .DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点库存预占用死信队列。
     *
     * <p>补偿消费者无法安全回补 Redis 时拒绝消息，RabbitMQ 将其路由到
     * 延迟重试交换机，避免消息被确认后永久丢失。</p>
     *
     * @return 热点库存预占用死信队列
     */
    @Bean
    public Queue hotStockReservationDeadLetterQueue() {
        return QueueBuilder.durable(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_QUEUE
                )
                .deadLetterExchange(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_RETRY_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_RETRY_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点库存预占用死信队列。
     *
     * @param hotStockReservationDeadLetterQueue 热点库存预占用死信队列
     * @param hotStockReservationDeadLetterExchange 热点库存预占用死信交换机
     * @return 热点库存预占用死信队列绑定
     */
    @Bean
    public Binding hotStockReservationDeadLetterBinding(
            Queue hotStockReservationDeadLetterQueue,
            DirectExchange hotStockReservationDeadLetterExchange
    ) {
        return BindingBuilder.bind(hotStockReservationDeadLetterQueue)
                .to(hotStockReservationDeadLetterExchange)
                .with(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_ROUTING_KEY
                );
    }

    /**
     * 声明热点库存预占用死信补偿延迟重试交换机。
     *
     * @return 死信补偿延迟重试交换机
     */
    @Bean
    public DirectExchange hotStockReservationDeadLetterRetryExchange() {
        return new DirectExchange(
                HotStockReservationMessageConstants
                        .DEAD_LETTER_RETRY_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明热点库存预占用死信补偿延迟重试队列。
     *
     * <p>消息在此队列等待配置的 TTL；TTL 到期后 RabbitMQ 自动将消息
     * 路由回死信交换机和死信队列，等待下一次补偿。</p>
     *
     * @return 死信补偿延迟重试队列
     */
    @Bean
    public Queue hotStockReservationDeadLetterRetryQueue() {
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
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_RETRY_QUEUE
                )
                .ttl((int) retryDelayMillis)
                .deadLetterExchange(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定热点库存预占用死信补偿延迟重试队列。
     *
     * @param hotStockReservationDeadLetterRetryQueue
     *        死信补偿延迟重试队列
     * @param hotStockReservationDeadLetterRetryExchange
     *        死信补偿延迟重试交换机
     * @return 死信补偿延迟重试队列绑定
     */
    @Bean
    public Binding hotStockReservationDeadLetterRetryBinding(
            Queue hotStockReservationDeadLetterRetryQueue,
            DirectExchange hotStockReservationDeadLetterRetryExchange
    ) {
        return BindingBuilder.bind(
                        hotStockReservationDeadLetterRetryQueue
                )
                .to(hotStockReservationDeadLetterRetryExchange)
                .with(
                        HotStockReservationMessageConstants
                                .DEAD_LETTER_RETRY_ROUTING_KEY
                );
    }

    /**
     * 声明订单创建事件主队列。
     *
     * @return 订单创建事件主队列
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(
                        OrderMessageConstants.ORDER_CREATED_QUEUE
                )
                .deadLetterExchange(
                        OrderMessageConstants.ORDER_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        OrderMessageConstants
                                .ORDER_CREATED_DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定订单创建事件主队列。
     *
     * @param orderCreatedQueue 订单创建事件主队列
     * @param orderExchange 订单事件交换机
     * @return 队列绑定
     */
    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(orderExchange)
                .with(OrderMessageConstants.ORDER_CREATED_ROUTING_KEY);
    }

    /**
     * 声明订单事件死信交换机。
     *
     * @return 订单事件死信交换机
     */
    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return new DirectExchange(
                OrderMessageConstants.ORDER_DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明订单创建事件死信队列。
     *
     * @return 订单创建事件死信队列
     */
    @Bean
    public Queue orderCreatedDeadLetterQueue() {
        return QueueBuilder.durable(
                OrderMessageConstants.ORDER_CREATED_DEAD_LETTER_QUEUE
        ).build();
    }

    /**
     * 绑定订单创建事件死信队列。
     *
     * @param orderCreatedDeadLetterQueue 订单创建事件死信队列
     * @param orderDeadLetterExchange 订单事件死信交换机
     * @return 死信队列绑定
     */
    @Bean
    public Binding orderCreatedDeadLetterBinding(Queue orderCreatedDeadLetterQueue, DirectExchange orderDeadLetterExchange) {
        return BindingBuilder.bind(orderCreatedDeadLetterQueue)
                .to(orderDeadLetterExchange)
                .with(OrderMessageConstants.ORDER_CREATED_DEAD_LETTER_ROUTING_KEY);
    }
}

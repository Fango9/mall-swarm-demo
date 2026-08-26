package cn.fango.mall.admin.config;

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
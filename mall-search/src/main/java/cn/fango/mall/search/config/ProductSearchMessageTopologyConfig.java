package cn.fango.mall.search.config;

import cn.fango.mall.common.messaging.ProductCacheMessageConstants;
import cn.fango.mall.common.messaging.ProductSearchMessageConstants;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 商品搜索索引消费者的 RabbitMQ 拓扑配置。
 *
 * <p>搜索服务复用 Admin 已发布商品变更事件的交换机和路由键，
 * 只声明自身的主队列、死信交换机、死信队列及绑定关系。</p>
 */
@Configuration
public class ProductSearchMessageTopologyConfig {

    /**
     * 创建 RabbitMQ 管理对象，并在搜索服务启动时声明本类定义的拓扑。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @return RabbitMQ 管理对象
     */
    @Bean
    public AmqpAdmin productSearchRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);

        return rabbitAdmin;
    }

    /**
     * 声明已有的商品变更直连交换机。
     *
     * <p>若 Portal 已声明过同名同属性交换机，RabbitMQ 会幂等复用它。</p>
     *
     * @return 商品变更直连交换机
     */
    @Bean
    public DirectExchange productChangedExchange() {
        return new DirectExchange(
                ProductCacheMessageConstants.PRODUCT_CACHE_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明搜索服务商品变更事件主队列。
     *
     * @return 搜索服务商品变更事件主队列
     */
    @Bean
    public Queue productSearchProductChangedQueue() {
        return QueueBuilder.durable(
                        ProductSearchMessageConstants.PRODUCT_CHANGED_QUEUE
                )
                .deadLetterExchange(
                        ProductSearchMessageConstants.PRODUCT_CHANGED_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        ProductSearchMessageConstants.PRODUCT_CHANGED_DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 将搜索服务主队列绑定到既有商品变更交换机。
     *
     * @param productSearchProductChangedQueue 搜索服务商品变更事件主队列
     * @param productChangedExchange 商品变更直连交换机
     * @return 搜索服务商品变更事件队列绑定
     */
    @Bean
    public Binding productSearchProductChangedBinding(Queue productSearchProductChangedQueue, DirectExchange productChangedExchange) {
        return BindingBuilder.bind(productSearchProductChangedQueue)
                .to(productChangedExchange)
                .with(ProductCacheMessageConstants.PRODUCT_CHANGED_ROUTING_KEY);
    }

    /**
     * 声明搜索服务商品变更事件死信交换机。
     *
     * @return 搜索服务商品变更事件死信交换机
     */
    @Bean
    public DirectExchange productSearchDeadLetterExchange() {
        return new DirectExchange(
                ProductSearchMessageConstants.PRODUCT_CHANGED_DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明搜索服务商品变更事件死信队列。
     *
     * @return 搜索服务商品变更事件死信队列
     */
    @Bean
    public Queue productSearchDeadLetterQueue() {
        return QueueBuilder.durable(
                ProductSearchMessageConstants.PRODUCT_CHANGED_DEAD_LETTER_QUEUE
        ).build();
    }

    /**
     * 绑定搜索服务商品变更事件死信队列。
     *
     * @param productSearchDeadLetterQueue 搜索服务商品变更事件死信队列
     * @param productSearchDeadLetterExchange 搜索服务商品变更事件死信交换机
     * @return 搜索服务商品变更事件死信队列绑定
     */
    @Bean
    public Binding productSearchDeadLetterBinding(Queue productSearchDeadLetterQueue, DirectExchange productSearchDeadLetterExchange) {
        return BindingBuilder.bind(productSearchDeadLetterQueue)
                .to(productSearchDeadLetterExchange)
                .with(ProductSearchMessageConstants.PRODUCT_CHANGED_DEAD_LETTER_ROUTING_KEY);
    }
}
package cn.fango.mall.portal.config;

import cn.fango.mall.common.messaging.ProductCacheMessageConstants;
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
 * Portal 商品缓存失效事件的 RabbitMQ 拓扑配置。
 *
 * <p>Portal 是消费者，因此由它声明商品变更主队列、死信队列及绑定关系。</p>
 */
@Configuration
public class ProductCacheMessageTopologyConfig {

    /**
     * 创建 RabbitMQ 管理对象，并在 Portal 启动时声明本类定义的拓扑。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @return RabbitMQ 管理对象
     */
    @Bean
    public AmqpAdmin productCacheRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);

        return rabbitAdmin;
    }

    /**
     * 声明商品变更事件直连交换机。
     *
     * @return 商品变更事件交换机
     */
    @Bean
    public DirectExchange productCacheExchange() {
        return new DirectExchange(
                ProductCacheMessageConstants.PRODUCT_CACHE_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明 Portal 商品变更事件主队列。
     *
     * @return 商品变更事件主队列
     */
    @Bean
    public Queue productChangedQueue() {
        return QueueBuilder.durable(
                        ProductCacheMessageConstants.PRODUCT_CHANGED_QUEUE
                )
                .deadLetterExchange(
                        ProductCacheMessageConstants.PRODUCT_CACHE_DEAD_LETTER_EXCHANGE
                )
                .deadLetterRoutingKey(
                        ProductCacheMessageConstants.PRODUCT_CHANGED_DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    /**
     * 绑定商品变更事件主队列。
     *
     * @param productChangedQueue 商品变更事件主队列
     * @param productCacheExchange 商品变更事件交换机
     * @return 商品变更事件队列绑定
     */
    @Bean
    public Binding productChangedBinding(Queue productChangedQueue, DirectExchange productCacheExchange) {
        return BindingBuilder.bind(productChangedQueue)
                .to(productCacheExchange)
                .with(ProductCacheMessageConstants.PRODUCT_CHANGED_ROUTING_KEY);
    }

    /**
     * 声明商品变更事件死信交换机。
     *
     * @return 商品变更事件死信交换机
     */
    @Bean
    public DirectExchange productCacheDeadLetterExchange() {
        return new DirectExchange(
                ProductCacheMessageConstants.PRODUCT_CACHE_DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    /**
     * 声明商品变更事件死信队列。
     *
     * @return 商品变更事件死信队列
     */
    @Bean
    public Queue productChangedDeadLetterQueue() {
        return QueueBuilder.durable(
                ProductCacheMessageConstants.PRODUCT_CHANGED_DEAD_LETTER_QUEUE
        ).build();
    }

    /**
     * 绑定商品变更事件死信队列。
     *
     * @param productChangedDeadLetterQueue 商品变更事件死信队列
     * @param productCacheDeadLetterExchange 商品变更事件死信交换机
     * @return 商品变更事件死信队列绑定
     */
    @Bean
    public Binding productChangedDeadLetterBinding(Queue productChangedDeadLetterQueue, DirectExchange productCacheDeadLetterExchange) {
        return BindingBuilder.bind(productChangedDeadLetterQueue)
                .to(productCacheDeadLetterExchange)
                .with(ProductCacheMessageConstants.PRODUCT_CHANGED_DEAD_LETTER_ROUTING_KEY);
    }
}
package cn.fango.mall.admin.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp
        .SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 热点订单确认死信消费者的 RabbitMQ 容器配置。
 *
 * <p>确认条件尚未满足时，消费者必须拒绝消息并发送至延迟重试队列；
 * 不能自动确认后永久丢失。</p>
 */
@Configuration
public class HotStockOrderDeadLetterListenerConfig {

    /**
     * 创建热点订单确认死信消费者的手动确认容器。
     *
     * @param configurer Spring Boot RabbitMQ 容器默认配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 热点订单确认死信消费者容器工厂
     */
    @Bean(
            "hotStockOrderDeadLetterListenerContainerFactory"
    )
    public SimpleRabbitListenerContainerFactory
    hotStockOrderDeadLetterListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        configurer.configure(factory, connectionFactory);

        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(true);
        factory.setPrefetchCount(1);

        return factory;
    }
}
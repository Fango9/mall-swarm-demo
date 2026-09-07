package cn.fango.mall.portal.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp
        .SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 热点库存确认事件 Portal 死信消费者的 RabbitMQ 容器配置。
 */
@Configuration
public class HotStockOrderConfirmedDeadLetterListenerConfig {

    /**
     * 创建热点库存确认事件死信消费者的手动确认容器。
     *
     * @param configurer Spring Boot RabbitMQ 容器默认配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 热点库存确认事件死信消费者容器工厂
     */
    @Bean(
            "hotStockOrderConfirmedDeadLetterListenerContainerFactory"
    )
    public SimpleRabbitListenerContainerFactory
    hotStockOrderConfirmedDeadLetterListenerContainerFactory(
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
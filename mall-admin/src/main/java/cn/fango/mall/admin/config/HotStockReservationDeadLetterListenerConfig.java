package cn.fango.mall.admin.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp
        .SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 热点库存预占用死信补偿消费者的 RabbitMQ 容器配置。
 *
 * <p>补偿消费者必须在 Redis 原子回补成功后才确认消息。回补基础设施暂时不可用时，
 * 消息会重新入队等待恢复，不能被丢弃。</p>
 */
@Configuration
public class HotStockReservationDeadLetterListenerConfig {

    /**
     * 创建热点库存预占用死信补偿消费者的手动确认容器。
     *
     * @param configurer Spring Boot RabbitMQ 容器默认配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 仅供热点库存预占用死信消费者使用的容器工厂
     */
    @Bean(
            "hotStockReservationDeadLetterListenerContainerFactory"
    )
    public SimpleRabbitListenerContainerFactory
    hotStockReservationDeadLetterListenerContainerFactory(
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
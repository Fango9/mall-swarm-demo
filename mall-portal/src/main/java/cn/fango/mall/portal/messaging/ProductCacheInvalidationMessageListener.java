package cn.fango.mall.portal.messaging;

import cn.fango.mall.common.event.ProductChangedEvent;
import cn.fango.mall.common.messaging.ProductCacheMessageConstants;
import cn.fango.mall.portal.cache.PortalProductCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 消费 Admin 商品变更事件并失效 Portal 商品浏览缓存。
 *
 * <p>缓存删除是幂等操作，因此消息重复投递不会产生错误。
 * 非法消息或 Redis 失效失败的消息会被拒绝并路由到死信队列。</p>
 */
@Component
public class ProductCacheInvalidationMessageListener {

    /**
     * Spring Boot 配置的 JSON 反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * Portal 商品 Cache Aside 服务。
     */
    private final PortalProductCacheService portalProductCacheService;

    /**
     * 创建商品缓存失效消息监听器。
     *
     * @param objectMapper JSON 反序列化工具
     * @param portalProductCacheService 商品 Cache Aside 服务
     */
    public ProductCacheInvalidationMessageListener(ObjectMapper objectMapper, PortalProductCacheService portalProductCacheService) {
        this.objectMapper = objectMapper;
        this.portalProductCacheService = portalProductCacheService;
    }

    /**
     * 消费一条商品变更事件并失效受影响的 Redis 缓存。
     *
     * @param message RabbitMQ 原始消息
     */
    @RabbitListener(queues = ProductCacheMessageConstants.PRODUCT_CHANGED_QUEUE)
    public void handleProductChanged(Message message) {
        ProductChangedEvent event = deserializeEvent(message);

        try {
            portalProductCacheService.invalidateProductCaches(event.productId());
        } catch (RuntimeException exception) {
            throw new AmqpRejectAndDontRequeueException(
                    "商品缓存失效失败，事件将进入死信队列",
                    exception
            );
        }
    }

    /**
     * 将 RabbitMQ 原始消息反序列化并校验为商品变更事件。
     *
     * @param message RabbitMQ 原始消息
     * @return 已校验的商品变更事件
     */
    private ProductChangedEvent deserializeEvent(Message message) {
        String eventJson = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            ProductChangedEvent event =
                    objectMapper.readValue(eventJson, ProductChangedEvent.class);

            if (event == null
                    || !StringUtils.hasText(event.eventId())
                    || event.productId() == null
                    || event.productId() <= 0) {
                throw new AmqpRejectAndDontRequeueException(
                        "商品变更事件缺少 eventId 或有效 productId"
                );
            }

            return event;
        } catch (JsonProcessingException exception) {
            throw new AmqpRejectAndDontRequeueException(
                    "商品变更事件 JSON 格式非法",
                    exception
            );
        }
    }
}
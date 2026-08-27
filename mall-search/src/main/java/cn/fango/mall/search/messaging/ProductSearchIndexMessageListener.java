package cn.fango.mall.search.messaging;

import cn.fango.mall.common.event.ProductChangedEvent;
import cn.fango.mall.common.messaging.ProductSearchMessageConstants;
import cn.fango.mall.search.service.ProductSearchIndexService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 消费商品变更事件并同步 Elasticsearch 商品索引。
 *
 * <p>索引同步按商品当前状态执行：重复消息只会再次覆盖同一文档，
 * 或再次删除同一文档，因此可安全处理至少一次投递。</p>
 */
@Component
public class ProductSearchIndexMessageListener {

    /**
     * Spring Boot 配置的 JSON 反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 商品搜索索引同步服务。
     */
    private final ProductSearchIndexService productSearchIndexService;

    /**
     * 创建商品搜索索引消息监听器。
     *
     * @param objectMapper JSON 反序列化工具
     * @param productSearchIndexService 商品搜索索引同步服务
     */
    public ProductSearchIndexMessageListener(
            ObjectMapper objectMapper,
            ProductSearchIndexService productSearchIndexService
    ) {
        this.objectMapper = objectMapper;
        this.productSearchIndexService = productSearchIndexService;
    }

    /**
     * 消费一条商品变更事件并同步受影响商品的 Elasticsearch 索引。
     *
     * @param message RabbitMQ 原始消息
     */
    @RabbitListener(queues = ProductSearchMessageConstants.PRODUCT_CHANGED_QUEUE)
    public void handleProductChanged(Message message) {
        ProductChangedEvent event = deserializeEvent(message);

        try {
            productSearchIndexService.synchronizeProduct(event.productId());
        } catch (RuntimeException exception) {
            throw new AmqpRejectAndDontRequeueException(
                    "商品搜索索引同步失败，事件将进入死信队列",
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
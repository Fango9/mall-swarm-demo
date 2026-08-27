package cn.fango.mall.search.messaging;

import cn.fango.mall.common.event.ProductChangedEvent;
import cn.fango.mall.search.service.ProductSearchIndexService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 商品搜索索引消息监听器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ProductSearchIndexMessageListenerTest {

    @Mock
    private ProductSearchIndexService productSearchIndexService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProductSearchIndexMessageListener listener;

    /**
     * 创建待测监听器。
     */
    @BeforeEach
    void setUp() {
        listener = new ProductSearchIndexMessageListener(
                objectMapper,
                productSearchIndexService
        );
    }

    /**
     * 合法商品变更事件应触发对应商品的索引同步。
     *
     * @throws Exception JSON 序列化失败时抛出
     */
    @Test
    void handleProductChangedSynchronizesProductWhenEventIsValid() throws Exception {
        ProductChangedEvent event = new ProductChangedEvent("event-1", 1L);
        Message message = new Message(objectMapper.writeValueAsBytes(event));

        listener.handleProductChanged(message);

        verify(productSearchIndexService).synchronizeProduct(1L);
    }

    /**
     * 索引同步失败时应拒绝消息且不重新入队。
     *
     * @throws Exception JSON 序列化失败时抛出
     */
    @Test
    void handleProductChangedRejectsMessageWhenSynchronizationFails() throws Exception {
        ProductChangedEvent event = new ProductChangedEvent("event-1", 1L);
        Message message = new Message(objectMapper.writeValueAsBytes(event));

        doThrow(new IllegalStateException("Elasticsearch 不可用"))
                .when(productSearchIndexService)
                .synchronizeProduct(1L);

        assertThatThrownBy(() -> listener.handleProductChanged(message))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("商品搜索索引同步失败，事件将进入死信队列");
    }

    /**
     * 缺少有效事件 ID 的消息应拒绝且不重新入队。
     */
    @Test
    void handleProductChangedRejectsMessageWhenEventIdIsBlank() {
        Message message = new Message(
                "{\"eventId\":\"\",\"productId\":1}".getBytes()
        );

        assertThatThrownBy(() -> listener.handleProductChanged(message))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("商品变更事件缺少 eventId 或有效 productId");
    }
}
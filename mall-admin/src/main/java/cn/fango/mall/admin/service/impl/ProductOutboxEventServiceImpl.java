package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.ProductOutboxErrorCode;
import cn.fango.mall.admin.service.ProductOutboxEventService;
import cn.fango.mall.common.event.ProductChangedEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsOutboxEventMapper;
import cn.fango.mall.mbg.model.PmsOutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

/**
 * 商品变更 Outbox 事件写入服务实现。
 */
@Service
public class ProductOutboxEventServiceImpl implements ProductOutboxEventService {

    /**
     * 商品变更事件对应的聚合类型。
     */
    private static final String PRODUCT_AGGREGATE_TYPE = "PRODUCT";

    /**
     * 商品或 SKU 变更后发布的事件类型。
     */
    private static final String PRODUCT_CHANGED_EVENT_TYPE = "PRODUCT_CHANGED";

    /**
     * 已随本地事务提交、等待 RabbitMQ 发布的 Outbox 状态。
     */
    private static final String PENDING_STATUS = "PENDING";

    /**
     * 商品 Outbox 事件基础数据访问对象。
     */
    private final PmsOutboxEventMapper pmsOutboxEventMapper;

    /**
     * Spring Boot 配置的 JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建商品变更 Outbox 事件写入服务。
     *
     * @param pmsOutboxEventMapper 商品 Outbox 事件基础数据访问对象
     * @param objectMapper JSON 序列化工具
     */
    public ProductOutboxEventServiceImpl(PmsOutboxEventMapper pmsOutboxEventMapper, ObjectMapper objectMapper) {
        this.pmsOutboxEventMapper = pmsOutboxEventMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 在调用方当前事务中保存一条待发布的商品变更 Outbox 事件。
     *
     * <p>事件载荷包含唯一事件 ID 与商品主键。若事件序列化或数据库写入失败，
     * 本方法抛出业务异常，由外层商品或 SKU 事务整体回滚。</p>
     *
     * @param productId 发生变化的商品主键
     * @throws ApiException 商品主键非法、事件序列化失败或事件写入失败时抛出
     */
    @Override
    public void recordProductChanged(Long productId) {
        if (productId == null || productId <= 0) {
            throw new ApiException(ProductOutboxErrorCode.OUTBOX_EVENT_CREATE_FAILED);
        }

        String eventId = UUID.randomUUID().toString();
        ProductChangedEvent productChangedEvent =
                new ProductChangedEvent(eventId, productId);

        PmsOutboxEvent outboxEvent = new PmsOutboxEvent();
        outboxEvent.setEventId(eventId);
        outboxEvent.setAggregateType(PRODUCT_AGGREGATE_TYPE);
        outboxEvent.setAggregateId(productId);
        outboxEvent.setEventType(PRODUCT_CHANGED_EVENT_TYPE);
        outboxEvent.setStatus(PENDING_STATUS);
        outboxEvent.setNextRetryAt(new Date());

        try {
            outboxEvent.setPayload(
                    objectMapper.writeValueAsString(productChangedEvent)
            );
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    ProductOutboxErrorCode.OUTBOX_EVENT_CREATE_FAILED,
                    exception
            );
        }

        int inserted = pmsOutboxEventMapper.insertSelective(outboxEvent);
        if (inserted != 1 || outboxEvent.getId() == null) {
            throw new ApiException(ProductOutboxErrorCode.OUTBOX_EVENT_CREATE_FAILED);
        }
    }
}
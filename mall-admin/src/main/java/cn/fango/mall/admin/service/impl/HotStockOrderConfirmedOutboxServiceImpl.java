package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.service.HotStockOrderConfirmedOutboxService;
import cn.fango.mall.common.event.HotStockOrderConfirmedEvent;
import cn.fango.mall.common.event.HotStockOrderCreatedEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsOutboxEventMapper;
import cn.fango.mall.mbg.model.PmsOutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

/**
 * 热点库存预占用已确认 Outbox 写入服务实现。
 */
@Service
public class HotStockOrderConfirmedOutboxServiceImpl
        implements HotStockOrderConfirmedOutboxService {

    /**
     * Outbox 聚合类型。
     */
    private static final String AGGREGATE_TYPE = "HOT_STOCK_ORDER";

    /**
     * Outbox 事件类型。
     */
    private static final String EVENT_TYPE = "HOT_STOCK_ORDER_CONFIRMED";

    /**
     * 待发布状态。
     */
    private static final String PENDING_STATUS = "PENDING";

    /**
     * Admin Outbox 数据访问对象。
     */
    private final PmsOutboxEventMapper pmsOutboxEventMapper;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建热点库存确认 Outbox 写入服务。
     *
     * @param pmsOutboxEventMapper Admin Outbox 数据访问对象
     * @param objectMapper JSON 序列化工具
     */
    public HotStockOrderConfirmedOutboxServiceImpl(
            PmsOutboxEventMapper pmsOutboxEventMapper,
            ObjectMapper objectMapper
    ) {
        this.pmsOutboxEventMapper = pmsOutboxEventMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 在当前事务中写入一条热点库存已确认事件。
     *
     * @param event Portal 发布的热点订单确认事件
     */
    @Override
    public void recordConfirmed(HotStockOrderCreatedEvent event) {
        validateEvent(event);

        String eventId = UUID.randomUUID().toString();
        HotStockOrderConfirmedEvent confirmedEvent = new HotStockOrderConfirmedEvent(
                        eventId,
                        event.orderId(),
                        event.orderSn()
                );

        PmsOutboxEvent outboxEvent = new PmsOutboxEvent();
        outboxEvent.setEventId(eventId);
        outboxEvent.setAggregateType(AGGREGATE_TYPE);
        outboxEvent.setAggregateId(event.orderId());
        outboxEvent.setEventType(EVENT_TYPE);
        outboxEvent.setStatus(PENDING_STATUS);
        outboxEvent.setNextRetryAt(new Date());
        outboxEvent.setPayload(serializeEvent(confirmedEvent));

        int inserted = pmsOutboxEventMapper.insertSelective(outboxEvent);
        if (inserted != 1) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_ORDER_CONFIRM_OUTBOX_FAILED
            );
        }
    }

    /**
     * 校验热点订单确认事件的公共字段。
     *
     * @param event 待校验的事件
     */
    private void validateEvent(HotStockOrderCreatedEvent event) {
        if (event == null
                || !StringUtils.hasText(event.eventId())
                || event.orderId() == null
                || event.orderId() <= 0
                || !StringUtils.hasText(event.orderSn())) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_ORDER_CONFIRM_OUTBOX_FAILED
            );
        }
    }

    /**
     * 序列化 Admin Outbox 事件。
     *
     * @param event 待序列化事件
     * @return JSON 事件载荷
     */
    private String serializeEvent(
            HotStockOrderConfirmedEvent event
    ) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_ORDER_CONFIRM_OUTBOX_FAILED,
                    exception
            );
        }
    }
}
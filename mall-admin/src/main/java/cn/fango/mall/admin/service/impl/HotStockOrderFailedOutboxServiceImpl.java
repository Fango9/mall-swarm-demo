package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.service.HotStockOrderFailedOutboxService;
import cn.fango.mall.common.event.HotStockOrderFailedEvent;
import cn.fango.mall.common.event.HotStockReservationEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsOutboxEventMapper;
import cn.fango.mall.mbg.model.PmsOutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * 热点库存预占用最终失败 Outbox 写入服务实现。
 */
@Service
public class HotStockOrderFailedOutboxServiceImpl
        implements HotStockOrderFailedOutboxService {

    /**
     * Outbox 聚合类型。
     */
    private static final String AGGREGATE_TYPE =
            "HOT_STOCK_ORDER_FAILURE";

    /**
     * Outbox 事件类型。
     */
    private static final String EVENT_TYPE =
            "HOT_STOCK_ORDER_FAILED";

    /**
     * 待发布状态。
     */
    private static final String PENDING_STATUS = "PENDING";

    /**
     * 确定性事件 ID 的命名空间。
     */
    private static final String EVENT_ID_NAMESPACE =
            "hot-stock-order-failed:";

    /**
     * 失败事件尚无 Portal 订单主键时使用的技术聚合主键。
     *
     * <p>Portal 通过 orderSn 定位订单；该值不参与业务幂等判断。</p>
     */
    private static final long TECHNICAL_AGGREGATE_ID = 0L;

    /**
     * Admin Outbox 数据访问对象。
     */
    private final PmsOutboxEventMapper pmsOutboxEventMapper;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建热点库存失败 Outbox 写入服务。
     *
     * @param pmsOutboxEventMapper Admin Outbox 数据访问对象
     * @param objectMapper JSON 序列化工具
     */
    public HotStockOrderFailedOutboxServiceImpl(
            PmsOutboxEventMapper pmsOutboxEventMapper,
            ObjectMapper objectMapper
    ) {
        this.pmsOutboxEventMapper = pmsOutboxEventMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 幂等写入热点库存预占用最终失败事件。
     *
     * @param event 已完成 Redis 库存回补的热点库存预占用事件
     */
    @Override
    @Transactional
    public void recordFailure(HotStockReservationEvent event) {
        validateEvent(event);

        String eventId = createDeterministicEventId(event.orderSn());
        HotStockOrderFailedEvent failedEvent =
                new HotStockOrderFailedEvent(
                        eventId,
                        event.orderSn()
                );

        PmsOutboxEvent outboxEvent = new PmsOutboxEvent();
        outboxEvent.setEventId(eventId);
        outboxEvent.setAggregateType(AGGREGATE_TYPE);
        outboxEvent.setAggregateId(TECHNICAL_AGGREGATE_ID);
        outboxEvent.setEventType(EVENT_TYPE);
        outboxEvent.setStatus(PENDING_STATUS);
        outboxEvent.setNextRetryAt(new Date());
        outboxEvent.setPayload(serializeEvent(failedEvent));

        try {
            int inserted = pmsOutboxEventMapper.insertSelective(outboxEvent);

            if (inserted != 1) {
                throw new ApiException(
                        StockReservationErrorCode
                                .HOT_STOCK_ORDER_FAILURE_OUTBOX_FAILED
                );
            }
        } catch (DuplicateKeyException exception) {
            // 相同 orderSn 的失败通知此前已写入 Outbox，视为幂等成功。
        }
    }

    /**
     * 校验热点库存预占用事件。
     *
     * @param event 待校验的事件
     */
    private void validateEvent(HotStockReservationEvent event) {
        if (event == null
                || !StringUtils.hasText(event.reservationNo())
                || !StringUtils.hasText(event.orderSn())) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_ORDER_FAILURE_OUTBOX_FAILED
            );
        }
    }

    /**
     * 根据订单编号生成确定性的失败事件 ID。
     *
     * @param orderSn 订单编号
     * @return 固定 UUID 字符串
     */
    private String createDeterministicEventId(String orderSn) {
        return UUID.nameUUIDFromBytes(
                (EVENT_ID_NAMESPACE + orderSn)
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    /**
     * 序列化失败事件。
     *
     * @param event 待序列化的失败事件
     * @return JSON 事件载荷
     */
    private String serializeEvent(HotStockOrderFailedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    StockReservationErrorCode
                            .HOT_STOCK_ORDER_FAILURE_OUTBOX_FAILED,
                    exception
            );
        }
    }
}
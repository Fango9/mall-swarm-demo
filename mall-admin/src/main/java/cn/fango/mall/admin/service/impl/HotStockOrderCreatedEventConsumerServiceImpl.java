package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.mapper.PmsOrderEventConsumerMapper;
import cn.fango.mall.admin.service.HotStockOrderConfirmedOutboxService;
import cn.fango.mall.admin.service.HotStockOrderCreatedEventConsumerService;
import cn.fango.mall.common.event.HotStockOrderCreatedEvent;
import cn.fango.mall.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 热点库存预占用订单确认事件消费服务实现。
 */
@Service
public class HotStockOrderCreatedEventConsumerServiceImpl
        implements HotStockOrderCreatedEventConsumerService {

    /**
     * 消费幂等日志中的消费者名称。
     */
    private static final String CONSUMER_NAME = "hot-stock-order-confirm";

    /**
     * 订单事件消费数据访问对象。
     */
    private final PmsOrderEventConsumerMapper pmsOrderEventConsumerMapper;

    /**
     * 热点库存预占用已确认 Outbox 写入服务。
     */
    private final HotStockOrderConfirmedOutboxService confirmedOutboxService;

    /**
     * 创建热点订单确认事件消费服务。
     *
     * @param pmsOrderEventConsumerMapper 订单事件消费数据访问对象
     */
    public HotStockOrderCreatedEventConsumerServiceImpl(
            PmsOrderEventConsumerMapper pmsOrderEventConsumerMapper,
            HotStockOrderConfirmedOutboxService confirmedOutboxService
    ) {
        this.pmsOrderEventConsumerMapper = pmsOrderEventConsumerMapper;
        this.confirmedOutboxService = confirmedOutboxService;
    }

    /**
     * 在一个 MySQL 本地事务中幂等确认热点库存预占用。
     *
     * <p>若 Redis Stream 消费者尚未把 LOCKED 记录落库，抛出可重试异常；
     * 消费日志插入会随事务回滚，因此下一次消息重试仍能获得确认机会。</p>
     *
     * @param event Portal Outbox 发布的热点订单确认事件
     */
    @Override
    @Transactional
    public void confirmHotStockReservation(HotStockOrderCreatedEvent event) {
        validateEvent(event);

        // 插入消费记录日志
        int consumeLogInserted = pmsOrderEventConsumerMapper.insertIgnoreConsumeLog(event.eventId(), CONSUMER_NAME);

        if (consumeLogInserted == 0) {
            return;
        }

        // 更新预占记录状态为 LOCKED -> CONFIRMED
        int confirmed = pmsOrderEventConsumerMapper.confirmLockedReservations(event.orderSn());

        if (confirmed <= 0) {
            throw new ApiException(StockReservationErrorCode.HOT_STOCK_ORDER_CONFIRM_PENDING);
        }

        // 写入一条订单预占记录已确认 outbox
        confirmedOutboxService.recordConfirmed(event);
    }

    /**
     * 校验热点订单确认事件的必填字段。
     *
     * @param event 待校验的热点订单确认事件
     */
    private void validateEvent(HotStockOrderCreatedEvent event) {
        if (event == null
                || !StringUtils.hasText(event.eventId())
                || event.orderId() == null
                || event.orderId() <= 0
                || !StringUtils.hasText(event.orderSn())) {
            throw new ApiException(
                    StockReservationErrorCode
                            .ORDER_CREATED_EVENT_INVALID
            );
        }
    }
}
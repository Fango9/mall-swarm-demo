package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.mapper.PmsOrderEventConsumerMapper;
import cn.fango.mall.admin.service.OrderCreatedEventConsumerService;
import cn.fango.mall.common.event.OrderCreatedEvent;
import cn.fango.mall.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 订单创建事件的库存预占确认服务实现。
 */
@Service
public class OrderCreatedEventConsumerServiceImpl
        implements OrderCreatedEventConsumerService {

    private static final String CONSUMER_NAME =
            "stock-reservation-confirm";

    /**
     * 订单事件消费数据访问对象。
     */
    private final PmsOrderEventConsumerMapper
            pmsOrderEventConsumerMapper;

    /**
     * 创建订单事件消费服务。
     *
     * @param pmsOrderEventConsumerMapper 订单事件消费数据访问对象
     */
    public OrderCreatedEventConsumerServiceImpl(
            PmsOrderEventConsumerMapper pmsOrderEventConsumerMapper
    ) {
        this.pmsOrderEventConsumerMapper =
                pmsOrderEventConsumerMapper;
    }

    /**
     * 在一个本地事务中记录消费幂等日志，并确认库存预占。
     *
     * <p>重复事件返回时，唯一键约束使插入返回 0，本方法直接结束；
     * 首次处理但库存确认失败时抛出异常，使消费日志与状态更新一起回滚。</p>
     *
     * @param event 已反序列化的订单创建事件
     */
    @Override
    @Transactional
    public void confirmStockReservation(OrderCreatedEvent event) {
        validateEvent(event);

        int consumeLogInserted = pmsOrderEventConsumerMapper.insertIgnoreConsumeLog(
                        event.eventId(),
                        CONSUMER_NAME
                );
        if (consumeLogInserted == 0) {
            return;
        }

        int confirmed = pmsOrderEventConsumerMapper.confirmLockedReservations(event.orderSn());
        if (confirmed <= 0) {
            throw new ApiException(
                    StockReservationErrorCode
                            .STOCK_RESERVATION_CONFIRM_FAILED
            );
        }
    }

    /**
     * 校验订单创建事件的必填字段。
     *
     * @param event 待校验的订单创建事件
     */
    private void validateEvent(OrderCreatedEvent event) {
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
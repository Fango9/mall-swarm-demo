package cn.fango.mall.portal.service.impl;

import cn.fango.mall.common.event.HotStockOrderConfirmedEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.portal.api.OrderErrorCode;
import cn.fango.mall.portal.mapper.OmsHotStockOrderEventConsumerMapper;
import cn.fango.mall.portal.service.HotStockOrderConfirmedEventConsumerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 热点库存预占用已确认事件消费服务实现。
 */
@Service
public class HotStockOrderConfirmedEventConsumerServiceImpl
        implements HotStockOrderConfirmedEventConsumerService {

    /**
     * Portal 消费幂等日志中的消费者名称。
     */
    private static final String CONSUMER_NAME =
            "hot-stock-order-confirmed";

    /**
     * 热点订单确认事件数据访问对象。
     */
    private final OmsHotStockOrderEventConsumerMapper
            eventConsumerMapper;

    /**
     * 创建热点库存确认事件消费服务。
     *
     * @param eventConsumerMapper 热点订单确认事件数据访问对象
     */
    public HotStockOrderConfirmedEventConsumerServiceImpl(
            OmsHotStockOrderEventConsumerMapper eventConsumerMapper
    ) {
        this.eventConsumerMapper = eventConsumerMapper;
    }

    /**
     * 在一个 Portal MySQL 本地事务中幂等推进热点订单状态。
     *
     * <p>消费日志与状态更新必须同时提交。若订单不存在、订单编号不匹配或订单不再是
     * {@code PENDING_STOCK}，抛出异常并回滚消费日志，使消息保留重试与对账机会。</p>
     *
     * @param event Admin Outbox 发布的热点库存预占用已确认事件
     */
    @Override
    @Transactional
    public void promoteOrderToPendingPayment(
            HotStockOrderConfirmedEvent event
    ) {
        validateEvent(event);

        int consumeLogInserted = eventConsumerMapper.insertIgnoreConsumeLog(
                        event.eventId(),
                        CONSUMER_NAME,
                        event.orderId(),
                        event.orderSn()
                );

        if (consumeLogInserted == 0) {
            return;
        }

        int updated = eventConsumerMapper.promotePendingStockOrder(
                event.orderId(),
                event.orderSn()
        );

        if (updated != 1) {
            throw new ApiException(
                    OrderErrorCode.HOT_STOCK_ORDER_CONFIRM_FAILED
            );
        }
    }

    /**
     * 校验热点库存确认事件的必填字段。
     *
     * @param event 待校验的热点库存确认事件
     */
    private void validateEvent(HotStockOrderConfirmedEvent event) {
        if (event == null
                || !StringUtils.hasText(event.eventId())
                || event.orderId() == null
                || event.orderId() <= 0
                || !StringUtils.hasText(event.orderSn())) {
            throw new ApiException(
                    OrderErrorCode.HOT_STOCK_ORDER_CONFIRM_FAILED
            );
        }
    }
}
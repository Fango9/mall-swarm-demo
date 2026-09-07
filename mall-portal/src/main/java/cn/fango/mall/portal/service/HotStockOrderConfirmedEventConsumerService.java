package cn.fango.mall.portal.service;

import cn.fango.mall.common.event.HotStockOrderConfirmedEvent;

/**
 * 热点库存预占用已确认事件消费服务。
 */
public interface HotStockOrderConfirmedEventConsumerService {

    /**
     * 幂等地将热点订单推进为待支付。
     *
     * @param event Admin Outbox 发布的热点库存预占用已确认事件
     */
    void promoteOrderToPendingPayment(
            HotStockOrderConfirmedEvent event
    );
}
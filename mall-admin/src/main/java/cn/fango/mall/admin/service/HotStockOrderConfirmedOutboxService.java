package cn.fango.mall.admin.service;

import cn.fango.mall.common.event.HotStockOrderCreatedEvent;

/**
 * 热点库存预占用已确认 Outbox 写入服务。
 */
public interface HotStockOrderConfirmedOutboxService {

    /**
     * 在当前 Admin MySQL 事务中写入库存已确认事件。
     *
     * @param event 已通过校验的热点订单确认事件
     */
    void recordConfirmed(
            HotStockOrderCreatedEvent event
    );

}
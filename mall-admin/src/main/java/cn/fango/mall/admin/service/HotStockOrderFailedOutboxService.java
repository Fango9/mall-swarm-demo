package cn.fango.mall.admin.service;

import cn.fango.mall.common.event.HotStockReservationEvent;

/**
 * 热点库存预占用最终失败 Outbox 写入服务。
 */
public interface HotStockOrderFailedOutboxService {

    /**
     * 在当前 Admin MySQL 事务中写入热点订单库存失败事件。
     *
     * @param event 已完成 Redis 库存回补的热点库存预占用事件
     */
    void recordFailure(
            HotStockReservationEvent event
    );
}
package cn.fango.mall.admin.service;

import cn.fango.mall.common.event.HotStockOrderCreatedEvent;

/**
 * 热点库存预占用订单确认事件消费服务。
 *
 * <p>服务在 Admin MySQL 本地事务中记录消费幂等日志，并确认对应的 LOCKED
 * 库存预占。Redis 状态终结由监听器在本方法成功返回后执行。</p>
 */
public interface HotStockOrderCreatedEventConsumerService {

    /**
     * 幂等确认 Portal 已提交的热点库存预占用订单。
     *
     * @param event Portal Outbox 发布的热点订单确认事件
     */
    void confirmHotStockReservation(HotStockOrderCreatedEvent event);
}
package cn.fango.mall.admin.service;

import cn.fango.mall.common.event.OrderCreatedEvent;

/**
 * 订单创建事件的库存预占确认服务。
 */
public interface OrderCreatedEventConsumerService {

    /**
     * 幂等地确认订单对应的库存预占。
     *
     * @param event 已反序列化的订单创建事件
     */
    void confirmStockReservation(OrderCreatedEvent event);
}
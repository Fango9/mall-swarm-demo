package cn.fango.mall.common.event;

/**
 * Portal 已提交热点库存预占用订单的可靠事件。
 *
 * <p>事件仅在 Portal 订单、本地订单明细和 Outbox 处于同一 MySQL 事务提交后产生。
 * Admin 可将其与热点库存预占用落库结果汇合；两者到达顺序不确定，消费者必须幂等重试。</p>
 *
 * @param eventId Portal Outbox 事件唯一编号
 * @param orderId Portal 订单主键
 * @param orderSn 订单编号，也是热点库存预占用编号
 */
public record HotStockOrderCreatedEvent(
        String eventId,
        Long orderId,
        String orderSn
) {
}
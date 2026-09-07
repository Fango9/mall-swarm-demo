package cn.fango.mall.common.event;

/**
 * 热点库存预占用已确认事件。
 *
 * <p>该事件由 Admin 在确认 MySQL 锁库存的本地事务中写入 Outbox，随后可靠投递给
 * Portal。下游使用 {@code eventId} 幂等处理，不能因重复投递重复变更订单状态。</p>
 *
 * @param eventId Admin Outbox 事件唯一编号
 * @param orderId Portal 订单主键
 * @param orderSn 订单编号，也是热点库存预占用编号
 */
public record HotStockOrderConfirmedEvent(
        String eventId,
        Long orderId,
        String orderSn
) {
}
package cn.fango.mall.common.event;

/**
 * 热点库存预占用最终失败事件。
 *
 * <p>该事件仅在 Admin 已通过 Redis Lua 将库存回补并写入 {@code FAILED} 终态后产生。
 * Portal 必须以 {@code eventId} 幂等处理，重复投递不能重复变更订单。</p>
 *
 * @param eventId Admin Outbox 事件唯一编号
 * @param orderSn 订单编号，也是热点库存预占用编号
 */
public record HotStockOrderFailedEvent(
        String eventId,
        String orderSn
) {
}
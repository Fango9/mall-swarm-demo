package cn.fango.mall.common.event;

/**
 * Portal 创建订单后写入 Outbox 的订单创建事件。
 *
 * <p>该事件按至少一次投递设计。eventId 在消息重复投递时保持不变，
 * Admin 使用它记录消费幂等日志。</p>
 *
 * @param eventId Outbox 事件 UUID
 * @param orderId Portal 订单主键
 * @param orderSn 订单编号，也是库存预占编号
 */
public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        String orderSn
) {
}
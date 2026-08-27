package cn.fango.mall.common.event;

/**
 * Admin 商品数据发生变化后发布的领域事件。
 *
 * <p>SKU 变更同样携带所属商品主键。Portal 收到事件后只做幂等缓存失效，
 * 不以消息内容直接更新缓存数据。</p>
 *
 * @param eventId 事件唯一标识，用于日志追踪和重复投递识别
 * @param productId 发生变化的商品主键
 */
public record ProductChangedEvent(String eventId, Long productId) {
}
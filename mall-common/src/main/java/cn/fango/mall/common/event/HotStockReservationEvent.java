package cn.fango.mall.common.event;

import java.util.List;

/**
 * Redis Stream 经可靠 Relay 投递到 RabbitMQ 的热点库存预占用事件。
 *
 * <p>事件按至少一次投递设计。下游必须使用 {@code reservationNo} 作为幂等键，
 * 不能使用 RabbitMQ messageId、Redis Stream recordId 或消费者本地序号。</p>
 *
 * @param reservationNo 全链路唯一库存预占用编号
 * @param orderSn Portal 订单编号
 * @param expireAtMillis 预占用超时的 Unix 时间戳，单位毫秒
 * @param items 本次预占用的 SKU 明细
 */
public record HotStockReservationEvent(
        String reservationNo,
        String orderSn,
        long expireAtMillis,
        List<Item> items
) {

    /**
     * 单个 SKU 的热点库存预占用明细。
     *
     * @param skuId SKU 主键
     * @param quantity 本次预扣数量
     */
    public record Item(
            Long skuId,
            Integer quantity
    ) {
    }
}

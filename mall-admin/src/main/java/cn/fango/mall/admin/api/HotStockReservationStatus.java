package cn.fango.mall.admin.api;

/**
 * 热点 SKU Redis 预占用的领域状态。
 *
 * <p>此状态保存于 Redis 预占用记录中，用于 Relay、超时补偿和对账；
 * 它不同于 MySQL 中 {@link StockReservationStatus} 的库存锁定状态。</p>
 */
public enum HotStockReservationStatus {

    /**
     * Redis Lua 已原子扣减可预占库存，并已写入 Redis Stream。
     */
    ACCEPTED,

    /**
     * Relay 已收到 RabbitMQ Publisher Confirm，Stream 消息已确认。
     */
    RELAYED,

    /**
     * RabbitMQ 消费者已成功将锁定库存和预占用记录提交到 MySQL。
     */
    PERSISTED,

    /**
     * Portal 订单已提交，Admin 已确认对应 MySQL 锁定库存。
     *
     * <p>进入该终态后必须移除 Redis 超时 ZSET，库存不再因预占用超时释放。</p>
     */
    CONFIRMED,

    /**
     * MySQL 落库被判定为不可恢复失败，Redis 库存已补回。
     */
    FAILED,

    /**
     * 预占用超时或失败补偿后，Redis 库存已原子补回。
     */
    RELEASED
}

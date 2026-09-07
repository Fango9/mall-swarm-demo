package cn.fango.mall.common.stock;
/**
 * 热点库存预占用受理结果。
 *
 * <p>只有 {@link #ACCEPTED} 和 {@link #ALREADY_ACCEPTED} 表示 Redis Lua
 * 已经可靠地持有预占用；{@link #NOT_HOT} 表示调用方可继续既有 MySQL 预占路径。</p>
 */
public enum HotStockReservationAcceptResult {

    /**
     * 首次调用成功：Redis 已原子预扣、写入预占用状态、Stream 和超时 ZSET。
     */
    ACCEPTED,

    /**
     * 相同 reservationNo 的幂等重试已命中原预占用，不会再次扣减或写入事件。
     */
    ALREADY_ACCEPTED,

    /**
     * 请求包含非热点 SKU，未修改 Redis；调用方应走原有同步 MySQL 预占流程。
     */
    NOT_HOT
}

package cn.fango.mall.admin.cache;

/**
 * Admin 热点 SKU Redis 快速库存过滤使用的键定义。
 *
 * <p>键值保存当前可预占库存，即 MySQL {@code stock - lock_stock} 的缓存投影；
 * 不保存物理总库存，也不替代 MySQL 条件更新的最终裁决。</p>
 */
public final class HotSkuStockCacheKeys {

    /**
     * 热点 SKU 可预占库存键前缀。
     */
    private static final String AVAILABLE_STOCK_KEY_PREFIX = "mall:admin:hot-sku:available:";

    /**
     * 热点库存预占用状态 Hash 键前缀。
     */
    private static final String RESERVATION_KEY_PREFIX = "mall:admin:hot-sku:reservation:";

    /**
     * 热点库存预占用事件 Redis Stream 键。
     */
    private static final String RESERVATION_STREAM_KEY = "mall:admin:hot-sku:reservation:stream";

    /** 热点预占用超时失败通知 Redis Stream 键。 */
    private static final String TIMEOUT_FAILURE_STREAM_KEY =
            "mall:admin:hot-sku:reservation:timeout-failure:stream";

    /**
     * 热点库存预占用超时释放 ZSET 键。
     */
    private static final String RESERVATION_TIMEOUT_ZSET_KEY = "mall:admin:hot-sku:reservation:timeout";

    /**
     * 热点库存人工对账修复维护锁键。
     *
     * <p>锁存在期间，Redis Lua 拒绝新的首次预占用，避免修复快照被并发扣减破坏。
     * 该锁不用于日常下单，也不会由定时任务自动创建。</p>
     */
    private static final String RECONCILIATION_REPAIR_LOCK_KEY =
            "mall:admin:hot-sku:reconciliation:repair:lock";

    /**
     * 工具类不允许创建实例。
     */
    private HotSkuStockCacheKeys() {
    }

    /**
     * 获取指定热点 SKU 的可预占库存 Redis 键。
     *
     * @param skuId SKU 主键
     * @return 热点 SKU 可预占库存 Redis 键
     */
    public static String availableStockKey(Long skuId) {
        if (skuId == null || skuId <= 0) {
            throw new IllegalArgumentException("skuId 必须大于 0");
        }

        return AVAILABLE_STOCK_KEY_PREFIX + skuId;
    }

    /**
     * 获取指定预占用编号的 Redis 状态 Hash 键。
     *
     * @param reservationNo 全链路唯一库存预占用编号
     * @return Redis 预占用状态键
     */
    public static String reservationKey(String reservationNo) {
        if (reservationNo == null || reservationNo.isBlank()) {
            throw new IllegalArgumentException("reservationNo 不能为空");
        }

        return RESERVATION_KEY_PREFIX + reservationNo;
    }

    /**
     * 获取热点库存预占用事件 Stream 键。
     *
     * @return Redis Stream 键
     */
    public static String reservationStreamKey() {
        return RESERVATION_STREAM_KEY;
    }

    /**
     * 获取热点预占用超时失败通知 Stream 键。
     *
     * @return 由超时释放 Lua 原子写入、由失败通知 Relay 消费的 Stream 键
     */
    public static String timeoutFailureStreamKey() {
        return TIMEOUT_FAILURE_STREAM_KEY;
    }

    /**
     * 获取热点库存预占用超时释放 ZSET 键。
     *
     * @return Redis ZSET 键
     */
    public static String reservationTimeoutZsetKey() {
        return RESERVATION_TIMEOUT_ZSET_KEY;
    }

    /**
     * 获取热点库存人工对账修复维护锁键。
     *
     * @return 对账修复维护锁 Redis 键
     */
    public static String reconciliationRepairLockKey() {
        return RECONCILIATION_REPAIR_LOCK_KEY;
    }
}

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
}
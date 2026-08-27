package cn.fango.mall.admin.api;

/**
 * 热点 SKU Redis 快速库存过滤结果。
 */
public enum HotSkuStockFilterResult {

    /**
     * 当前 SKU 不是热点、功能未启用、缓存未初始化或 Redis 暂不可用。
     *
     * <p>调用方必须继续执行 MySQL 条件更新，不得据此拒绝下单。</p>
     */
    NOT_APPLIED,

    /**
     * Redis 已原子预扣可预占库存，后续仍需执行 MySQL 条件更新裁决。
     */
    RESERVED,

    /**
     * Redis 可预占库存不足，可提前拒绝当前请求。
     */
    STOCK_NOT_ENOUGH

}
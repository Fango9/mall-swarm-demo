package cn.fango.mall.admin.service;

import cn.fango.mall.admin.api.HotSkuStockFilterResult;

/**
 * 热点 SKU Redis 快速库存过滤服务。
 *
 * <p>该服务只负责 Redis 快速过滤和预扣补偿；
 * MySQL 条件更新仍是库存预占的最终裁决者。</p>
 */
public interface HotSkuStockFilterService {

    /**
     * 对热点 SKU 尝试原子预扣 Redis 可预占库存。
     *
     * @param skuId SKU 主键
     * @param quantity 本次预占数量
     * @return Redis 快速过滤结果
     */
    HotSkuStockFilterResult tryReserve(Long skuId, Integer quantity);

    /**
     * 将需要补偿或已经成功释放的库存补回 Redis 可预占库存。
     *
     * <p>适用于两种场景：Redis 预扣后 MySQL 预占失败或事务回滚；
     * 以及 MySQL 锁定库存已成功释放并完成事务提交。</p>
     *
     * @param skuId SKU 主键
     * @param quantity 需要补回的库存数量
     */
    void restoreReservedStock(Long skuId, Integer quantity);
}
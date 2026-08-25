package cn.fango.mall.admin.service;

import cn.fango.mall.common.stock.StockReleaseRequest;
import cn.fango.mall.common.stock.StockReservationRequest;

/**
 * SKU 库存预占服务。
 */
public interface StockReservationService {

    /**
     * 按预占编号原子预占多个 SKU 的库存。
     * 相同预占编号且明细相同的重复请求应直接成功，不重复增加锁定库存。
     *
     * @param request 库存预占请求
     * @return 是否预占成功
     */
    boolean reserveStock(StockReservationRequest request);

    /**
     * 按预占编号释放已锁定的全部 SKU 库存。
     * 已释放的重复请求应直接成功，不重复减少锁定库存。
     *
     * @param request 库存释放请求
     * @return 是否释放成功
     */
    boolean releaseStock(StockReleaseRequest request);
}
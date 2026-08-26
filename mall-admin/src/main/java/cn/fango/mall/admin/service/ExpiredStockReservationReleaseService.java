package cn.fango.mall.admin.service;

import cn.fango.mall.mbg.model.PmsStockReservation;

import java.util.Date;
import java.util.List;

/**
 * 过期库存预占释放服务。
 */
public interface ExpiredStockReservationReleaseService {

    /**
     * 查询当前已过期且仍为 LOCKED 的库存预占。
     *
     * @param now 当前时间
     * @param limit 本次最多查询的记录数
     * @return 已过期库存预占列表
     */
    List<PmsStockReservation> listExpiredLockedReservations(
            Date now,
            int limit
    );

    /**
     * 尝试释放一条已过期库存预占。
     *
     * @param reservation 已过期库存预占快照
     * @param now 当前时间
     * @return true 表示本次成功释放；false 表示已被其他流程处理
     */
    boolean releaseExpiredReservation(
            PmsStockReservation reservation,
            Date now
    );

}
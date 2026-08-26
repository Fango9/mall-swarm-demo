package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.mapper.PmsExpiredStockReservationMapper;
import cn.fango.mall.admin.mapper.PmsSkuStockReservationMapper;
import cn.fango.mall.admin.service.ExpiredStockReservationReleaseService;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.model.PmsStockReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 过期库存预占释放服务实现。
 */
@Service

public class ExpiredStockReservationReleaseServiceImpl
        implements ExpiredStockReservationReleaseService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ExpiredStockReservationReleaseServiceImpl.class
            );

    private final PmsExpiredStockReservationMapper
            pmsExpiredStockReservationMapper;

    private final PmsSkuStockReservationMapper
            pmsSkuStockReservationMapper;

    /**
     * 创建过期库存预占释放服务。
     *
     * @param pmsExpiredStockReservationMapper 已过期预占数据访问对象
     * @param pmsSkuStockReservationMapper SKU 原子库存更新数据访问对象
     */
    public ExpiredStockReservationReleaseServiceImpl(
            PmsExpiredStockReservationMapper
                    pmsExpiredStockReservationMapper,
            PmsSkuStockReservationMapper
                    pmsSkuStockReservationMapper
    ) {
        this.pmsExpiredStockReservationMapper =
                pmsExpiredStockReservationMapper;
        this.pmsSkuStockReservationMapper =
                pmsSkuStockReservationMapper;
    }

    /**
     * 查询当前已过期且仍为 LOCKED 的库存预占。
     *
     * @param now 当前时间
     * @param limit 本次最多查询的记录数
     * @return 已过期库存预占列表
     */
    @Override
    public List<PmsStockReservation> listExpiredLockedReservations(
            Date now,
            int limit
    ) {
        return pmsExpiredStockReservationMapper
                .selectExpiredLockedReservations(now, limit);
    }

    /**
     * 在一个本地事务中释放一条已过期库存预占。
     *
     * <p>先用条件更新获得释放权，再减少 SKU 锁定库存。
     * 若库存更新失败，事务回滚，预占状态不会停留在 RELEASED。</p>
     *
     * @param reservation 已过期库存预占快照
     * @param now 当前时间
     * @return true 表示本次成功释放；false 表示已被其他流程处理
     */
    @Override
    @Transactional
    public boolean releaseExpiredReservation(
            PmsStockReservation reservation,
            Date now
    ) {
        if (reservation == null
                || reservation.getId() == null
                || reservation.getSkuId() == null
                || reservation.getQuantity() == null
                || reservation.getQuantity() <= 0) {
            return false;
        }

        int marked = pmsExpiredStockReservationMapper
                .markReleasedIfExpired(
                        reservation.getId(),
                        now
                );
        if (marked == 0) {
            LOGGER.info(
                    "跳过过期库存预占释放，未获得释放权，reservationId={}",
                    reservation.getId()
            );
            return false;
        }

        int released = pmsSkuStockReservationMapper.releaseStock(
                reservation.getSkuId(),
                reservation.getQuantity()
        );
        if (released != 1) {
            throw new ApiException(
                    StockReservationErrorCode.STOCK_RELEASE_FAILED
            );
        }

        return true;
    }

}
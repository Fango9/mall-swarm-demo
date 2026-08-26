package cn.fango.mall.admin.job;

import cn.fango.mall.admin.service.ExpiredStockReservationReleaseService;
import cn.fango.mall.mbg.model.PmsStockReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 已过期库存预占的定时释放任务。
 */
@Component
public class ExpiredStockReservationReleaseJob {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ExpiredStockReservationReleaseJob.class
            );

    private final ExpiredStockReservationReleaseService
            expiredStockReservationReleaseService;

    private final int batchSize;

    /**
     * 创建已过期库存预占释放任务。
     *
     * @param expiredStockReservationReleaseService 过期库存预占释放服务
     * @param batchSize 单次扫描最多处理的预占数量
     */
    public ExpiredStockReservationReleaseJob(
            ExpiredStockReservationReleaseService
                    expiredStockReservationReleaseService,
            @Value("${mall.stock-reservation.expire-release-batch-size}")
            int batchSize
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "mall.stock-reservation.expire-release-batch-size 必须大于 0"
            );
        }

        this.expiredStockReservationReleaseService =
                expiredStockReservationReleaseService;
        this.batchSize = batchSize;
    }

    /**
     * 扫描并释放已过期且仍为 LOCKED 的库存预占。
     */
    @Scheduled(
            fixedDelayString =
                    "${mall.stock-reservation.expire-release-fixed-delay-ms}"
    )
    public void releaseExpiredReservations() {
        Date now = new Date();

        List<PmsStockReservation> reservations =
                expiredStockReservationReleaseService
                        .listExpiredLockedReservations(
                                now,
                                batchSize
                        );

        if (!reservations.isEmpty()) {
            LOGGER.info(
                    "发现 {} 条已过期 LOCKED 库存预占，开始释放",
                    reservations.size()
            );
        }

        for (PmsStockReservation reservation : reservations) {
            try {
                boolean released =
                        expiredStockReservationReleaseService
                                .releaseExpiredReservation(
                                        reservation,
                                        now
                                );
                if (released) {
                    LOGGER.info(
                            "已释放过期库存预占，reservationNo={}，skuId={}，quantity={}",
                            reservation.getReservationNo(),
                            reservation.getSkuId(),
                            reservation.getQuantity()
                    );
                }
            } catch (RuntimeException exception) {
                LOGGER.error(
                        "释放过期库存预占失败，reservationNo={}，skuId={}",
                        reservation.getReservationNo(),
                        reservation.getSkuId(),
                        exception
                );
            }
        }
    }

}
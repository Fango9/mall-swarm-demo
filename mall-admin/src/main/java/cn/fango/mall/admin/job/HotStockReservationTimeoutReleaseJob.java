package cn.fango.mall.admin.job;

import cn.fango.mall.admin.config.HotStockReservationProperties;
import cn.fango.mall.admin.service.HotStockReservationTimeoutReleaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 热点库存预占用超时释放定时任务。
 *
 * <p>仅在热点预占用链路启用后执行。任务只负责调度，具体的 Redis ZSET 扫描、
 * 事件恢复与 Lua 原子回补由服务层负责。</p>
 */
@Component
public class HotStockReservationTimeoutReleaseJob {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HotStockReservationTimeoutReleaseJob.class
    );

    /**
     * 热点库存预占用运行配置。
     */
    private final HotStockReservationProperties properties;

    /**
     * 热点库存预占用超时释放服务。
     */
    private final HotStockReservationTimeoutReleaseService
            timeoutReleaseService;

    /**
     * 创建热点库存预占用超时释放定时任务。
     *
     * @param properties 热点库存预占用运行配置
     * @param timeoutReleaseService 热点库存预占用超时释放服务
     */
    public HotStockReservationTimeoutReleaseJob(
            HotStockReservationProperties properties,
            HotStockReservationTimeoutReleaseService timeoutReleaseService
    ) {
        this.properties = properties;
        this.timeoutReleaseService = timeoutReleaseService;
    }

    /**
     * 扫描并尝试释放已超时的热点库存预占用。
     */
    @Scheduled(
            fixedDelayString =
                    "${mall.hot-stock-reservation."
                            + "timeout-release-fixed-delay-millis:1000}"
    )
    public void releaseExpiredReservations() {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            int releasedCount = timeoutReleaseService
                    .releaseExpiredReservations(
                            properties.getTimeoutReleaseBatchSize()
                    );

            if (releasedCount > 0) {
                LOGGER.info(
                        "已释放 {} 条超时热点库存预占用",
                        releasedCount
                );
            }
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "扫描超时热点库存预占用失败，下一轮将重试",
                    exception
            );
        }
    }
}

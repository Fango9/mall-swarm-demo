package cn.fango.mall.admin.cache;

import cn.fango.mall.admin.config.HotSkuStockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审计热点 SKU Redis 可预占库存键的健康状态。
 *
 * <p>该组件不以 MySQL {@code stock - lock_stock} 覆盖 Redis。热点 Redis 余额包含
 * 已受理但尚未异步落库的预占用，因此定时覆盖会错误返还库存并造成超卖。Redis 键缺失时
 * 热点 Lua 链路必须失败关闭，之后由人工执行对账和显式修复。</p>
 */
@Component
public class HotSkuStockCacheInitializer {

    /**
     * 热点 SKU 初始化过程日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HotSkuStockCacheInitializer.class);

    /**
     * 热点 SKU 配置。
     */
    private final HotSkuStockProperties hotSkuStockProperties;

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建热点 SKU Redis 库存初始化器。
     *
     * @param hotSkuStockProperties 热点 SKU 配置
     * @param stringRedisTemplate Redis 字符串操作模板
     */
    public HotSkuStockCacheInitializer(
            HotSkuStockProperties hotSkuStockProperties,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.hotSkuStockProperties = hotSkuStockProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 在应用完全启动后审计配置中的热点 SKU 库存键。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeHotSkuStocks() {
        auditConfiguredHotSkuStocks(true);
    }

    /**
     * 定期审计热点 SKU Redis 可预占库存键。
     *
     * <p>该任务只报告缺失、非数字或负数的键值，绝不修改 Redis 库存。MySQL 与 Redis
     * 在异步落库窗口内天然可能不同，因此不能直接把两者的可用库存做相等性判定。</p>
     */
    @Scheduled(
            fixedDelayString =
                    "${mall.hot-sku-stock.audit-fixed-delay-millis:30000}"
    )
    public void refreshHotSkuStocks() {
        auditConfiguredHotSkuStocks(false);
    }

    /**
     * 按当前配置审计全部热点 SKU Redis 库存键。
     *
     * @param logSuccess 是否记录每个 SKU 的成功审计日志
     */
    private void auditConfiguredHotSkuStocks(boolean logSuccess) {
        if (!hotSkuStockProperties.isEnabled()) {
            return;
        }

        for (Long skuId : hotSkuStockProperties.getSkuIds()) {
            auditHotSkuStock(skuId, logSuccess);
        }
    }

    /**
     * 审计一个热点 SKU 的 Redis 可预占库存键。
     *
     * @param skuId 热点 SKU 主键
     * @param logSuccess 是否记录成功审计日志
     */
    private void auditHotSkuStock(Long skuId, boolean logSuccess) {
        if (skuId == null || skuId <= 0) {
            LOGGER.warn("跳过非法热点 SKU 配置，skuId={}", skuId);
            return;
        }

        try {
            String cacheKey = HotSkuStockCacheKeys.availableStockKey(skuId);
            String value = stringRedisTemplate.opsForValue().get(cacheKey);

            if (value == null) {
                LOGGER.error(
                        "热点 SKU Redis 可预占库存键缺失，热点预占用将失败关闭，skuId={}",
                        skuId
                );
                return;
            }

            int availableStock = Integer.parseInt(value);
            if (availableStock < 0) {
                LOGGER.error(
                        "热点 SKU Redis 可预占库存为负数，需要人工对账修复，skuId={}",
                        skuId
                );
                return;
            }

            if (logSuccess) {
                LOGGER.info(
                        "热点 SKU Redis 可预占库存键审计通过，skuId={}，availableStock={}",
                        skuId,
                        availableStock
                );
            } else {
                LOGGER.debug(
                        "热点 SKU Redis 可预占库存键审计通过，skuId={}，availableStock={}",
                        skuId,
                        availableStock
                );
            }
        } catch (NumberFormatException exception) {
            LOGGER.error(
                    "热点 SKU Redis 可预占库存不是整数，需要人工对账修复，skuId={}",
                    skuId,
                    exception
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("热点 SKU Redis 库存审计失败，skuId={}", skuId, exception);
        }
    }
}

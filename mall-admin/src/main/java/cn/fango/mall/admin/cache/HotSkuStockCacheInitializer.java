package cn.fango.mall.admin.cache;

import cn.fango.mall.admin.config.HotSkuStockProperties;
import cn.fango.mall.mbg.mapper.PmsSkuStockMapper;
import cn.fango.mall.mbg.model.PmsSkuStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 在 Admin 启动完成后初始化热点 SKU 的 Redis 可预占库存。
 *
 * <p>Redis 键值由 MySQL {@code stock - lock_stock} 计算得出。
 * 初始化失败只记录日志；后续库存预占会安全回退 MySQL 条件更新。</p>
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
     * SKU 库存数据访问对象。
     */
    private final PmsSkuStockMapper pmsSkuStockMapper;

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建热点 SKU Redis 库存初始化器。
     *
     * @param hotSkuStockProperties 热点 SKU 配置
     * @param pmsSkuStockMapper SKU 库存数据访问对象
     * @param stringRedisTemplate Redis 字符串操作模板
     */
    public HotSkuStockCacheInitializer(HotSkuStockProperties hotSkuStockProperties, PmsSkuStockMapper pmsSkuStockMapper, StringRedisTemplate stringRedisTemplate) {
        this.hotSkuStockProperties = hotSkuStockProperties;
        this.pmsSkuStockMapper = pmsSkuStockMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 在应用完全启动后初始化配置中的热点 SKU 库存键。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeHotSkuStocks() {
        refreshConfiguredHotSkuStocks(true);
    }

    /**
     * 定期从 MySQL 重建热点 SKU 的 Redis 可预占库存。
     *
     * <p>用于修正管理员直接改库存、Redis 临时故障导致补偿失败等场景。
     * 固定延迟保证一次同步完成后才开始下一次同步。</p>
     */
    @Scheduled(fixedDelayString = "${mall.hot-sku-stock.refresh-fixed-delay-ms:30000}")
    public void refreshHotSkuStocks() {
        refreshConfiguredHotSkuStocks(false);
    }

    /**
     * 按当前配置刷新全部热点 SKU Redis 库存键。
     *
     * @param logSuccess 是否记录每个 SKU 的成功初始化日志
     */
    private void refreshConfiguredHotSkuStocks(boolean logSuccess) {
        if (!hotSkuStockProperties.isEnabled()) {
            return;
        }

        for (Long skuId : hotSkuStockProperties.getSkuIds()) {
            refreshHotSkuStock(skuId, logSuccess);
        }
    }

    /**
     * 从 MySQL 读取一个热点 SKU 的可预占库存并写入 Redis。
     *
     * @param skuId 热点 SKU 主键
     * @param logSuccess 是否记录成功同步日志
     */
    private void refreshHotSkuStock(Long skuId, boolean logSuccess){
        if (skuId == null || skuId <= 0) {
            LOGGER.warn("跳过非法热点 SKU 配置，skuId={}", skuId);
            return;
        }

        try {
            PmsSkuStock skuStock = pmsSkuStockMapper.selectByPrimaryKey(skuId);
            String cacheKey = HotSkuStockCacheKeys.availableStockKey(skuId);

            if (skuStock == null) {
                stringRedisTemplate.delete(cacheKey);
                LOGGER.warn("热点 SKU 不存在，已删除可能残留的 Redis 键，skuId={}", skuId);
                return;
            }

            int availableStock = calculateAvailableStock(skuStock);
            stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(availableStock));
            if (logSuccess) {
                LOGGER.info("热点 SKU Redis 可预占库存初始化完成，skuId={}，availableStock={}", skuId, availableStock);
            } else {
                LOGGER.debug("热点 SKU Redis 可预占库存定时校准完成，skuId={}，availableStock={}", skuId, availableStock);
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("热点 SKU Redis 库存初始化失败，将回退 MySQL 条件更新，skuId={}", skuId, exception);
        }
    }

    /**
     * 根据 MySQL SKU 库存记录计算当前可预占库存。
     *
     * @param skuStock MySQL SKU 库存记录
     * @return 不小于零的可预占库存
     */
    private int calculateAvailableStock(PmsSkuStock skuStock) {
        int stock = skuStock.getStock() == null ? 0 : skuStock.getStock();
        int lockStock = skuStock.getLockStock() == null ? 0 : skuStock.getLockStock();

        return Math.max(0, stock - lockStock);
    }
}
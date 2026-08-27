package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.HotSkuStockFilterResult;
import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import cn.fango.mall.admin.config.HotSkuStockProperties;
import cn.fango.mall.admin.service.HotSkuStockFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 热点 SKU Redis 快速库存过滤服务实现。
 *
 * <p>Redis Lua 脚本只在热点键已存在时原子预扣库存；键不存在或 Redis 异常时，
 * 安全返回 {@link HotSkuStockFilterResult#NOT_APPLIED}，由调用方继续执行 MySQL
 * 条件更新。Redis 不可用不会阻断正常下单。</p>
 */
@Service
public class HotSkuStockFilterServiceImpl implements HotSkuStockFilterService {

    /**
     * 热点库存过滤过程日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HotSkuStockFilterServiceImpl.class);

    /**
     * Redis 键不存在时 Lua 脚本返回的标记值。
     */
    private static final long CACHE_KEY_MISSING = -2L;

    /**
     * Redis 可预占库存不足时 Lua 脚本返回的标记值。
     */
    private static final long STOCK_NOT_ENOUGH = -1L;

    /**
     * 原子检查并预扣热点 SKU 可预占库存的 Lua 脚本。
     */
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>();

    /**
     * 仅在热点库存键仍存在时补回预扣数量的 Lua 脚本。
     */
    private static final DefaultRedisScript<Long> RESTORE_SCRIPT = new DefaultRedisScript<>();

    static {
        RESERVE_SCRIPT.setScriptText("""
                local availableStock = redis.call('GET', KEYS[1])
                if not availableStock then
                    return -2
                end
                if tonumber(availableStock) < tonumber(ARGV[1]) then
                    return -1
                end
                return redis.call('DECRBY', KEYS[1], ARGV[1])
                """);
        RESERVE_SCRIPT.setResultType(Long.class);

        RESTORE_SCRIPT.setScriptText("""
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return 0
                end
                return redis.call('INCRBY', KEYS[1], ARGV[1])
                """);
        RESTORE_SCRIPT.setResultType(Long.class);
    }

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 热点 SKU 配置。
     */
    private final HotSkuStockProperties hotSkuStockProperties;

    /**
     * 创建热点 SKU Redis 快速库存过滤服务。
     *
     * @param stringRedisTemplate Redis 字符串操作模板
     * @param hotSkuStockProperties 热点 SKU 配置
     */
    public HotSkuStockFilterServiceImpl(StringRedisTemplate stringRedisTemplate, HotSkuStockProperties hotSkuStockProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.hotSkuStockProperties = hotSkuStockProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HotSkuStockFilterResult tryReserve(Long skuId, Integer quantity) {
        if (!isHotSku(skuId) || quantity == null || quantity <= 0) {
            return HotSkuStockFilterResult.NOT_APPLIED;
        }

        String cacheKey = HotSkuStockCacheKeys.availableStockKey(skuId);

        try {
            Long remainingStock = stringRedisTemplate.execute(RESERVE_SCRIPT, Collections.singletonList(cacheKey), quantity.toString());

            if (remainingStock == null || remainingStock == CACHE_KEY_MISSING) {
                return HotSkuStockFilterResult.NOT_APPLIED;
            }
            if (remainingStock == STOCK_NOT_ENOUGH) {
                return HotSkuStockFilterResult.STOCK_NOT_ENOUGH;
            }

            return HotSkuStockFilterResult.RESERVED;
        } catch (RuntimeException exception) {
            LOGGER.warn("热点 SKU Redis 快速过滤不可用，将回退 MySQL 条件更新，skuId={}", skuId, exception);
            return HotSkuStockFilterResult.NOT_APPLIED;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreReservedStock(Long skuId, Integer quantity) {
        if (!isHotSku(skuId) || quantity == null || quantity <= 0) {
            return;
        }

        String cacheKey = HotSkuStockCacheKeys.availableStockKey(skuId);

        try {
            stringRedisTemplate.execute(RESTORE_SCRIPT, Collections.singletonList(cacheKey), quantity.toString());
        } catch (RuntimeException exception) {
            LOGGER.warn("热点 SKU Redis 预扣补回失败，后续将由库存同步修正，skuId={}", skuId, exception);
        }
    }

    /**
     * 判断指定 SKU 当前是否启用热点 Redis 快速过滤。
     *
     * @param skuId SKU 主键
     * @return 当前 SKU 为已启用热点 SKU 时返回 {@code true}
     */
    private boolean isHotSku(Long skuId) {
        return hotSkuStockProperties.isEnabled()
                && skuId != null
                && skuId > 0
                && hotSkuStockProperties.getSkuIds().contains(skuId);
    }
}
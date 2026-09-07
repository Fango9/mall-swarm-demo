package cn.fango.mall.admin.redis;

import cn.fango.mall.admin.api.HotStockReservationStatus;
import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import cn.fango.mall.admin.support.HotStockReservationFingerprint;
import cn.fango.mall.common.event.HotStockReservationEvent;
import cn.fango.mall.common.stock.StockReservationItem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 执行热点库存预占用释放 Lua 脚本的 Redis 访问组件。
 *
 * <p>本类只按 Lua 契约组装 {@code KEYS} 和 {@code ARGV}。库存回补、状态转换
 * 与超时 ZSET 移除始终由同一次 Lua 原子执行完成。</p>
 */
@Component
public class HotStockReservationReleaseLuaExecutor {

    /**
     * Redis 原子释放预占用脚本。
     */
    private static final DefaultRedisScript<Long> RELEASE_RESERVATION_SCRIPT =
            new DefaultRedisScript<>();

    static {
        RELEASE_RESERVATION_SCRIPT.setLocation(
                new ClassPathResource(
                        "lua/hot-stock-reservation-release.lua"
                )
        );
        RELEASE_RESERVATION_SCRIPT.setResultType(Long.class);
    }

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建预占用释放 Lua 执行器。
     *
     * @param stringRedisTemplate Redis 字符串操作模板
     */
    public HotStockReservationReleaseLuaExecutor(
            StringRedisTemplate stringRedisTemplate
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 原子释放尚未完成 MySQL 落库的热点库存预占用。
     *
     * <p>只有 {@link HotStockReservationStatus#FAILED} 和
     * {@link HotStockReservationStatus#RELEASED} 可作为释放终态。Lua 会再次校验
     * 当前 Redis 状态和明细指纹，重复调用不会重复回补库存。</p>
     *
     * @param event 原始热点库存预占用事件
     * @param targetStatus 释放后写入 Redis 的终态
     * @return Lua 原始返回码
     */
    public Long release(HotStockReservationEvent event, HotStockReservationStatus targetStatus) {
        validateEvent(event);
        validateTargetStatus(targetStatus);

        List<HotStockReservationEvent.Item> sortedItems =
                new ArrayList<>(event.items());
        sortedItems.sort(
                Comparator.comparing(HotStockReservationEvent.Item::skuId)
        );

        List<String> keys = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<StockReservationItem> fingerprintItems = new ArrayList<>();
        Set<Long> skuIds = new HashSet<>();

        for (HotStockReservationEvent.Item item : sortedItems) {
            validateItem(item);

            if (!skuIds.add(item.skuId())) {
                throw new IllegalArgumentException("热点库存预占用事件包含重复 SKU");
            }

            keys.add(HotSkuStockCacheKeys.availableStockKey(item.skuId()));
            quantities.add(item.quantity());
            fingerprintItems.add(
                    new StockReservationItem(item.skuId(), item.quantity())
            );
        }

        keys.add(
                HotSkuStockCacheKeys.reservationKey(event.reservationNo())
        );
        keys.add(HotSkuStockCacheKeys.reservationTimeoutZsetKey());
        keys.add(HotSkuStockCacheKeys.timeoutFailureStreamKey());

        List<Object> arguments = new ArrayList<>();
        arguments.add(String.valueOf(sortedItems.size()));
        arguments.add(event.reservationNo());
        arguments.add(targetStatus.name());
        arguments.add(
                HotStockReservationFingerprint.create(fingerprintItems)
        );

        for (Integer quantity : quantities) {
            arguments.add(String.valueOf(quantity));
        }

        return stringRedisTemplate.execute(
                RELEASE_RESERVATION_SCRIPT,
                keys,
                arguments.toArray()
        );
    }

    /**
     * 校验事件的公共字段。
     *
     * @param event 待释放的热点库存预占用事件
     */
    private void validateEvent(HotStockReservationEvent event) {
        if (event == null
                || event.reservationNo() == null
                || event.reservationNo().isBlank()
                || event.items() == null
                || event.items().isEmpty()) {
            throw new IllegalArgumentException("热点库存预占用事件非法");
        }
    }

    /**
     * 校验单个 SKU 明细。
     *
     * @param item 待校验的 SKU 明细
     */
    private void validateItem(HotStockReservationEvent.Item item) {
        if (item == null
                || item.skuId() == null
                || item.skuId() <= 0
                || item.quantity() == null
                || item.quantity() <= 0) {
            throw new IllegalArgumentException("热点库存预占用 SKU 明细非法");
        }
    }

    /**
     * 校验可写入 Redis 的释放终态。
     *
     * @param targetStatus 目标终态
     */
    private void validateTargetStatus(
            HotStockReservationStatus targetStatus
    ) {
        if (targetStatus != HotStockReservationStatus.FAILED
                && targetStatus != HotStockReservationStatus.RELEASED) {
            throw new IllegalArgumentException(
                    "热点库存预占用释放终态必须为 FAILED 或 RELEASED"
            );
        }
    }
}

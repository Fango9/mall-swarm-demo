package cn.fango.mall.admin.redis;

import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行热点库存预占用 Lua 脚本的 Redis 访问组件。
 *
 * <p>本类只负责严格按 Lua 契约组装 KEYS 和 ARGV，并返回脚本原始返回码。
 * 业务校验、热点 SKU 判断和返回码映射由上层服务负责。</p>
 */
@Component
public class HotStockReservationLuaExecutor {

    /**
     * Redis 原子库存预占用脚本。
     */
    private static final DefaultRedisScript<Long> ACCEPT_RESERVATION_SCRIPT = new DefaultRedisScript<>();

    static {
        ACCEPT_RESERVATION_SCRIPT.setLocation(
                new ClassPathResource(
                        "lua/hot-stock-reservation-accept.lua"
                )
        );
        ACCEPT_RESERVATION_SCRIPT.setResultType(Long.class);
    }

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建 Lua 执行器。
     *
     * @param stringRedisTemplate Redis 字符串操作模板
     */
    public HotStockReservationLuaExecutor(
            StringRedisTemplate stringRedisTemplate
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 执行热点库存预占用 Lua 脚本。
     *
     * @param availableStockKeys 已按预占用明细顺序排列的库存键
     * @param quantities 与库存键一一对应的预占用数量
     * @param reservationNo 全链路唯一预占用编号
     * @param orderSn 订单编号
     * @param itemsFingerprint 已规范化的 SKU 明细指纹
     * @param expireAtMillis 预占用超时时间戳，单位毫秒
     * @param stateRetentionMillis 预占用状态终态保留时长，单位毫秒
     * @param eventJson 待写入 Redis Stream 的预占用事件 JSON
     * @return Lua 原始返回码
     */
    public Long accept(
            List<String> availableStockKeys,
            List<Integer> quantities,
            String reservationNo,
            String orderSn,
            String itemsFingerprint,
            long expireAtMillis,
            long stateRetentionMillis,
            String eventJson
    ) {
        validateArguments(
                availableStockKeys,
                quantities,
                reservationNo,
                orderSn,
                itemsFingerprint,
                expireAtMillis,
                stateRetentionMillis,
                eventJson
        );

        List<String> keys = new ArrayList<>(availableStockKeys);
        keys.add(HotSkuStockCacheKeys.reservationKey(reservationNo));
        keys.add(HotSkuStockCacheKeys.reservationStreamKey());
        keys.add(HotSkuStockCacheKeys.reservationTimeoutZsetKey());
        keys.add(HotSkuStockCacheKeys.reconciliationRepairLockKey());

        List<Object> arguments = new ArrayList<>();
        arguments.add(String.valueOf(availableStockKeys.size()));
        arguments.add(reservationNo);
        arguments.add(orderSn);
        arguments.add(itemsFingerprint);
        arguments.add(String.valueOf(expireAtMillis));
        arguments.add(String.valueOf(stateRetentionMillis));
        arguments.add(eventJson);

        for (Integer quantity : quantities) {
            arguments.add(String.valueOf(quantity));
        }

        return stringRedisTemplate.execute(
                ACCEPT_RESERVATION_SCRIPT,
                keys,
                arguments.toArray()
        );
    }

    /**
     * 校验传入的 Lua 参数在索引和业务含义上完全对应。
     *
     * @param availableStockKeys SKU 可预占库存键
     * @param quantities SKU 预占用数量
     * @param reservationNo 预占用编号
     * @param orderSn 订单编号
     * @param itemsFingerprint 预占用明细指纹
     * @param expireAtMillis 超时时间戳
     * @param stateRetentionMillis 状态保留时长
     * @param eventJson Stream 事件 JSON
     */
    private void validateArguments(
            List<String> availableStockKeys,
            List<Integer> quantities,
            String reservationNo,
            String orderSn,
            String itemsFingerprint,
            long expireAtMillis,
            long stateRetentionMillis,
            String eventJson
    ) {
        if (availableStockKeys == null
                || availableStockKeys.isEmpty()
                || quantities == null
                || availableStockKeys.size() != quantities.size()) {
            throw new IllegalArgumentException(
                    "库存键和预约数量必须一一对应且不能为空"
            );
        }

        if (reservationNo == null
                || reservationNo.isBlank()
                || orderSn == null
                || orderSn.isBlank()
                || itemsFingerprint == null
                || itemsFingerprint.isBlank()
                || eventJson == null
                || eventJson.isBlank()
                || expireAtMillis <= 0
                || stateRetentionMillis <= 0) {
            throw new IllegalArgumentException("Lua 预约参数非法");
        }

        for (Integer quantity : quantities) {
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("预约数量必须大于 0");
            }
        }
    }
}

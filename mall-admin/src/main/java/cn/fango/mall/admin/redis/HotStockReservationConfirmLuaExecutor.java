package cn.fango.mall.admin.redis;

import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 执行热点库存预占用确认 Lua 脚本的 Redis 访问组件。
 *
 * <p>脚本原子地将 {@code PERSISTED} 转为 {@code CONFIRMED}，并移除超时
 * ZSET 索引。本类只负责组装 Lua 参数并返回原始结果码。</p>
 */
@Component
public class HotStockReservationConfirmLuaExecutor {

    /**
     * Redis 原子确认预占用脚本。
     */
    private static final DefaultRedisScript<Long> CONFIRM_RESERVATION_SCRIPT =
            new DefaultRedisScript<>();

    static {
        CONFIRM_RESERVATION_SCRIPT.setLocation(
                new ClassPathResource(
                        "lua/hot-stock-reservation-confirm.lua"
                )
        );
        CONFIRM_RESERVATION_SCRIPT.setResultType(Long.class);
    }

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建预占用确认 Lua 执行器。
     *
     * @param stringRedisTemplate Redis 字符串操作模板
     */
    public HotStockReservationConfirmLuaExecutor(
            StringRedisTemplate stringRedisTemplate
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 原子确认一笔已完成 MySQL 落库的热点库存预占用。
     *
     * @param reservationNo 全链路唯一库存预占用编号
     * @return Lua 原始返回码
     */
    public Long confirm(String reservationNo) {
        if (reservationNo == null || reservationNo.isBlank()) {
            throw new IllegalArgumentException("reservationNo 不能为空");
        }

        List<String> keys = List.of(
                HotSkuStockCacheKeys.reservationKey(reservationNo),
                HotSkuStockCacheKeys.reservationTimeoutZsetKey()
        );

        return stringRedisTemplate.execute(
                CONFIRM_RESERVATION_SCRIPT,
                keys,
                reservationNo
        );
    }
}
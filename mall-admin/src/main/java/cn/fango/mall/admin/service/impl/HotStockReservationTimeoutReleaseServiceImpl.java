package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.HotStockReservationReleaseLuaResult;
import cn.fango.mall.admin.api.HotStockPersistedReleaseResult;
import cn.fango.mall.admin.api.HotStockReservationStatus;
import cn.fango.mall.admin.cache.HotSkuStockCacheKeys;
import cn.fango.mall.admin.redis.HotStockReservationReleaseLuaExecutor;
import cn.fango.mall.admin.service.HotStockReservationTimeoutReleaseService;
import cn.fango.mall.admin.service.HotStockPersistedReservationReleaseService;
import cn.fango.mall.common.event.HotStockReservationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 热点库存预占用超时释放服务实现。
 *
 * <p>本类只负责扫描超时 ZSET 和恢复事件；库存回补、状态变更及 ZSET 移除
 * 始终由 Redis Lua 在一次原子操作中完成。</p>
 */
@Service
public class HotStockReservationTimeoutReleaseServiceImpl
        implements HotStockReservationTimeoutReleaseService {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HotStockReservationTimeoutReleaseServiceImpl.class
    );

    /**
     * JSON 序列化与反序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * Redis 字符串操作模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Redis 原子释放预占用 Lua 执行器。
     */
    private final HotStockReservationReleaseLuaExecutor releaseLuaExecutor;

    /** 已持久化热点预占用的 MySQL 超时释放服务。 */
    private final HotStockPersistedReservationReleaseService
            persistedReleaseService;

    /**
     * 创建热点库存预占用超时释放服务。
     *
     * @param objectMapper JSON 序列化与反序列化工具
     * @param stringRedisTemplate Redis 字符串操作模板
     * @param releaseLuaExecutor Redis 原子释放预占用 Lua 执行器
     * @param persistedReleaseService 已持久化热点预占用 MySQL 超时释放服务
     */
    public HotStockReservationTimeoutReleaseServiceImpl(
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate,
            HotStockReservationReleaseLuaExecutor releaseLuaExecutor,
            HotStockPersistedReservationReleaseService persistedReleaseService
    ) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.releaseLuaExecutor = releaseLuaExecutor;
        this.persistedReleaseService = persistedReleaseService;
    }

    /**
     * 扫描并尝试释放一批已超时的热点库存预占用。
     *
     * <p>无法读取事件、事件非法、库存 Key 丢失或 Lua 暂时拒绝释放时，
     * 不删除 ZSET 项，以保证后续可重试和可对账。</p>
     *
     * @param batchSize 本次最多扫描的预占用数量
     * @return Lua 确认已释放或已完成幂等释放的数量
     */
    @Override
    public int releaseExpiredReservations(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于 0");
        }

        Set<String> reservationNos = stringRedisTemplate.opsForZSet()
                .rangeByScore(
                        HotSkuStockCacheKeys.reservationTimeoutZsetKey(),
                        0,
                        System.currentTimeMillis(),
                        0,
                        batchSize
                );

        if (reservationNos == null || reservationNos.isEmpty()) {
            return 0;
        }

        int releasedCount = 0;

        for (String reservationNo : reservationNos) {
            if (tryRelease(reservationNo)) {
                releasedCount++;
            }
        }

        return releasedCount;
    }

    /**
     * 尝试释放一个已到期的热点库存预占用。
     *
     * @param reservationNo ZSET 中的预占用编号
     * @return Lua 已确认释放或已完成幂等释放时返回 {@code true}
     */
    private boolean tryRelease(String reservationNo) {
        Object eventJsonValue = stringRedisTemplate.opsForHash().get(
                HotSkuStockCacheKeys.reservationKey(reservationNo),
                "eventJson"
        );

        if (!(eventJsonValue instanceof String eventJson)
                || eventJson.isBlank()) {
            LOGGER.error(
                    "热点库存预占用超时释放缺少事件，保留 ZSET 记录等待对账，"
                            + "reservationNo={}",
                    reservationNo
            );
            return false;
        }

        try {
            HotStockReservationEvent event = objectMapper.readValue(
                    eventJson,
                    HotStockReservationEvent.class
            );
            if (!releasePersistedMySqlStockIfNecessary(event)) {
                return false;
            }
            Long luaResult = releaseLuaExecutor.release(
                    event,
                    HotStockReservationStatus.RELEASED
            );

            if (HotStockReservationReleaseLuaResult.RELEASED
                    == luaResult
                    || HotStockReservationReleaseLuaResult.ALREADY_RELEASED
                    == luaResult) {
                return true;
            }

            LOGGER.warn(
                    "热点库存预占用超时释放暂未完成，保留 ZSET 记录，"
                            + "reservationNo={}, luaResult={}",
                    reservationNo,
                    luaResult
            );
            return false;
        } catch (JsonProcessingException exception) {
            LOGGER.error(
                    "热点库存预占用超时释放事件无法解析，保留 ZSET 记录等待对账，"
                            + "reservationNo={}",
                    reservationNo,
                    exception
            );
            return false;
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "热点库存预占用超时释放异常，保留 ZSET 记录等待重试，"
                            + "reservationNo={}",
                    reservationNo,
                    exception
            );
            return false;
        }
    }

    /**
     * 对已落库热点预占用先释放 MySQL 锁库存并可靠写入订单失败通知。
     *
     * <p>Redis 状态为 {@code ACCEPTED}/{@code RELAYED} 时，MySQL 尚无锁库存，
     * 可直接由 Redis Lua 回补。状态为 {@code PERSISTED} 时必须先完成 MySQL
     * 本地事务；否则 Redis 先回补会让新的预占用看到虚假的可用库存。</p>
     *
     * @param event Redis 保存的原始热点预占用事件
     * @return 可继续执行 Redis 回补时返回 {@code true}
     */
    private boolean releasePersistedMySqlStockIfNecessary(
            HotStockReservationEvent event
    ) {
        Object statusValue = stringRedisTemplate.opsForHash().get(
                HotSkuStockCacheKeys.reservationKey(event.reservationNo()),
                "status"
        );
        if (!HotStockReservationStatus.PERSISTED.name().equals(
                statusValue == null ? null : statusValue.toString()
        )) {
            return true;
        }

        HotStockPersistedReleaseResult result = persistedReleaseService
                .release(event);
        if (result == HotStockPersistedReleaseResult.RELEASED
                || result == HotStockPersistedReleaseResult.ALREADY_RELEASED) {
            return true;
        }

        LOGGER.warn(
                "热点库存预占用 MySQL 超时释放暂未完成，保留 ZSET 记录，"
                        + "reservationNo={}, result={}",
                event.reservationNo(),
                result
        );
        return false;
    }
}

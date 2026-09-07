package cn.fango.mall.admin.service;

/**
 * 热点库存预占用超时释放服务。
 *
 * <p>服务从 Redis 超时 ZSET 扫描到期预占用，并通过 Lua 原子脚本回补可预占库存。
 * 无法安全释放的记录必须保留在 ZSET 中，以便后续重试或对账修复。</p>
 */
public interface HotStockReservationTimeoutReleaseService {

    /**
     * 扫描并尝试释放一批已超时的热点库存预占用。
     *
     * @param batchSize 本次最多扫描的预占用数量
     * @return 本次 Lua 确认已释放或已完成幂等释放的数量
     */
    int releaseExpiredReservations(int batchSize);
}
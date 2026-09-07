package cn.fango.mall.admin.api;

/**
 * Redis 热点库存预占用确认 Lua 脚本返回码。
 *
 * <p>确认操作将已完成 MySQL 落库的预占用转为 {@code CONFIRMED}，
 * 并原子移除超时 ZSET 索引。</p>
 */
public final class HotStockReservationConfirmLuaResult {

    /**
     * 首次确认成功，Redis 状态已转为 CONFIRMED。
     */
    public static final long CONFIRMED = 1L;

    /**
     * 该预占用此前已确认，属于幂等成功。
     */
    public static final long ALREADY_CONFIRMED = 2L;

    /**
     * 当前状态不允许确认，例如 MySQL 尚未完成持久化。
     */
    public static final long NOT_CONFIRMABLE = -1L;

    /**
     * Redis 中不存在对应的预占用状态记录。
     */
    public static final long STATE_NOT_FOUND = -2L;

    private HotStockReservationConfirmLuaResult() {
    }
}
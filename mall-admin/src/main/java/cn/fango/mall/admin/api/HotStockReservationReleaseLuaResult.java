package cn.fango.mall.admin.api;

/**
 * Redis 热点库存预占用释放 Lua 脚本返回码。
 *
 * <p>释放脚本仅允许将尚未完成 MySQL 落库的预占用原子回补到 Redis。
 * 相同预占用编号重复执行时不得重复增加可预占库存。</p>
 */
public final class HotStockReservationReleaseLuaResult {

    /**
     * 首次释放成功：Redis 可预占库存已回补，预占用状态已更新。
     */
    public static final long RELEASED = 1L;

    /**
     * 预占用此前已进入 {@code FAILED} 或 {@code RELEASED} 终态。
     *
     * <p>调用方应将其视为幂等成功，不能再次回补库存。</p>
     */
    public static final long ALREADY_RELEASED = 2L;

    /**
     * 当前预占用不允许释放，例如 MySQL 已成功持久化。
     */
    public static final long NOT_RELEASEABLE = -1L;

    /**
     * Redis 中不存在对应的预占用状态记录。
     */
    public static final long STATE_NOT_FOUND = -2L;

    /**
     * 某个 SKU 的可预占库存键不存在，脚本未执行任何库存回补。
     *
     * <p>调用方必须保留待处理记录并进入对账修复，不能把它当作释放成功。</p>
     */
    public static final long AVAILABLE_STOCK_KEY_MISSING = -3L;

    private HotStockReservationReleaseLuaResult() {
    }
}
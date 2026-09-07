package cn.fango.mall.admin.api;

/**
 * Redis 热点库存预占用 Lua 脚本返回码。
 *
 * <p>Lua 只返回整数，Java 服务根据这些固定值转换为领域结果或业务异常。
 * 返回码是 Redis 与 Java 之间的内部契约，不能作为外部接口响应直接暴露。</p>
 */
public final class HotStockReservationLuaResult {

    /**
     * 首次预占用成功，Redis 已完成全部原子写入。
     */
    public static final long ACCEPTED = 1L;

    /**
     * 相同预占用编号和相同明细已存在，属于幂等重试。
     */
    public static final long ALREADY_ACCEPTED = 2L;

    /**
     * 至少一个 SKU 的 Redis 可预占库存不足。
     */
    public static final long STOCK_NOT_ENOUGH = -1L;

    /**
     * 相同预占用编号携带了不同 SKU 明细，不能继续处理。
     */
    public static final long RESERVATION_CONFLICT = -2L;

    /**
     * 热点 SKU 的 Redis 可预占库存键不存在。
     *
     * <p>这不是库存不足，表示初始化、Redis 数据恢复或配置存在异常；
     * 调用方必须失败关闭。</p>
     */
    public static final long AVAILABLE_STOCK_KEY_MISSING = -3L;

    /**
     * 相同预占用编号已处于 RELEASED 或 FAILED 终态，不能重新受理。
     */
    public static final long RESERVATION_NOT_ACTIVE = -4L;

    /**
     * 管理员正在执行受控对账修复，新的首次预占用被维护锁拒绝。
     */
    public static final long RECONCILIATION_REPAIR_IN_PROGRESS = -5L;

    /**
     * 工具类不允许创建实例。
     */
    private HotStockReservationLuaResult() {
    }
}

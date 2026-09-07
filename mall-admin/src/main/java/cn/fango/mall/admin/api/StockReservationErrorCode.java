package cn.fango.mall.admin.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * SKU 库存预占领域错误码。
 */
public enum StockReservationErrorCode implements IErrorCode {

    /** 预占编号为空。 */
    RESERVATION_NO_REQUIRED(43101, "库存预占编号不能为空"),

    /** 预占项为空。 */
    RESERVATION_ITEMS_REQUIRED(43102, "库存预占项不能为空"),

    /** SKU ID 或预占数量非法。 */
    RESERVATION_ITEM_INVALID(43103, "SKU ID 和预占数量必须大于 0"),

    /** 相同预占编号的请求明细不一致。 */
    RESERVATION_REQUEST_CONFLICT(40941, "相同预占编号的库存预占明细不一致"),

    /** 当前预占已释放，不能再次预占。 */
    RESERVATION_ALREADY_RELEASED(40943, "库存预占已释放，不能再次预占"),

    /** 库存预占当前状态不允许重复预占。 */
    RESERVATION_STATUS_CONFLICT(40945, "库存预占当前状态不允许重复预占"),

    /** SKU 不存在或可用库存不足。 */
    STOCK_NOT_ENOUGH(40942, "SKU 不存在或可用库存不足"),

    /** 当前预占记录不存在。 */
    RESERVATION_NOT_FOUND(40441, "库存预占记录不存在"),

    /** 库存释放失败。 */
    STOCK_RELEASE_FAILED(53041, "库存释放失败"),

    /** 创建库存预占记录失败。 */
    RESERVATION_CREATE_FAILED(53042, "创建库存预占记录失败"),

    /** 订单创建事件内容非法。 */
    ORDER_CREATED_EVENT_INVALID(43104, "订单创建事件内容非法"),

    /** 订单创建事件无法确认库存预占。 */
    STOCK_RESERVATION_CONFIRM_FAILED(53043, "确认库存预占失败"),

    /** 热点 SKU 的 Redis 可预占库存不足。 */
    HOT_STOCK_NOT_ENOUGH(40944, "热点 SKU 可预占库存不足"),

    /** 热点 SKU 的 Redis 可预占库存不足。 */
    HOT_STOCK_RESERVATION_UNAVAILABLE(53044, "热点库存预约暂不可用，请稍后重试"),

    /** Redis Stream Relay 投递的热点库存预占用事件内容非法。 */
    HOT_STOCK_RESERVATION_EVENT_INVALID(43105, "热点库存预约事件内容非法"),

    /** 热点库存预占用无法在 MySQL 中完成锁定库存和预占用记录写入。 */
    HOT_STOCK_RESERVATION_PERSIST_FAILED(53045, "热点库存预约落库失败"),

    /** 热点订单确认事件先到，等待对应库存预占用完成 MySQL 落库。 */
    HOT_STOCK_ORDER_CONFIRM_PENDING(53046, "热点库存预占用尚未完成落库，请稍后重试"),

    /** 热点库存确认事务无法写入通知 Portal 的 Outbox 事件。 */
    HOT_STOCK_ORDER_CONFIRM_OUTBOX_FAILED(53047, "热点库存确认通知创建失败"),

    /** 热点库存最终失败后无法写入通知 Portal 的 Outbox 事件。 */
    HOT_STOCK_ORDER_FAILURE_OUTBOX_FAILED(53048, "热点库存失败通知创建失败");

    private final long code;
    private final String message;

    /**
     * 创建库存预占领域错误码。
     *
     * @param code 业务错误码
     * @param message 错误信息
     */
    StockReservationErrorCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCode() {
        return code;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return message;
    }
}

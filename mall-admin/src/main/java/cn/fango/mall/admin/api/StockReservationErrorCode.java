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

    /** SKU 不存在或可用库存不足。 */
    STOCK_NOT_ENOUGH(40942, "SKU 不存在或可用库存不足"),

    /** 当前预占记录不存在。 */
    RESERVATION_NOT_FOUND(40441, "库存预占记录不存在"),

    /** 库存释放失败。 */
    STOCK_RELEASE_FAILED(53041, "库存释放失败"),

    /** 创建库存预占记录失败。 */
    RESERVATION_CREATE_FAILED(53042, "创建库存预占记录失败");

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
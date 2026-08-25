package cn.fango.mall.portal.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 订单领域错误码。
 */
public enum OrderErrorCode implements IErrorCode {

    /** 下单请求中的购物车项列表为空、包含空值、非正数或重复主键。 */
    ORDER_CREATE_REQUEST_INVALID(44011, "请选择至少一个有效且不重复的购物车项"),

    /** Idempotency-Key 请求头缺失、为空或过长。 */
    IDEMPOTENCY_KEY_INVALID(44012, "Idempotency-Key 必须为 1 到 64 个字符"),

    /** 当前会员下不存在指定的购物车项。 */
    ORDER_CART_ITEM_NOT_FOUND(40461, "购物车项不存在或不属于当前会员"),

    /** 当前会员下不存在指定的订单。 */
    ORDER_NOT_FOUND(40462, "订单不存在"),

    /** 调用后台库存预占服务失败。 */
    STOCK_RESERVATION_FAILED(54011, "库存预占失败"),

    /** 保存订单主记录失败。 */
    ORDER_CREATE_FAILED(54012, "创建订单失败"),

    /** 保存订单明细失败。 */
    ORDER_ITEM_CREATE_FAILED(54013, "创建订单明细失败"),

    /** 下单成功后删除对应购物车项失败。 */
    CART_ITEM_CLEAR_FAILED(54014, "清理购物车项失败"),

    /** 调用后台库存释放补偿服务失败。 */
    STOCK_RELEASE_COMPENSATION_FAILED(54015, "库存释放补偿失败");

    /**
     * 业务错误码。
     */
    private final long code;

    /**
     * 面向调用方的错误信息。
     */
    private final String message;

    /**
     * 创建订单领域错误码。
     *
     * @param code 业务错误码
     * @param message 面向调用方的错误信息
     */
    OrderErrorCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码
     */
    @Override
    public long getCode() {
        return code;
    }

    /**
     * 获取面向调用方的错误信息。
     *
     * @return 面向调用方的错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }
}
package cn.fango.mall.portal.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 购物车领域错误码。
 */
public enum CartErrorCode implements IErrorCode {

    /** 商品 ID、SKU ID 或购买数量非法。 */
    CART_ITEM_INVALID(44001, "商品 ID、SKU ID 和购买数量必须大于 0"),

    /** 后台商品查询失败。 */
    PRODUCT_QUERY_FAILED(54001, "查询商品信息失败"),

    /** 指定 SKU 不属于当前商品或不存在。 */
    SKU_NOT_FOUND(40451, "指定 SKU 不存在"),

    /** 当前会员下未找到指定购物车项。 */
    CART_ITEM_NOT_FOUND(40452, "购物车项不存在"),

    /** 创建购物车项失败。 */
    CART_ITEM_CREATE_FAILED(54002, "创建购物车项失败"),

    /** 修改购物车项失败。 */
    CART_ITEM_UPDATE_FAILED(54003, "修改购物车项失败"),

    /** 删除购物车项失败。 */
    CART_ITEM_DELETE_FAILED(54004, "删除购物车项失败");

    /**
     * 业务错误码。
     */
    private final long code;

    /**
     * 面向调用方的错误信息。
     */
    private final String message;

    /**
     * 创建购物车领域错误码。
     *
     * @param code 业务错误码
     * @param message 错误信息
     */
    CartErrorCode(long code, String message) {
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
     * @return 错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }
}
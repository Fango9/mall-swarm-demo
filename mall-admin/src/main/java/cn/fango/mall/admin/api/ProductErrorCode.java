package cn.fango.mall.admin.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 商品领域错误码。
 */
public enum ProductErrorCode implements IErrorCode {

    /** 商品名称为空。 */
    PRODUCT_NAME_REQUIRED(42001, "商品名称不能为空"),

    /** 商品货号为空。 */
    PRODUCT_SN_REQUIRED(42002, "商品货号不能为空"),

    /** 商品分类为空。 */
    PRODUCT_CATEGORY_REQUIRED(42003, "商品分类不能为空"),

    /** 商品分类不存在。 */
    PRODUCT_CATEGORY_NOT_FOUND(40421, "商品分类不存在"),

    /** 商品货号已存在。 */
    PRODUCT_SN_EXISTS(40921, "商品货号已存在"),

    /** 商品价格非法。 */
    PRODUCT_PRICE_INVALID(42004, "商品价格不能小于 0"),

    /** 商品库存非法。 */
    PRODUCT_STOCK_INVALID(42005, "商品库存不能小于 0"),

    /** 商品上架状态非法。 */
    PRODUCT_PUBLISH_STATUS_INVALID(42006, "商品上架状态只能为 0 或 1"),

    /** 未找到指定商品。 */
    PRODUCT_NOT_FOUND(40422, "商品不存在"),

    /** 创建商品失败。 */
    PRODUCT_CREATE_FAILED(52001, "商品创建失败"),

    /** 更新商品失败。 */
    PRODUCT_UPDATE_FAILED(52002, "商品更新失败"),

    /** 删除商品失败。 */
    PRODUCT_DELETE_FAILED(52003, "商品删除失败");

    private final long code;
    private final String message;

    /**
     * 创建商品领域错误码。
     *
     * @param code 业务错误码
     * @param message 错误信息
     */
    ProductErrorCode(long code, String message) {
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
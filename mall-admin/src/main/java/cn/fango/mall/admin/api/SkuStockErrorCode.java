package cn.fango.mall.admin.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 商品 SKU 领域错误码。
 */
public enum SkuStockErrorCode implements IErrorCode {

    /** SKU 编码为空。 */
    SKU_CODE_REQUIRED(43001, "SKU 编码不能为空"),

    /** SKU 编码重复。 */
    SKU_CODE_EXISTS(40931, "当前商品的 SKU 编码已存在"),

    /** SKU 价格非法。 */
    SKU_PRICE_INVALID(43002, "SKU 价格不能小于 0"),

    /** SKU 库存非法。 */
    SKU_STOCK_INVALID(43003, "SKU 库存不能小于 0"),

    /** 未找到指定 SKU。 */
    SKU_NOT_FOUND(40431, "SKU 不存在"),

    /** 创建 SKU 失败。 */
    SKU_CREATE_FAILED(53001, "SKU 创建失败"),

    /** 更新 SKU 失败。 */
    SKU_UPDATE_FAILED(53002, "SKU 更新失败"),

    /** 删除 SKU 失败。 */
    SKU_DELETE_FAILED(53003, "SKU 删除失败");

    private final long code;
    private final String message;

    /**
     * 创建商品 SKU 领域错误码。
     *
     * @param code 业务错误码
     * @param message 错误信息
     */
    SkuStockErrorCode(long code, String message) {
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
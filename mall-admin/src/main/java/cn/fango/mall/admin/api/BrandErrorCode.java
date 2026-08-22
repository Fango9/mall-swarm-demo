package cn.fango.mall.admin.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 品牌领域错误码。
 */
public enum BrandErrorCode implements IErrorCode {

    /** 品牌名称为空。 */
    BRAND_NAME_REQUIRED(40001, "品牌名称不能为空"),

    /** 品牌创建失败。 */
    BRAND_CREATE_FAILED(50001, "品牌创建失败"),

    /** 未找到指定品牌。 */
    BRAND_NOT_FOUND(40401, "品牌不存在"),

    /** 品牌更新失败。 */
    BRAND_UPDATE_FAILED(50002, "品牌更新失败"),

    /** 品牌删除失败。 */
    BRAND_DELETE_FAILED(50003, "品牌删除失败");

    private final long code;
    private final String message;

    /**
     * 创建品牌领域错误码。
     *
     * @param code 业务错误码
     * @param message 错误信息
     */
    BrandErrorCode(long code, String message) {
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
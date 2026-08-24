package cn.fango.mall.admin.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 商品分类领域错误码。
 */
public enum ProductCategoryErrorCode implements IErrorCode {

    /** 分类名称为空。 */
    CATEGORY_NAME_REQUIRED(41001, "商品分类名称不能为空"),

    /** 分类层级非法。 */
    CATEGORY_LEVEL_INVALID(41002, "商品分类层级只能为 0 或 1"),

    /** 父分类不存在。 */
    PARENT_CATEGORY_NOT_FOUND(40411, "父商品分类不存在"),

    /** 父分类层级不匹配。 */
    PARENT_CATEGORY_LEVEL_INVALID(41003, "父商品分类层级不匹配"),

    /** 未找到指定分类。 */
    CATEGORY_NOT_FOUND(40412, "商品分类不存在"),

    /** 创建分类失败。 */
    CATEGORY_CREATE_FAILED(51001, "商品分类创建失败"),

    /** 更新分类失败。 */
    CATEGORY_UPDATE_FAILED(51002, "商品分类更新失败"),

    /** 分类下仍有子分类。 */
    CATEGORY_HAS_CHILDREN(41004, "商品分类下仍有子分类，不能删除"),

    /** 删除分类失败。 */
    CATEGORY_DELETE_FAILED(51003, "商品分类删除失败");

    private final long code;
    private final String message;

    /**
     * 创建商品分类领域错误码。
     *
     * @param code 业务错误码
     * @param message 错误信息
     */
    ProductCategoryErrorCode(long code, String message) {
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
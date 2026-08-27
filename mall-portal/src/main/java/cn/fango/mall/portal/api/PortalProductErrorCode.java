package cn.fango.mall.portal.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * Portal 商品浏览领域错误码。
 */
public enum PortalProductErrorCode implements IErrorCode {

    /** 请求的商品不存在、已下架或所属分类不可展示。 */
    PRODUCT_NOT_FOUND(40422, "商品不存在"),

    /** 商品详情缓存正在由其他请求重建。 */
    PRODUCT_DETAIL_LOADING(50322, "商品详情正在加载，请稍后重试");

    /**
     * 业务错误码。
     */
    private final long code;

    /**
     * 面向调用方的错误信息。
     */
    private final String message;

    /**
     * 创建 Portal 商品浏览错误码。
     *
     * @param code 业务错误码
     * @param message 面向调用方的错误信息
     */
    PortalProductErrorCode(long code, String message) {
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
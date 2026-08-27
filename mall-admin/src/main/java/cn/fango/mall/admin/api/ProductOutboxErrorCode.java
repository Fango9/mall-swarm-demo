package cn.fango.mall.admin.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 商品变更 Outbox 领域错误码。
 */
public enum ProductOutboxErrorCode implements IErrorCode {

    /** 商品变更后创建 Outbox 事件失败。 */
    OUTBOX_EVENT_CREATE_FAILED(54021, "创建商品变更事件失败");

    /**
     * 业务错误码。
     */
    private final long code;

    /**
     * 面向调用方的错误信息。
     */
    private final String message;

    /**
     * 创建商品变更 Outbox 错误码。
     *
     * @param code 业务错误码
     * @param message 面向调用方的错误信息
     */
    ProductOutboxErrorCode(long code, String message) {
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
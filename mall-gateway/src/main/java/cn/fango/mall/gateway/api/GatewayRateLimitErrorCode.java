package cn.fango.mall.gateway.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * Gateway Redis 限流领域错误码。
 */
public enum GatewayRateLimitErrorCode implements IErrorCode {

    /** 当前分桶的令牌已耗尽，调用方应稍后重试。 */
    REQUEST_RATE_LIMITED(42901, "请求过于频繁，请稍后再试");

    /**
     * 业务错误码。
     */
    private final long code;

    /**
     * 面向调用方的错误信息。
     */
    private final String message;

    /**
     * 创建 Gateway 限流错误码。
     *
     * @param code 业务错误码
     * @param message 面向调用方的错误信息
     */
    GatewayRateLimitErrorCode(long code, String message) {
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
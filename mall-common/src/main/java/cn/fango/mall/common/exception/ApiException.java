package cn.fango.mall.common.exception;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 表示可预期的 API 业务异常。
 *
 * <p>异常可携带 {@link IErrorCode}，供后续全局异常处理器
 * 构造统一的 {@code CommonResult} 失败响应。</p>
 */
public class ApiException extends RuntimeException {

    private IErrorCode errorCode;

    /**
     * 使用错误码的默认消息创建 API 异常。
     *
     * @param errorCode 业务错误码
     */
    public ApiException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码和原始异常原因创建 API 异常。
     *
     * @param errorCode 业务错误码
     * @param cause 原始异常原因
     */
    public ApiException(IErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 使用指定消息创建 API 异常。
     *
     * @param message 异常消息
     */
    public ApiException(String message) {
        super(message);
    }

    /**
     * 使用异常原因创建 API 异常。
     *
     * @param cause 原始异常原因
     */
    public ApiException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用指定消息和异常原因创建 API 异常。
     *
     * @param message 异常消息
     * @param cause 原始异常原因
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 获取异常携带的业务错误码。
     *
     * @return 业务错误码；未通过错误码构造时返回 {@code null}
     */
    public IErrorCode getErrorCode() {
        return errorCode;
    }
}
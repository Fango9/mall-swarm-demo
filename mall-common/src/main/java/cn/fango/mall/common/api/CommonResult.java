package cn.fango.mall.common.api;

import cn.hutool.json.JSONUtil;

/**
 * mall-swarm 统一 API 返回对象。
 *
 * @param <T> 业务数据类型
 */
public class CommonResult<T> {

    private long code;
    private String message;
    private T data;

    /**
     * 创建空的返回对象，供序列化框架使用。
     */
    protected CommonResult() {
    }

    /**
     * 创建包含完整返回信息的对象。
     *
     * @param code 返回码
     * @param message 返回信息
     * @param data 业务数据
     */
    protected CommonResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建使用默认成功消息的成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                data
        );
    }

    /**
     * 创建使用指定成功消息的成功响应。
     *
     * @param data 业务数据
     * @param message 成功提示信息
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> CommonResult<T> success(T data, String message) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 根据错误码创建失败响应。
     *
     * @param errorCode 错误码
     * @param <T> 业务数据类型
     * @return 不携带业务数据的失败响应
     */
    public static <T> CommonResult<T> failed(IErrorCode errorCode) {
        return new CommonResult<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    /**
     * 根据错误码和指定消息创建失败响应。
     *
     * @param errorCode 错误码
     * @param message 面向调用方的错误信息
     * @param <T> 业务数据类型
     * @return 不携带业务数据的失败响应
     */
    public static <T> CommonResult<T> failed(IErrorCode errorCode, String message) {
        return new CommonResult<>(errorCode.getCode(), message, null);
    }

    /**
     * 使用默认失败码和指定消息创建失败响应。
     *
     * @param message 面向调用方的错误信息
     * @param <T> 业务数据类型
     * @return 不携带业务数据的失败响应
     */
    public static <T> CommonResult<T> failed(String message) {
        return new CommonResult<>(ResultCode.FAILED.getCode(), message, null);
    }

    /**
     * 使用默认失败码和默认失败消息创建失败响应。
     *
     * @param <T> 业务数据类型
     * @return 不携带业务数据的失败响应
     */
    public static <T> CommonResult<T> failed() {
        return failed(ResultCode.FAILED);
    }

    /**
     * 创建使用默认校验失败码和默认消息的响应。
     *
     * @param <T> 业务数据类型
     * @return 不携带业务数据的参数校验失败响应
     */
    public static <T> CommonResult<T> validateFailed() {
        return failed(ResultCode.VALIDATE_FAILED);
    }

    /**
     * 创建使用默认校验失败码和指定消息的响应。
     *
     * @param message 面向调用方的校验失败信息
     * @param <T> 业务数据类型
     * @return 不携带业务数据的参数校验失败响应
     */
    public static <T> CommonResult<T> validateFailed(String message) {
        return new CommonResult<>(
                ResultCode.VALIDATE_FAILED.getCode(),
                message,
                null
        );
    }

    /**
     * 创建未登录或令牌失效响应。
     *
     * @param data 返回给调用方的业务数据
     * @param <T> 业务数据类型
     * @return 未登录响应
     */
    public static <T> CommonResult<T> unauthorized(T data) {
        return new CommonResult<>(
                ResultCode.UNAUTHORIZED.getCode(),
                ResultCode.UNAUTHORIZED.getMessage(),
                data
        );
    }

    /**
     * 创建无权限响应。
     *
     * @param data 返回给调用方的业务数据
     * @param <T> 业务数据类型
     * @return 无权限响应
     */
    public static <T> CommonResult<T> forbidden(T data) {
        return new CommonResult<>(
                ResultCode.FORBIDDEN.getCode(),
                ResultCode.FORBIDDEN.getMessage(),
                data
        );
    }

    /**
     * 获取返回码。
     *
     * @return 返回码
     */
    public long getCode() {
        return code;
    }

    /**
     * 设置返回码。
     *
     * @param code 返回码
     */
    public void setCode(long code) {
        this.code = code;
    }

    /**
     * 获取返回信息。
     *
     * @return 返回信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置返回信息。
     *
     * @param message 返回信息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取业务数据。
     *
     * @return 业务数据
     */
    public T getData() {
        return data;
    }

    /**
     * 设置业务数据。
     *
     * @param data 业务数据
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * 将当前返回对象转换为 JSON 字符串。
     *
     * @return JSON 格式的返回对象
     */
    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
    }
}
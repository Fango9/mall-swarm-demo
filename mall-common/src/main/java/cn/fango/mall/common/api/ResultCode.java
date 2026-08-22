package cn.fango.mall.common.api;

/**
 * mall-swarm 公共 API 返回码。
 */
public enum ResultCode implements IErrorCode {

    /** 操作成功。 */
    SUCCESS(200, "操作成功"),

    /** 操作失败。 */
    FAILED(500, "操作失败"),

    /** 请求参数校验失败。 */
    VALIDATE_FAILED(404, "参数检验失败"),

    /** 当前请求未登录或令牌已失效。 */
    UNAUTHORIZED(401, "暂未登录或token已经过期"),

    /** 当前用户没有执行操作所需的权限。 */
    FORBIDDEN(403, "没有相关权限");

    private final long code;
    private final String message;

    /**
     * 创建一个公共返回码。
     *
     * @param code 返回码
     * @param message 默认返回信息
     */
    ResultCode(long code, String message) {
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
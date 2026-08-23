package cn.fango.mall.auth.api;

import cn.fango.mall.common.api.IErrorCode;

/**
 * 认证领域错误码。
 */
public enum AuthErrorCode implements IErrorCode {

    /** 用户名为空。 */
    USERNAME_REQUIRED(40011, "用户名不能为空"),

    /** 密码为空。 */
    PASSWORD_REQUIRED(40012, "密码不能为空"),

    /** 用户名已存在。 */
    USERNAME_ALREADY_EXISTS(40013, "用户名已存在"),

    /** 用户名或密码错误。 */
    LOGIN_FAILED(40014, "用户名或密码错误"),

    /** 会员账号已被禁用。 */
    MEMBER_DISABLED(40015, "账号已被禁用"),

    /** 会员不存在。 */
    MEMBER_NOT_FOUND(40411, "会员不存在"),

    /** 会员创建失败。 */
    MEMBER_CREATE_FAILED(50011, "会员创建失败");

    private final long code;
    private final String message;

    /**
     * 创建认证领域错误码。
     *
     * @param code 业务错误码
     * @param message 默认错误信息
     */
    AuthErrorCode(long code, String message) {
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
package cn.fango.mall.auth.dto;

/**
 * 登录成功响应。
 */
public class LoginResponse {

    private Long memberId;

    private String username;

    private String role;

    private String token;

    private String tokenType;

    /**
     * 创建登录成功响应。
     *
     * @param memberId 会员主键
     * @param username 登录用户名
     * @param role 会员角色
     * @param token 原始访问令牌
     * @param tokenType 令牌类型
     */
    public LoginResponse(
            Long memberId,
            String username,
            String role,
            String token,
            String tokenType
    ) {
        this.memberId = memberId;
        this.username = username;
        this.role = role;
        this.token = token;
        this.tokenType = tokenType;
    }

    /**
     * 获取会员主键。
     *
     * @return 会员主键
     */
    public Long getMemberId() {
        return memberId;
    }

    /**
     * 获取登录用户名。
     *
     * @return 登录用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 获取会员角色。
     *
     * @return 会员角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 获取原始访问令牌。
     *
     * @return 原始访问令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 获取令牌类型。
     *
     * @return 令牌类型
     */
    public String getTokenType() {
        return tokenType;
    }
}
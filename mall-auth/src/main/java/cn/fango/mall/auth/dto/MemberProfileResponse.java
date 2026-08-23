package cn.fango.mall.auth.dto;

/**
 * 当前登录会员的身份信息响应。
 */
public class MemberProfileResponse {

    private Long memberId;

    private String username;

    private String role;

    /**
     * 创建会员身份信息响应。
     *
     * @param memberId 会员主键
     * @param username 登录用户名
     * @param role 会员角色
     */
    public MemberProfileResponse(Long memberId, String username, String role) {
        this.memberId = memberId;
        this.username = username;
        this.role = role;
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
}
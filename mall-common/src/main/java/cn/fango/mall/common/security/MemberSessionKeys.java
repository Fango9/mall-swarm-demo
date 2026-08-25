package cn.fango.mall.common.security;

/**
 * 会员 Redis 会话属性键。
 */
public final class MemberSessionKeys {

    /**
     * 当前登录会员的角色属性键。
     */
    public static final String ROLE = "member-role";

    /**
     * 工具类不允许创建实例。
     */
    private MemberSessionKeys() {
    }

}
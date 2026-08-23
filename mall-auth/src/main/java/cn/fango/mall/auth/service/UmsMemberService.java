package cn.fango.mall.auth.service;

import cn.fango.mall.mbg.model.UmsMember;

/**
 * 会员身份服务。
 */
public interface UmsMemberService {

    /**
     * 注册普通会员。
     *
     * @param username 登录用户名
     * @param rawPassword 明文密码
     * @return 新会员主键
     */
    Long register(String username, String rawPassword);

    /**
     * 校验会员账号与密码。
     *
     * <p>该方法只完成身份认证，不创建登录会话，也不返回给外部调用方。</p>
     *
     * @param username 登录用户名
     * @param rawPassword 明文密码
     * @return 已认证的会员实体
     */
    UmsMember authenticate(String username, String rawPassword);

    /**
     * 按主键查询会员。
     *
     * @param memberId 会员主键
     * @return 查询到的会员；不存在时返回 {@code null}
     */
    UmsMember findById(Long memberId);

}
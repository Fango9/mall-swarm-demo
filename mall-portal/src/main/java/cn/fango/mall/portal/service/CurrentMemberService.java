package cn.fango.mall.portal.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.fango.mall.common.api.ResultCode;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.security.MemberSessionKeys;
import org.springframework.stereotype.Service;

/**
 * 获取当前已登录会员身份的服务。
 */
@Service
public class CurrentMemberService {

    /**
     * 允许访问会员购物车和订单资源的角色。
     */
    private static final String MEMBER_ROLE = "MEMBER";

    /**
     * 获取当前登录的 MEMBER 会员主键。
     *
     * <p>会员主键和角色均来自 Redis 中的 Sa-Token 服务端会话，
     * 不信任客户端请求中提供的身份字段。</p>
     *
     * @return 当前登录会员主键
     */
    public Long getCurrentMemberId() {
        if (!StpUtil.isLogin()) {
            throw new ApiException(ResultCode.UNAUTHORIZED);
        }

        Object role = StpUtil.getSession().get(MemberSessionKeys.ROLE);
        if (!MEMBER_ROLE.equals(role)) {
            throw new ApiException(ResultCode.FORBIDDEN);
        }

        return StpUtil.getLoginIdAsLong();
    }

}
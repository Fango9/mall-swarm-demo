package cn.fango.mall.gateway.config;

import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.security.MemberSessionKeys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway 的 Sa-Token 鉴权配置。
 */
@Configuration
public class SaTokenGatewayConfig {

    /**
     * 允许访问购物车和订单接口的会员角色。
     */
    private static final String MEMBER_ROLE = "MEMBER";

    /**
     * 允许访问监控中心的管理员角色。
     */
    private static final String ADMIN_ROLE = "ADMIN";

    /**
     * 创建 Gateway 鉴权过滤器。
     *
     * <p>后台接口只校验已登录状态，具体 ADMIN 权限仍由 mall-admin 校验。
     * 购物车和订单接口要求已登录，且 Redis 会话中的角色必须为 MEMBER。</p>
     *
     * @return Sa-Token Reactor 过滤器
     */
    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .setAuth(object -> {
                    SaRouter.match(
                            "/admin/**",
                            route -> StpUtil.checkLogin()
                    );

                    SaRouter.match(
                            "/portal/cart/**",
                            route -> checkMemberRole()
                    );

                    SaRouter.match(
                            "/portal/orders/**",
                            route -> checkMemberRole()
                    );

                    SaRouter.match(
                            "/monitor",
                            route -> checkAdminRole()
                    );

                    SaRouter.match(
                            "/monitor/**",
                            route -> checkAdminRole()
                    );
                })
                .setError(throwable -> {
                    if (throwable instanceof NotRoleException) {
                        return CommonResult.forbidden(null).toString();
                    }

                    return CommonResult.unauthorized(null).toString();
                });
    }

    /**
     * 校验当前请求已经登录，且登录会话中保存的角色为 MEMBER。
     */
    private void checkMemberRole() {
        StpUtil.checkLogin();

        Object role = StpUtil.getSession().get(MemberSessionKeys.ROLE);
        if (!MEMBER_ROLE.equals(role)) {
            throw new NotRoleException(MEMBER_ROLE, StpUtil.getLoginType());
        }
    }

    /**
     * 校验当前请求已经登录，且登录会话中保存的角色为 ADMIN。
     */
    private void checkAdminRole() {
        StpUtil.checkLogin();

        Object role = StpUtil.getSession().get(MemberSessionKeys.ROLE);
        if (!ADMIN_ROLE.equals(role)) {
            throw new NotRoleException(ADMIN_ROLE, StpUtil.getLoginType());
        }
    }
}
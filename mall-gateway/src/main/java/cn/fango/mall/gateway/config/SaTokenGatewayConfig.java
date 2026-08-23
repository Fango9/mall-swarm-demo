package cn.fango.mall.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.fango.mall.common.api.CommonResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway 的 Sa-Token 鉴权配置。
 */
@Configuration
public class SaTokenGatewayConfig {

    /**
     * 创建 Gateway 鉴权过滤器。
     *
     * <p>认证接口和 Demo 接口不会匹配 {@code /admin/**}，因此会直接放行。
     * 后台接口必须先通过登录校验，再转发到 mall-admin 进行角色校验。</p>
     *
     * @return Sa-Token Reactor 过滤器
     */
    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .setAuth(object -> SaRouter.match(
                        "/admin/**",
                        route -> StpUtil.checkLogin()
                ))
                .setError(throwable -> CommonResult.unauthorized(null).toString());
    }

}
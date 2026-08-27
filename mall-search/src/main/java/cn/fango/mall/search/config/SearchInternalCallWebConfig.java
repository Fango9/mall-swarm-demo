package cn.fango.mall.search.config;

import cn.fango.mall.common.api.CommonResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/**
 * mall-search 内部服务调用的访问控制配置。
 */
@Configuration
public class SearchInternalCallWebConfig implements WebMvcConfigurer {

    private static final String INTERNAL_TOKEN_HEADER = "X-Mall-Internal-Token";

    private final String internalToken;

    /**
     * 创建内部服务调用访问控制配置。
     *
     * @param internalToken Nacos 配置的内部调用令牌
     */
    public SearchInternalCallWebConfig(@Value("${mall.internal.token}") String internalToken) {
        this.internalToken = internalToken;
    }

    /**
     * 注册内部接口令牌校验拦截器。
     *
     * @param registry Spring MVC 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new InternalCallInterceptor())
                .addPathPatterns("/internal/**");
    }

    /**
     * 校验内部调用令牌的拦截器。
     */
    private class InternalCallInterceptor implements HandlerInterceptor {

        /**
         * 在访问内部接口前校验请求头中的内部令牌。
         *
         * @param request 当前 HTTP 请求
         * @param response 当前 HTTP 响应
         * @param handler 当前处理器
         * @return 令牌有效时返回 true，否则返回 false
         * @throws IOException 写出拒绝响应失败时抛出
         */
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
            String requestToken = request.getHeader(INTERNAL_TOKEN_HEADER);
            if (!StringUtils.hasText(internalToken) || !internalToken.equals(requestToken)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(CommonResult.forbidden(null).toString());
                return false;
            }

            return true;
        }
    }
}
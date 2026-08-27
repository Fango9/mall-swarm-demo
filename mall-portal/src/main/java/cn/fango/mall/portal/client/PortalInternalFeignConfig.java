package cn.fango.mall.portal.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * 商城门户调用内部服务的 Feign 配置。
 */
public class PortalInternalFeignConfig {

    private static final String INTERNAL_TOKEN_HEADER = "X-Mall-Internal-Token";

    private final String internalToken;

    /**
     * 创建商城门户内部服务 Feign 配置。
     *
     * @param internalToken Nacos 配置的内部调用令牌
     */
    public PortalInternalFeignConfig(@Value("${mall.internal.token}") String internalToken) {
        this.internalToken = internalToken;
    }

    /**
     * 为调用内部服务的 Feign 请求附加内部令牌。
     *
     * @return 内部令牌请求拦截器
     */
    @Bean
    public RequestInterceptor internalTokenRequestInterceptor() {
        return requestTemplate ->
                requestTemplate.header(INTERNAL_TOKEN_HEADER, internalToken);
    }

}
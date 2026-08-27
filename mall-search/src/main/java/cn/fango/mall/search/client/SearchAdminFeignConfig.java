package cn.fango.mall.search.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * 商品搜索服务调用 Admin 的 Feign 配置。
 */
public class SearchAdminFeignConfig {

    /**
     * 内部服务调用令牌。
     */
    private final String internalToken;

    /**
     * 创建商品搜索服务的 Admin Feign 配置。
     *
     * @param internalToken Nacos 配置的内部调用令牌
     */
    public SearchAdminFeignConfig(
            @Value("${mall.internal.token}") String internalToken
    ) {
        this.internalToken = internalToken;
    }

    /**
     * 为调用 Admin 的 Feign 请求附加内部令牌。
     *
     * @return 内部令牌请求拦截器
     */
    @Bean
    public RequestInterceptor internalTokenRequestInterceptor() {
        return requestTemplate ->
                requestTemplate.header("X-Mall-Internal-Token", internalToken);
    }
}
package cn.fango.mall.gateway.config;

import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * mall-gateway 的 Actuator 专用安全配置。
 *
 * <p>健康检查允许匿名访问；其余 Actuator 端点必须使用 Basic Auth。
 * 非 Actuator 路径继续由现有 Sa-Token Reactor 过滤器处理。</p>
 */
@Configuration(proxyBeanMethods = false)
public class GatewayActuatorSecurityConfig {

    /**
     * 配置 Gateway Actuator 端点的安全过滤器链。
     *
     * @param http Spring Security 的 WebFlux HTTP 配置器
     * @return Actuator 专用安全过滤器链
     */
    @Bean
    @Order(1)
    public SecurityWebFilterChain actuatorSecurityWebFilterChain(ServerHttpSecurity http) {
        return http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeExchange(authorize -> authorize
                        .matchers(EndpointRequest.to("health")).permitAll()
                        .anyExchange().hasRole("ACTUATOR"))
                .httpBasic(Customizer.withDefaults())
                .csrf(CsrfSpec::disable)
                .build();
    }

    /**
     * 配置 Gateway 非 Actuator 路径的安全过滤器链。
     *
     * <p>该链不改变既有业务鉴权；Sa-Token Reactor 过滤器仍负责
     * Gateway 的管理员与会员路径校验。</p>
     *
     * @param http Spring Security 的 WebFlux HTTP 配置器
     * @return 非 Actuator 路径安全过滤器链
     */
    @Bean
    @Order(2)
    public SecurityWebFilterChain applicationSecurityWebFilterChain(ServerHttpSecurity http) {
        return http.authorizeExchange(authorize -> authorize
                        .anyExchange().permitAll())
                .csrf(CsrfSpec::disable)
                .build();
    }
}
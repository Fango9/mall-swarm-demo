package cn.fango.mall.portal.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * mall-demo 的 Actuator 专用安全配置。
 *
 * <p>健康检查允许匿名访问，便于基础探活；其余 Actuator 端点必须使用
 * Basic Auth 认证。业务接口不由此配置保护，仍保持当前访问行为。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PortalActuatorSecurityConfig {

    /**
     * 配置 Actuator 端点的安全过滤器链。
     *
     * @param http Spring Security 的 HTTP 配置器
     * @return Actuator 专用安全过滤器链
     * @throws Exception 配置安全过滤器链失败时抛出
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(EndpointRequest.to("health")).permitAll()
                        .anyRequest().hasRole("ACTUATOR"))
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * 配置非 Actuator 路径的安全过滤器链。
     *
     * <p>mall-demo 当前没有业务鉴权需求，因此保持业务路径可访问；
     * 后续 Admin、Auth、Portal 等服务会保留各自的 Sa-Token 鉴权。</p>
     *
     * @param http Spring Security 的 HTTP 配置器
     * @return 业务路径安全过滤器链
     * @throws Exception 配置安全过滤器链失败时抛出
     */
    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
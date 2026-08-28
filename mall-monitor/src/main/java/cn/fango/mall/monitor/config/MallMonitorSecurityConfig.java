package cn.fango.mall.monitor.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * mall-monitor 的 Web 安全配置。
 *
 * <p>监控页面及其他请求需要登录；静态资源和登录页除外。
 * 同时仅对 Spring Boot Admin 必需的注册接口及自身 Actuator 接口放宽 CSRF 校验。</p>
 */
@Configuration(proxyBeanMethods = false)
public class MallMonitorSecurityConfig {

    private final AdminServerProperties adminServerProperties;

    /**
     * 创建监控中心安全配置。
     *
     * @param adminServerProperties Spring Boot Admin 服务端属性
     */
    public MallMonitorSecurityConfig(AdminServerProperties adminServerProperties) {
        this.adminServerProperties = adminServerProperties;
    }

    /**
     * 配置 Spring Boot Admin 的登录与 CSRF 规则。
     *
     * @param http Spring Security 的 HTTP 配置器
     * @return 已构建的安全过滤器链
     * @throws Exception 配置安全过滤器链失败时抛出
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServerProperties.path("/"));

        PathPatternRequestMatcher.Builder requestMatchers = PathPatternRequestMatcher.withDefaults();

        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(this.adminServerProperties.path("/assets/**")).permitAll()
                        .requestMatchers(this.adminServerProperties.path("/login")).permitAll()
                        .anyRequest().authenticated())
                .formLogin(formLogin -> formLogin
                        .loginPage(this.adminServerProperties.path("/login"))
                        .successHandler(successHandler))
                .logout(logout -> logout
                        .logoutUrl(this.adminServerProperties.path("/logout")))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                requestMatchers.matcher(
                                        HttpMethod.POST,
                                        this.adminServerProperties.path("/instances")),
                                requestMatchers.matcher(
                                        HttpMethod.DELETE,
                                        this.adminServerProperties.path("/instances/*")),
                                requestMatchers.matcher(
                                        this.adminServerProperties.path("/actuator/**"))));

        return http.build();
    }
}
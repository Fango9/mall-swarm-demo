package cn.fango.mall.gateway.config;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Gateway Redis 令牌桶限流的分桶键配置。
 *
 * <p>购物车和下单接口按已登录会员 ID 分桶；公共商品浏览接口按客户端 IP 分桶。
 * 本地直连场景不信任可伪造的 X-Forwarded-For 请求头。</p>
 */
@Configuration
public class GatewayRateLimitKeyResolverConfig {

    /**
     * 会员限流键前缀。
     */
    private static final String MEMBER_KEY_PREFIX = "member:";

    /**
     * 客户端 IP 限流键前缀。
     */
    private static final String CLIENT_IP_KEY_PREFIX = "ip:";

    /**
     * 无法取得远端地址时使用的保底分桶值。
     */
    private static final String UNKNOWN_CLIENT_IP = "unknown";

    /**
     * Bearer Token 的 HTTP 认证方案前缀。
     */
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";

    /**
     * 创建按会员 ID 分桶的限流键解析器。
     *
     * <p>购物车、下单路由已经由 Sa-Token 要求 MEMBER 登录；
     * {@code anonymous} 仅用于异常兜底，避免产生空分桶键。</p>
     *
     * @return 按会员 ID 生成 Redis 令牌桶键的解析器
     */
    @Primary
    @Bean("memberRateLimitKeyResolver")
    public KeyResolver memberRateLimitKeyResolver() {
        return exchange -> Mono.just(MEMBER_KEY_PREFIX + resolveMemberId(exchange));
    }

    /**
     * 创建按客户端 IP 分桶的限流键解析器。
     *
     * @return 按客户端 IP 生成 Redis 令牌桶键的解析器
     */
    @Bean("clientIpRateLimitKeyResolver")
    public KeyResolver clientIpRateLimitKeyResolver() {
        return exchange -> Mono.just(CLIENT_IP_KEY_PREFIX + resolveClientIp(exchange));
    }

    /**
     * 从请求连接信息取得客户端 IP。
     *
     * @param exchange 当前 Gateway 请求交换对象
     * @return 客户端 IP；无法取得时返回保底值
     */
    private String resolveClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return UNKNOWN_CLIENT_IP;
        }

        return remoteAddress.getAddress().getHostAddress();
    }

    /**
     * 从当前请求的 Bearer Token 解析会员 ID。
     *
     * <p>该方法不依赖 Sa-Token 的线程本地上下文，因此可安全运行在
     * Spring Cloud Gateway 的 Reactor 限流回调中。</p>
     *
     * @param exchange 当前 Gateway 请求交换对象
     * @return 已登录会员 ID；无法解析时返回 {@code anonymous}
     */
    private String resolveMemberId(ServerWebExchange exchange) {
        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_TOKEN_PREFIX)) {
            return "anonymous";
        }

        String tokenValue = authorization.substring(BEARER_TOKEN_PREFIX.length());
        Object loginId = StpUtil.getLoginIdByToken(tokenValue);

        return loginId == null ? "anonymous" : loginId.toString();
    }
}
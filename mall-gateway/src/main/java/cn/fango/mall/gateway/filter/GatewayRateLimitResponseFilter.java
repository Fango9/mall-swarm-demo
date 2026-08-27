package cn.fango.mall.gateway.filter;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.gateway.api.GatewayRateLimitErrorCode;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 将 Gateway Redis 令牌桶拒绝的 HTTP 429 响应转换为统一 JSON。
 *
 * <p>内置 {@code RequestRateLimiter} 在令牌不足时直接调用 {@code setComplete()}，
 * 不会进入普通异常处理流程，因此需要在响应提交前拦截该操作。</p>
 */
@Component
public class GatewayRateLimitResponseFilter implements GlobalFilter, Ordered {

    /**
     * 限流响应体使用的 UTF-8 字节数组。
     */
    private static final byte[] RATE_LIMIT_RESPONSE_BODY = CommonResult
            .failed(GatewayRateLimitErrorCode.REQUEST_RATE_LIMITED)
            .toString()
            .getBytes(StandardCharsets.UTF_8);

    /**
     * 用响应装饰器拦截限流器提交的 HTTP 429。
     *
     * @param exchange 当前 Gateway 请求交换对象
     * @param chain Gateway 过滤器链
     * @return 异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {

            /**
             * 在 Gateway 准备结束响应时写入统一限流 JSON。
             *
             * @return 异步写入结果
             */
            @Override
            public Mono<Void> setComplete() {
                if (!HttpStatus.TOO_MANY_REQUESTS.equals(getStatusCode())) {
                    return super.setComplete();
                }

                getHeaders().setContentType(MediaType.APPLICATION_JSON);
                getHeaders().setContentLength(RATE_LIMIT_RESPONSE_BODY.length);

                DataBuffer buffer = bufferFactory().wrap(RATE_LIMIT_RESPONSE_BODY);
                return writeWith(Mono.just(buffer));
            }
        };

        return chain.filter(exchange.mutate().response(responseDecorator).build());
    }

    /**
     * 在路由过滤器之前包装响应，确保能够截获限流器的直接提交动作。
     *
     * @return Gateway 过滤器优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
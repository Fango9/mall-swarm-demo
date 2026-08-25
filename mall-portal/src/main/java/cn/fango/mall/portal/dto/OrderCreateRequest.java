package cn.fango.mall.portal.dto;

import java.util.List;

/**
 * 创建订单请求。
 *
 * <p>幂等键不放在请求体中，而由 HTTP 请求头 {@code Idempotency-Key} 提供。
 * 这样相同请求体在不同重试策略下也不会意外复用幂等键。</p>
 *
 * @param cartItemIds 本次需要结算的购物车项主键列表
 */
public record OrderCreateRequest(
        List<Long> cartItemIds
) {
}
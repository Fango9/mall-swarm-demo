package cn.fango.mall.portal.service;

import cn.fango.mall.portal.dto.OrderCreateRequest;
import cn.fango.mall.portal.dto.OrderDetailResponse;

/**
 * 会员订单服务。
 */
public interface OrderService {

    /**
     * 从当前会员选中的购物车项创建待支付订单。
     *
     * <p>同一会员使用相同幂等键重复调用时，必须返回第一次创建的订单，
     * 不得重复预占库存、创建订单或清理购物车。</p>
     *
     * @param memberId 当前登录会员主键
     * @param idempotencyKey HTTP 请求头 {@code Idempotency-Key} 的值
     * @param request 下单请求
     * @return 已创建或幂等命中的订单详情
     */
    OrderDetailResponse createOrder(
            Long memberId,
            String idempotencyKey,
            OrderCreateRequest request
    );

    /**
     * 查询当前会员拥有的订单详情。
     *
     * @param memberId 当前登录会员主键
     * @param orderId 订单主键
     * @return 订单及其明细快照
     */
    OrderDetailResponse getOrderDetail(Long memberId, Long orderId);
}
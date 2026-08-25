package cn.fango.mall.portal.controller;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.portal.dto.OrderCreateRequest;
import cn.fango.mall.portal.dto.OrderDetailResponse;
import cn.fango.mall.portal.service.CurrentMemberService;
import cn.fango.mall.portal.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员订单接口。
 */
@RestController
@RequestMapping("/portal/orders")
public class OrderController {

    /**
     * 会员订单服务。
     */
    private final OrderService orderService;

    /**
     * 当前登录会员身份服务。
     */
    private final CurrentMemberService currentMemberService;

    /**
     * 创建会员订单接口。
     *
     * @param orderService 会员订单服务
     * @param currentMemberService 当前登录会员身份服务
     */
    public OrderController(
            OrderService orderService,
            CurrentMemberService currentMemberService
    ) {
        this.orderService = orderService;
        this.currentMemberService = currentMemberService;
    }

    /**
     * 从当前会员选中的购物车项创建待支付订单。
     *
     * <p>每次请求都必须提供 {@code Idempotency-Key} 请求头；
     * 该键不信任客户端会员身份，仅用于当前已登录会员的重复提交识别。</p>
     *
     * @param idempotencyKey 下单幂等键
     * @param request 下单请求
     * @return 统一响应中的订单详情
     */
    @PostMapping
    public CommonResult<OrderDetailResponse> createOrder(
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            ) String idempotencyKey,
            @RequestBody(required = false) OrderCreateRequest request
    ) {
        Long memberId = currentMemberService.getCurrentMemberId();
        OrderDetailResponse order = orderService.createOrder(
                memberId,
                idempotencyKey,
                request
        );

        return CommonResult.success(order);
    }

    /**
     * 查询当前会员自己的订单详情。
     *
     * @param orderId 订单主键
     * @return 统一响应中的订单详情
     */
    @GetMapping("/{orderId}")
    public CommonResult<OrderDetailResponse> getOrderDetail(
            @PathVariable Long orderId
    ) {
        Long memberId = currentMemberService.getCurrentMemberId();
        OrderDetailResponse order =
                orderService.getOrderDetail(memberId, orderId);

        return CommonResult.success(order);
    }
}
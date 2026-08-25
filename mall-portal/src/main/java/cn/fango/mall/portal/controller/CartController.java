package cn.fango.mall.portal.controller;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.portal.dto.CartItemAddRequest;
import cn.fango.mall.portal.dto.CartItemQuantityUpdateRequest;
import cn.fango.mall.portal.dto.CartItemResponse;
import cn.fango.mall.portal.service.CartService;
import cn.fango.mall.portal.service.CurrentMemberService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员购物车接口。
 */
@RestController
@RequestMapping("/portal/cart/items")
public class CartController {

    /**
     * 会员购物车服务。
     */
    private final CartService cartService;

    /**
     * 当前登录会员身份服务。
     */
    private final CurrentMemberService currentMemberService;

    /**
     * 创建会员购物车接口。
     *
     * @param cartService 会员购物车服务
     * @param currentMemberService 当前登录会员身份服务
     */
    public CartController(
            CartService cartService,
            CurrentMemberService currentMemberService
    ) {
        this.cartService = cartService;
        this.currentMemberService = currentMemberService;
    }

    /**
     * 将指定 SKU 加入当前会员购物车。
     *
     * @param request 新增购物车项请求
     * @return 统一响应中的新增或合并后的购物车项主键
     */
    @PostMapping
    public CommonResult<Long> addCartItem(
            @RequestBody(required = false) CartItemAddRequest request
    ) {
        Long memberId = currentMemberService.getCurrentMemberId();
        Long cartItemId = cartService.addCartItem(memberId, request);

        return CommonResult.success(cartItemId);
    }

    /**
     * 查询当前会员的购物车项。
     *
     * @return 统一响应中的购物车项列表
     */
    @GetMapping
    public CommonResult<List<CartItemResponse>> listCartItems() {
        Long memberId = currentMemberService.getCurrentMemberId();
        List<CartItemResponse> cartItems =
                cartService.listCartItems(memberId);

        return CommonResult.success(cartItems);
    }

    /**
     * 修改当前会员指定购物车项的购买数量。
     *
     * @param cartItemId 购物车项主键
     * @param request 修改数量请求
     * @return 统一响应中的修改结果
     */
    @PutMapping("/{cartItemId}")
    public CommonResult<Boolean> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @RequestBody(required = false)
            CartItemQuantityUpdateRequest request
    ) {
        Long memberId = currentMemberService.getCurrentMemberId();
        boolean updated = cartService.updateCartItemQuantity(
                memberId,
                cartItemId,
                request
        );

        return CommonResult.success(updated);
    }

    /**
     * 删除当前会员指定购物车项。
     *
     * @param cartItemId 购物车项主键
     * @return 统一响应中的删除结果
     */
    @DeleteMapping("/{cartItemId}")
    public CommonResult<Boolean> deleteCartItem(
            @PathVariable Long cartItemId
    ) {
        Long memberId = currentMemberService.getCurrentMemberId();
        boolean deleted = cartService.deleteCartItem(memberId, cartItemId);

        return CommonResult.success(deleted);
    }
}
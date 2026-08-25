package cn.fango.mall.portal.service;

import cn.fango.mall.portal.dto.CartItemAddRequest;
import cn.fango.mall.portal.dto.CartItemQuantityUpdateRequest;
import cn.fango.mall.portal.dto.CartItemResponse;

import java.util.List;

/**
 * 会员购物车服务。
 */
public interface CartService {

    /**
     * 将指定 SKU 加入当前会员的购物车。
     *
     * @param memberId 当前登录会员主键
     * @param request 新增购物车项请求
     * @return 新增或合并后的购物车项主键
     */
    Long addCartItem(Long memberId, CartItemAddRequest request);

    /**
     * 查询当前会员的全部购物车项。
     *
     * @param memberId 当前登录会员主键
     * @return 按最近修改时间倒序排列的购物车项列表
     */
    List<CartItemResponse> listCartItems(Long memberId);

    /**
     * 修改当前会员指定购物车项的购买数量。
     *
     * @param memberId 当前登录会员主键
     * @param cartItemId 购物车项主键
     * @param request 修改数量请求
     * @return 是否修改成功
     */
    boolean updateCartItemQuantity(
            Long memberId,
            Long cartItemId,
            CartItemQuantityUpdateRequest request
    );

    /**
     * 删除当前会员指定购物车项。
     *
     * @param memberId 当前登录会员主键
     * @param cartItemId 购物车项主键
     * @return 是否删除成功
     */
    boolean deleteCartItem(Long memberId, Long cartItemId);
}
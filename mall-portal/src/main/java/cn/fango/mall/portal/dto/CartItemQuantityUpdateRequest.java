package cn.fango.mall.portal.dto;

/**
 * 修改购物车项数量请求。
 *
 * @param quantity 修改后的购买数量
 */
public record CartItemQuantityUpdateRequest(
        Integer quantity
) {
}
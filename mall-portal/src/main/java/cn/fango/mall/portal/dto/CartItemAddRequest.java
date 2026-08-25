package cn.fango.mall.portal.dto;

/**
 * 新增购物车项请求。
 *
 * @param productId 商品主键
 * @param skuId SKU 主键
 * @param quantity 加入购物车的数量
 */
public record CartItemAddRequest(
        Long productId,
        Long skuId,
        Integer quantity
) {
}
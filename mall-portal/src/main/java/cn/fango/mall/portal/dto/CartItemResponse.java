package cn.fango.mall.portal.dto;

import java.math.BigDecimal;

/**
 * 购物车项响应。
 *
 * @param id 购物车项主键
 * @param productId 商品主键
 * @param productName 商品名称快照
 * @param productPic 商品图片快照
 * @param skuId SKU 主键
 * @param skuCode SKU 编码快照
 * @param price SKU 单价快照
 * @param quantity 购买数量
 * @param totalAmount 当前购物车项金额
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String productPic,
        Long skuId,
        String skuCode,
        BigDecimal price,
        Integer quantity,
        BigDecimal totalAmount
) {
}
package cn.fango.mall.portal.dto;

import java.math.BigDecimal;

/**
 * 订单明细响应，字段均为下单时保存的商品与 SKU 快照。
 *
 * @param productId 商品主键
 * @param productName 商品名称快照
 * @param productPic 商品图片快照
 * @param skuId SKU 主键
 * @param skuCode SKU 编码快照
 * @param skuAttrs SKU 销售属性快照
 * @param price 下单时 SKU 单价
 * @param quantity 购买数量
 * @param totalAmount 当前订单明细金额
 */
public record OrderItemResponse(
        Long productId,
        String productName,
        String productPic,
        Long skuId,
        String skuCode,
        String skuAttrs,
        BigDecimal price,
        Integer quantity,
        BigDecimal totalAmount
) {
}
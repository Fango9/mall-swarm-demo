package cn.fango.mall.common.product;

import java.math.BigDecimal;

/**
 * 商品 SKU 查询响应。
 *
 * @param id SKU 主键
 * @param skuCode SKU 编码
 * @param price SKU 销售价格
 * @param stock SKU 库存
 * @param pic SKU 图片
 * @param saleAttrs 销售属性 JSON
 */
public record SkuStockResponse(
        Long id,
        String skuCode,
        BigDecimal price,
        Integer stock,
        String pic,
        String saleAttrs
) {
}
package cn.fango.mall.admin.dto;

import java.math.BigDecimal;

/**
 * 创建商品 SKU 请求。
 *
 * @param skuCode SKU 编码
 * @param price SKU 销售价格
 * @param stock SKU 库存
 * @param pic SKU 图片
 * @param saleAttrs 销售属性 JSON
 */
public record SkuStockCreateRequest(
        String skuCode,
        BigDecimal price,
        Integer stock,
        String pic,
        String saleAttrs
) {
}
package cn.fango.mall.common.product;

import java.math.BigDecimal;

/**
 * 商品列表查询响应。
 *
 * @param id 商品主键
 * @param productCategoryId 商品分类主键
 * @param name 商品名称
 * @param productSn 商品货号
 * @param price 商品最低销售价格
 * @param stock 商品总库存
 * @param pic 商品主图
 */
public record ProductSummaryResponse(
        Long id,
        Long productCategoryId,
        String name,
        String productSn,
        BigDecimal price,
        Integer stock,
        String pic
) {
}
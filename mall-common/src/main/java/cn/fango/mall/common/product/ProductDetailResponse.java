package cn.fango.mall.common.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情查询响应。
 *
 * @param id 商品主键
 * @param productCategoryId 商品分类主键
 * @param name 商品名称
 * @param productSn 商品货号
 * @param price 商品最低销售价格
 * @param stock 商品总库存
 * @param pic 商品主图
 * @param detailDesc 商品详情描述
 * @param skuStocks 商品 SKU 列表
 */
public record ProductDetailResponse(
        Long id,
        Long productCategoryId,
        String name,
        String productSn,
        BigDecimal price,
        Integer stock,
        String pic,
        String detailDesc,
        List<SkuStockResponse> skuStocks
) {
}
package cn.fango.mall.common.search;

import java.math.BigDecimal;

/**
 * Admin 提供给商品搜索服务的可索引商品数据。
 *
 * <p>该对象只承载已发布商品的索引源数据；
 * 未发布、已删除或不存在的商品不会返回该对象。</p>
 *
 * @param id 商品主键
 * @param productCategoryId 商品分类主键
 * @param name 商品名称
 * @param productSn 商品货号
 * @param price 商品最低销售价格
 * @param stock 商品总库存
 * @param pic 商品主图
 * @param detailDesc 商品详情描述
 */
public record ProductSearchSourceResponse(
        Long id,
        Long productCategoryId,
        String name,
        String productSn,
        BigDecimal price,
        Integer stock,
        String pic,
        String detailDesc
) {
}
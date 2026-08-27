package cn.fango.mall.common.search;

import java.math.BigDecimal;

/**
 * 商品关键词搜索结果。
 *
 * <p>该对象是 mall-search 对外提供的搜索结果契约，
 * 不绑定 MySQL 实体，也不暴露 Elasticsearch 文档实现细节。</p>
 *
 * @param id 商品主键
 * @param productCategoryId 商品分类主键
 * @param name 商品名称
 * @param productSn 商品货号
 * @param price 商品最低销售价格
 * @param pic 商品主图
 */
public record ProductSearchResponse(
        Long id,
        Long productCategoryId,
        String name,
        String productSn,
        BigDecimal price,
        String pic
) {
}
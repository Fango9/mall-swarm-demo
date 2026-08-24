package cn.fango.mall.admin.dto;

import java.math.BigDecimal;

/**
 * 更新商品请求。
 *
 * @param brandId 品牌主键，未关联品牌时传 0
 * @param productCategoryId 商品分类主键
 * @param name 商品名称
 * @param productSn 商品货号
 * @param publishStatus 上架状态：0->下架；1->上架
 * @param sort 排序值
 * @param price 商品销售价格
 * @param stock 商品总库存
 * @param lowStock 库存预警值
 * @param pic 商品主图
 * @param detailDesc 商品详情描述
 */
public record ProductUpdateRequest(
        Long brandId,
        Long productCategoryId,
        String name,
        String productSn,
        Byte publishStatus,
        Integer sort,
        BigDecimal price,
        Integer stock,
        Integer lowStock,
        String pic,
        String detailDesc
) {
}
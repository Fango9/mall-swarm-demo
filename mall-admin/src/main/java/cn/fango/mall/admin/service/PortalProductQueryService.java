package cn.fango.mall.admin.service;

import cn.fango.mall.common.product.ProductCategoryResponse;
import cn.fango.mall.common.product.ProductDetailResponse;
import cn.fango.mall.common.product.ProductSummaryResponse;

import java.util.List;

/**
 * 为商城门户提供的商品只读查询服务。
 */
public interface PortalProductQueryService {

    /**
     * 查询全部可展示商品分类。
     *
     * @return 可展示商品分类列表
     */
    List<ProductCategoryResponse> listVisibleCategories();

    /**
     * 查询可展示商品。
     *
     * @param categoryId 商品分类主键；传 null 时查询全部分类
     * @return 可展示商品列表
     */
    List<ProductSummaryResponse> listPublishedProducts(Long categoryId);

    /**
     * 查询可展示商品详情及其 SKU。
     *
     * @param productId 商品主键
     * @return 商品详情及 SKU 列表
     */
    ProductDetailResponse getPublishedProductDetail(Long productId);
}
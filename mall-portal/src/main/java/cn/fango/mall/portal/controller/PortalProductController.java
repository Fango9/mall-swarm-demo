package cn.fango.mall.portal.controller;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.product.ProductCategoryResponse;
import cn.fango.mall.common.product.ProductDetailResponse;
import cn.fango.mall.common.product.ProductSummaryResponse;
import cn.fango.mall.portal.client.PortalProductClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城门户公开商品接口。
 */
@RestController
@RequestMapping("/portal")
public class PortalProductController {

    private final PortalProductClient portalProductClient;

    /**
     * 创建商城门户商品接口。
     *
     * @param portalProductClient 后台商品查询 Feign 客户端
     */
    public PortalProductController(PortalProductClient portalProductClient) {
        this.portalProductClient = portalProductClient;
    }

    /**
     * 查询可展示商品分类。
     *
     * @return 统一响应中的商品分类列表
     */
    @GetMapping("/categories")
    public CommonResult<List<ProductCategoryResponse>> listVisibleCategories() {
        return portalProductClient.listVisibleCategories();
    }

    /**
     * 查询可展示商品。
     *
     * @param categoryId 商品分类主键；不传时查询全部分类
     * @return 统一响应中的商品列表
     */
    @GetMapping("/products")
    public CommonResult<List<ProductSummaryResponse>> listPublishedProducts(
            @RequestParam(required = false) Long categoryId
    ) {
        return portalProductClient.listPublishedProducts(categoryId);
    }

    /**
     * 查询可展示商品详情。
     *
     * @param productId 商品主键
     * @return 统一响应中的商品详情和 SKU 列表
     */
    @GetMapping("/products/{productId}")
    public CommonResult<ProductDetailResponse> getPublishedProductDetail(
            @PathVariable Long productId
    ) {
        return portalProductClient.getPublishedProductDetail(productId);
    }
}
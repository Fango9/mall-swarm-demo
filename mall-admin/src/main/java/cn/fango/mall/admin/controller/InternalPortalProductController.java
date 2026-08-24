package cn.fango.mall.admin.controller;

import cn.fango.mall.admin.service.PortalProductQueryService;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.product.ProductCategoryResponse;
import cn.fango.mall.common.product.ProductDetailResponse;
import cn.fango.mall.common.product.ProductSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提供给商城门户的内部商品查询接口。
 */
@RestController
@RequestMapping("/internal/portal")
public class InternalPortalProductController {

    private final PortalProductQueryService portalProductQueryService;

    /**
     * 创建内部商品查询接口。
     *
     * @param portalProductQueryService 门户商品查询服务
     */
    public InternalPortalProductController(PortalProductQueryService portalProductQueryService) {
        this.portalProductQueryService = portalProductQueryService;
    }

    /**
     * 查询可展示商品分类。
     *
     * @return 统一响应中的商品分类列表
     */
    @GetMapping("/categories")
    public CommonResult<List<ProductCategoryResponse>> listVisibleCategories() {
        List<ProductCategoryResponse> categories =
                portalProductQueryService.listVisibleCategories();

        return CommonResult.success(categories);
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
        List<ProductSummaryResponse> products =
                portalProductQueryService.listPublishedProducts(categoryId);

        return CommonResult.success(products);
    }

    /**
     * 查询可展示商品详情。
     *
     * @param productId 商品主键
     * @return 统一响应中的商品详情
     */
    @GetMapping("/products/{productId}")
    public CommonResult<ProductDetailResponse> getPublishedProductDetail(
            @PathVariable Long productId
    ) {
        ProductDetailResponse product =
                portalProductQueryService.getPublishedProductDetail(productId);

        return CommonResult.success(product);
    }
}
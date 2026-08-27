package cn.fango.mall.portal.client;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.product.ProductCategoryResponse;
import cn.fango.mall.common.product.ProductDetailResponse;
import cn.fango.mall.common.product.ProductSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 商城门户调用后台商品内部查询接口的 Feign 客户端。
 */
@FeignClient(
        name = "mall-admin",
        contextId = "portalProductClient",
        configuration = PortalInternalFeignConfig.class
)
public interface PortalProductClient {

    /**
     * 查询可展示商品分类。
     *
     * @return 统一响应中的商品分类列表
     */
    @GetMapping("/internal/portal/categories")
    CommonResult<List<ProductCategoryResponse>> listVisibleCategories();

    /**
     * 查询可展示商品。
     *
     * @param categoryId 商品分类主键；传 null 时查询全部分类
     * @return 统一响应中的商品列表
     */
    @GetMapping("/internal/portal/products")
    CommonResult<List<ProductSummaryResponse>> listPublishedProducts(
            @RequestParam(value = "categoryId", required = false) Long categoryId
    );

    /**
     * 查询可展示商品详情。
     *
     * @param productId 商品主键
     * @return 统一响应中的商品详情
     */
    @GetMapping("/internal/portal/products/{productId}")
    CommonResult<ProductDetailResponse> getPublishedProductDetail(
            @PathVariable("productId") Long productId
    );
}
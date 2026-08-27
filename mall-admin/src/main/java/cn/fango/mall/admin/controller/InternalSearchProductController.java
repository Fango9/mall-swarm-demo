package cn.fango.mall.admin.controller;

import cn.fango.mall.admin.service.ProductSearchSourceQueryService;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.search.ProductSearchSourceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提供给商品搜索服务的内部索引源数据接口。
 */
@RestController
@RequestMapping("/internal/search/products")
public class InternalSearchProductController {

    /**
     * 商品搜索索引源数据查询服务。
     */
    private final ProductSearchSourceQueryService productSearchSourceQueryService;

    /**
     * 创建商品搜索内部接口。
     *
     * @param productSearchSourceQueryService 商品搜索索引源数据查询服务
     */
    public InternalSearchProductController(ProductSearchSourceQueryService productSearchSourceQueryService) {
        this.productSearchSourceQueryService = productSearchSourceQueryService;
    }

    /**
     * 查询当前全部可写入搜索索引的商品源数据。
     *
     * @return 统一响应中的全部可索引商品源数据
     */
    @GetMapping
    public CommonResult<List<ProductSearchSourceResponse>> listPublishedProductsForIndex() {
        List<ProductSearchSourceResponse> products =
                productSearchSourceQueryService.listPublishedProductsForIndex();

        return CommonResult.success(products);
    }

    /**
     * 查询指定商品当前可写入搜索索引的源数据。
     *
     * @param productId 商品主键
     * @return 统一响应中的可索引商品源数据；不可索引时 data 为 {@code null}
     */
    @GetMapping("/{productId}")
    public CommonResult<ProductSearchSourceResponse> getPublishedProductForIndex(
            @PathVariable Long productId
    ) {
        ProductSearchSourceResponse product =
                productSearchSourceQueryService.getPublishedProductForIndex(productId);

        return CommonResult.success(product);
    }
}
package cn.fango.mall.search.controller;

import cn.fango.mall.common.api.CommonPage;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.search.ProductSearchResponse;
import cn.fango.mall.search.service.ProductSearchQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供给 Portal 的商品搜索内部查询接口。
 */
@RestController
@RequestMapping("/internal/search/products")
public class InternalProductSearchController {

    private final ProductSearchQueryService productSearchQueryService;

    /**
     * 创建商品搜索内部查询接口。
     *
     * @param productSearchQueryService 商品关键词搜索服务
     */
    public InternalProductSearchController(ProductSearchQueryService productSearchQueryService) {
        this.productSearchQueryService = productSearchQueryService;
    }

    /**
     * 按关键词分页搜索商品。
     *
     * @param keyword 商品关键词
     * @param pageNum 页码，从 1 开始；不传时默认为 1
     * @param pageSize 每页数量；不传时默认为 10
     * @return 统一响应中的商品搜索结果分页数据
     */
    @GetMapping
    public CommonResult<CommonPage<ProductSearchResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        CommonPage<ProductSearchResponse> result =
                productSearchQueryService.searchProducts(keyword, pageNum, pageSize);

        return CommonResult.success(result);
    }
}
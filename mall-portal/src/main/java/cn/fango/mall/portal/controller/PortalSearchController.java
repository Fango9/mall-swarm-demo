package cn.fango.mall.portal.controller;

import cn.fango.mall.common.api.CommonPage;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.search.ProductSearchResponse;
import cn.fango.mall.portal.client.PortalSearchClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城门户公开商品搜索接口。
 */
@RestController
@RequestMapping("/portal/search")
public class PortalSearchController {

    private final PortalSearchClient portalSearchClient;

    /**
     * 创建商城门户公开商品搜索接口。
     *
     * @param portalSearchClient 商品搜索内部查询 Feign 客户端
     */
    public PortalSearchController(PortalSearchClient portalSearchClient) {
        this.portalSearchClient = portalSearchClient;
    }

    /**
     * 按关键词分页搜索商品。
     *
     * @param keyword 商品关键词
     * @param pageNum 页码；不传时默认为 1
     * @param pageSize 每页数量；不传时默认为 10
     * @return 统一响应中的商品搜索结果分页数据
     */
    @GetMapping("/products")
    public CommonResult<CommonPage<ProductSearchResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return portalSearchClient.searchProducts(keyword, pageNum, pageSize);
    }
}
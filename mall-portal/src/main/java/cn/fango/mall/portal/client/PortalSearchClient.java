package cn.fango.mall.portal.client;

import cn.fango.mall.common.api.CommonPage;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.search.ProductSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商城门户调用商品搜索内部查询接口的 Feign 客户端。
 */
@FeignClient(
        name = "mall-search",
        contextId = "portalSearchClient",
        configuration = PortalInternalFeignConfig.class
)
public interface PortalSearchClient {

    /**
     * 按关键词分页搜索商品。
     *
     * @param keyword 商品关键词
     * @param pageNum 页码；为空时由 mall-search 使用默认值
     * @param pageSize 每页数量；为空时由 mall-search 使用默认值
     * @return 统一响应中的商品搜索结果分页数据
     */
    @GetMapping("/internal/search/products")
    CommonResult<CommonPage<ProductSearchResponse>> searchProducts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize
    );
}
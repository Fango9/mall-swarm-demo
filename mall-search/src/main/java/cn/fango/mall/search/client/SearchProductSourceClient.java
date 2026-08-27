package cn.fango.mall.search.client;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.search.ProductSearchSourceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 商品搜索服务调用 Admin 索引源数据接口的 Feign 客户端。
 */
@FeignClient(
        name = "mall-admin",
        contextId = "searchProductSourceClient",
        configuration = SearchAdminFeignConfig.class
)
public interface SearchProductSourceClient {

    /**
     * 查询指定商品当前可写入搜索索引的源数据。
     *
     * @param productId 商品主键
     * @return 统一响应中的可索引商品源数据；不可索引时 data 为 {@code null}
     */
    @GetMapping("/internal/search/products/{productId}")
    CommonResult<ProductSearchSourceResponse> getPublishedProductForIndex(
            @PathVariable("productId") Long productId
    );

    /**
     * 查询当前全部可写入搜索索引的商品源数据。
     *
     * @return 统一响应中的全部可索引商品源数据
     */
    @GetMapping("/internal/search/products")
    CommonResult<List<ProductSearchSourceResponse>> listPublishedProductsForIndex();

}
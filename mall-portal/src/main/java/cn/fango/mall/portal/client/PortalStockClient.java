package cn.fango.mall.portal.client;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.stock.StockReleaseRequest;
import cn.fango.mall.common.stock.StockReservationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 商城门户调用后台库存内部接口的 Feign 客户端。
 */
@FeignClient(
        name = "mall-admin",
        contextId = "portalStockClient",
        configuration = PortalAdminFeignConfig.class
)
public interface PortalStockClient {

    /**
     * 按订单编号预占多个 SKU 库存。
     *
     * @param request 包含订单编号和 SKU 明细的库存预占请求
     * @return 统一响应中的预占结果
     */
    @PostMapping("/internal/portal/stocks/reservations")
    CommonResult<Boolean> reserveStock(
            @RequestBody StockReservationRequest request
    );

    /**
     * 按订单编号释放全部已预占 SKU 库存。
     *
     * @param request 包含订单编号的库存释放请求
     * @return 统一响应中的释放结果
     */
    @PostMapping("/internal/portal/stocks/releases")
    CommonResult<Boolean> releaseStock(
            @RequestBody StockReleaseRequest request
    );
}
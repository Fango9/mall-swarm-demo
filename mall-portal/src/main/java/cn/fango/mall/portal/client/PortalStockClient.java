package cn.fango.mall.portal.client;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.stock.HotStockReservationAcceptResult;
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
        configuration = PortalInternalFeignConfig.class
)
public interface PortalStockClient {

    /**
     * 按订单编号预占多个 SKU 库存。
     *
     * @param request 包含订单编号和 SKU 明细的库存预占请求
     * @return 统一响应中的预占结果
     */
    @PostMapping("/internal/portal/stocks/reservations")
    CommonResult<Boolean> reserveStock(@RequestBody StockReservationRequest request);

    /**
     * 尝试由 Admin 的 Redis Lua 热点预占用链路受理库存请求。
     *
     * <p>返回 {@code NOT_HOT} 时，Portal 应改走既有同步 MySQL 预占接口；
     * 返回 {@code ACCEPTED} 或 {@code ALREADY_ACCEPTED} 时，不得再调用
     * 同步预占接口。</p>
     *
     * @param request 包含预占用编号和 SKU 明细的库存预占用请求
     * @return 统一响应中的热点预占用受理结果
     */
    @PostMapping("/internal/portal/stocks/hot-reservations")
    CommonResult<HotStockReservationAcceptResult> acceptHotReservation(
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

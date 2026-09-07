package cn.fango.mall.admin.controller;

import cn.fango.mall.admin.service.HotStockReservationService;
import cn.fango.mall.admin.service.StockReservationService;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.stock.HotStockReservationAcceptResult;
import cn.fango.mall.common.stock.StockReleaseRequest;
import cn.fango.mall.common.stock.StockReservationRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供给商城门户的内部库存预占接口。
 */
@RestController
@RequestMapping("/internal/portal/stocks")
public class InternalPortalStockController {

    /**
     * SKU 库存预占服务。
     */
    private final StockReservationService stockReservationService;

    /**
     * 热点 SKU Redis 原子预占用服务。
     */
    private final HotStockReservationService hotStockReservationService;

    /**
     * 创建商城门户内部库存接口。
     *
     * @param stockReservationService SKU 库存预占服务
     * @param hotStockReservationService 热点 SKU Redis 原子预占用服务
     */
    public InternalPortalStockController(
            StockReservationService stockReservationService,
            HotStockReservationService hotStockReservationService
    ) {
        this.stockReservationService = stockReservationService;
        this.hotStockReservationService = hotStockReservationService;
    }

    /**
     * 尝试以 Redis Lua 原子方式受理全热点 SKU 库存预占用。
     *
     * <p>返回 {@code NOT_HOT} 时未访问 Redis，Portal 应继续走既有
     * MySQL 同步预占路径；其余成功结果表示 Redis 已可靠受理预占用。</p>
     *
     * @param request 包含预占用编号和 SKU 明细的库存预占用请求
     * @return 统一响应中的热点预占用受理结果
     */
    @PostMapping("/hot-reservations")
    public CommonResult<HotStockReservationAcceptResult>
    acceptHotReservation(@RequestBody StockReservationRequest request) {
        HotStockReservationAcceptResult result = hotStockReservationService.accept(request);

        return CommonResult.success(result);
    }

    /**
     * 按订单编号预占多个 SKU 库存。
     *
     * @param request 包含订单编号和 SKU 明细的库存预占请求
     * @return 统一响应中的预占结果
     */
    @PostMapping("/reservations")
    public CommonResult<Boolean> reserveStock(
            @RequestBody StockReservationRequest request
    ) {
        boolean reserved = stockReservationService.reserveStock(request);
        return CommonResult.success(reserved);
    }

    /**
     * 按订单编号释放已预占的全部 SKU 库存。
     *
     * @param request 包含订单编号的库存释放请求
     * @return 统一响应中的释放结果
     */
    @PostMapping("/releases")
    public CommonResult<Boolean> releaseStock(@RequestBody StockReleaseRequest request) {
        boolean released = stockReservationService.releaseStock(request);
        return CommonResult.success(released);
    }
}

package cn.fango.mall.admin.service.support;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.admin.config.HotSkuStockProperties;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.stock.StockReservationItem;
import cn.fango.mall.common.stock.StockReservationRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 热点库存预占用请求的校验与热点范围判定组件。
 *
 * <p>只有全部 SKU 都在热点配置中，才允许进入 Redis Lua 路径；
 * 混合购物车保持既有 MySQL 同步预占逻辑，避免一张订单出现两套库存账本。</p>
 */
@Component
public class HotStockReservationRequestValidator {

    /**
     * 当前热点 SKU 配置。
     */
    private final HotSkuStockProperties hotSkuStockProperties;

    /**
     * 创建热点库存预占用请求校验组件。
     *
     * @param hotSkuStockProperties 当前热点 SKU 配置
     */
    public HotStockReservationRequestValidator(
            HotSkuStockProperties hotSkuStockProperties
    ) {
        this.hotSkuStockProperties = hotSkuStockProperties;
    }

    /**
     * 校验库存预占用请求的公共字段和明细。
     *
     * @param request 待校验的库存预占用请求
     */
    public void validate(StockReservationRequest request) {
        if (request == null
                || request.reservationNo() == null
                || request.reservationNo().isBlank()) {
            throw new ApiException(
                    StockReservationErrorCode.RESERVATION_NO_REQUIRED
            );
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new ApiException(
                    StockReservationErrorCode.RESERVATION_ITEMS_REQUIRED
            );
        }

        Set<Long> skuIds = new HashSet<>();

        for (StockReservationItem item : request.items()) {
            if (item == null
                    || item.skuId() == null
                    || item.skuId() <= 0
                    || item.quantity() == null
                    || item.quantity() <= 0) {
                throw new ApiException(
                        StockReservationErrorCode.RESERVATION_ITEM_INVALID
                );
            }

            if (!skuIds.add(item.skuId())) {
                throw new ApiException(
                        StockReservationErrorCode.RESERVATION_REQUEST_CONFLICT
                );
            }
        }
    }

    /**
     * 判断请求是否可以进入热点 Redis 预占用链路。
     *
     * @param request 已完成公共字段校验的库存预占用请求
     * @return 功能启用且所有 SKU 都已配置为热点时返回 {@code true}
     */
    public boolean isAllHotSku(StockReservationRequest request) {
        if (!hotSkuStockProperties.isEnabled()) {
            return false;
        }

        for (StockReservationItem item : request.items()) {
            if (!hotSkuStockProperties.getSkuIds().contains(item.skuId())) {
                return false;
            }
        }

        return true;
    }
}

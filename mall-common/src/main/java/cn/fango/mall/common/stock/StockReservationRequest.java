package cn.fango.mall.common.stock;

import java.util.List;

/**
 * SKU 库存预占请求。
 *
 * @param reservationNo 预占编号，使用订单编号
 * @param items 需要预占的 SKU 列表
 */
public record StockReservationRequest(
        String reservationNo,
        List<StockReservationItem> items
) {
}
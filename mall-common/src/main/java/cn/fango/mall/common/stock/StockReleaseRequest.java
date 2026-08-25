package cn.fango.mall.common.stock;

/**
 * SKU 库存释放请求。
 *
 * @param reservationNo 待释放预占的订单编号
 */
public record StockReleaseRequest(
        String reservationNo
) {
}
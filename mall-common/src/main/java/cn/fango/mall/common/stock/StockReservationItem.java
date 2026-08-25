package cn.fango.mall.common.stock;

/**
 * 单个 SKU 的库存预占项。
 *
 * @param skuId SKU 主键
 * @param quantity 预占数量
 */
public record StockReservationItem(
        Long skuId,
        Integer quantity
) {
}
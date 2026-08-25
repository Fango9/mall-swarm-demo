package cn.fango.mall.admin.api;

/**
 * SKU 库存预占状态。
 */
public enum StockReservationStatus {

    /** 库存已预占，等待后续支付或补偿。 */
    LOCKED,

    /** 库存预占已释放。 */
    RELEASED

}
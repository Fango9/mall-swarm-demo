package cn.fango.mall.admin.api;

/**
 * SKU 库存预占状态。
 */
public enum StockReservationStatus {

    /** 库存已预占，等待订单创建事件确认或超时释放。 */
    LOCKED,

    /** Portal 已创建订单，库存预占已确认，不再允许超时释放。 */
    CONFIRMED,

    /** 库存预占已释放，锁定库存已归还。 */
    RELEASED
}
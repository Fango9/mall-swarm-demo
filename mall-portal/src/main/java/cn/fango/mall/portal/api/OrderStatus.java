package cn.fango.mall.portal.api;

/**
 * 商城订单状态。
 */
public enum OrderStatus {

    /**
     * 订单已创建，等待支付。
     */
    PENDING_PAYMENT,

    /**
     * Redis 已受理库存预占用，等待 RabbitMQ 消费者异步落库确认。
     */
    PENDING_STOCK,

    /**
     * 库存预占用落库失败或超时释放，订单不能继续支付。
     */
    STOCK_FAILED

}

package cn.fango.mall.portal.api;

/**
 * 事务外盒事件发布状态。
 */
public enum OutboxEventStatus {

    /** 事件已随本地事务提交，等待发布。 */
    PENDING,

    /** RabbitMQ 已确认接收事件。 */
    PUBLISHED
}
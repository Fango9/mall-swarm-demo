package cn.fango.mall.common.messaging;

/**
 * 订单事件 RabbitMQ 拓扑常量。
 */
public final class OrderMessageConstants {

    /** 订单事件交换机。 */
    public static final String ORDER_EXCHANGE = "mall.order.exchange";

    /** 订单创建事件路由键。 */
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    /** 订单创建事件主队列。 */
    public static final String ORDER_CREATED_QUEUE =
            "mall.order.created.queue";

    /** 订单事件死信交换机。 */
    public static final String ORDER_DEAD_LETTER_EXCHANGE =
            "mall.order.dlx";

    /** 订单创建事件死信路由键。 */
    public static final String ORDER_CREATED_DEAD_LETTER_ROUTING_KEY =
            "order.created.dead";

    /** 订单创建事件死信队列。 */
    public static final String ORDER_CREATED_DEAD_LETTER_QUEUE =
            "mall.order.created.dlq";

    private OrderMessageConstants() {
    }
}
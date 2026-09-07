package cn.fango.mall.common.messaging;

/**
 * 热点库存预占用订单确认事件的 RabbitMQ 拓扑常量。
 *
 * <p>该拓扑仅承载 Portal 订单本地事务提交后的确认事件，不承载 Redis Stream
 * 预占用事件，也不复用普通订单创建事件队列。</p>
 */
public final class HotStockOrderMessageConstants {

    /**
     * 热点订单确认事件交换机。
     */
    public static final String EXCHANGE = "mall.hot-stock.order.exchange";

    /**
     * 热点订单确认事件路由键。
     */
    public static final String ROUTING_KEY = "hot-stock.order.created";

    /**
     * 热点订单确认主队列。
     */
    public static final String QUEUE = "mall.hot-stock.order.created.queue";

    /**
     * 热点订单确认死信交换机。
     */
    public static final String DEAD_LETTER_EXCHANGE = "mall.hot-stock.order.dlx";

    /**
     * 热点订单确认死信路由键。
     */
    public static final String DEAD_LETTER_ROUTING_KEY = "hot-stock.order.created.dead";

    /**
     * 热点订单确认死信队列。
     */
    public static final String DEAD_LETTER_QUEUE = "mall.hot-stock.order.created.dlq";

    /**
     * 热点订单确认延迟重试交换机。
     */
    public static final String RETRY_EXCHANGE = "mall.hot-stock.order.retry.exchange";

    /**
     * 热点订单确认延迟重试路由键。
     */
    public static final String RETRY_ROUTING_KEY = "hot-stock.order.retry";

    /**
     * 热点订单确认延迟重试队列。
     */
    public static final String RETRY_QUEUE = "mall.hot-stock.order.retry.queue";

    /**
     * 热点库存预占用已确认事件路由键。
     */
    public static final String CONFIRMED_ROUTING_KEY = "hot-stock.order.confirmed";

    /**
     * 热点库存预占用已确认事件队列。
     */
    public static final String CONFIRMED_QUEUE = "mall.hot-stock.order.confirmed.queue";

    /**
     * 热点库存预占用已确认事件死信交换机。
     */
    public static final String CONFIRMED_DEAD_LETTER_EXCHANGE = "mall.hot-stock.order.confirmed.dlx";

    /**
     * 热点库存预占用已确认事件死信路由键。
     */
    public static final String CONFIRMED_DEAD_LETTER_ROUTING_KEY = "hot-stock.order.confirmed.dead";

    /**
     * 热点库存预占用已确认事件死信队列。
     */
    public static final String CONFIRMED_DEAD_LETTER_QUEUE = "mall.hot-stock.order.confirmed.dlq";

    /**
     * 热点库存预占用已确认事件延迟重试交换机。
     */
    public static final String CONFIRMED_RETRY_EXCHANGE = "mall.hot-stock.order.confirmed.retry.exchange";

    /**
     * 热点库存预占用已确认事件延迟重试路由键。
     */
    public static final String CONFIRMED_RETRY_ROUTING_KEY = "hot-stock.order.confirmed.retry";

    /**
     * 热点库存预占用已确认事件延迟重试队列。
     */
    public static final String CONFIRMED_RETRY_QUEUE = "mall.hot-stock.order.confirmed.retry.queue";

    /**
     * 热点库存预占用最终失败事件路由键。
     */
    public static final String FAILED_ROUTING_KEY = "hot-stock.order.failed";

    /**
     * 热点库存预占用最终失败事件队列。
     */
    public static final String FAILED_QUEUE = "mall.hot-stock.order.failed.queue";

    /**
     * 热点库存预占用最终失败事件死信交换机。
     */
    public static final String FAILED_DEAD_LETTER_EXCHANGE = "mall.hot-stock.order.failed.dlx";

    /**
     * 热点库存预占用最终失败事件死信路由键。
     */
    public static final String FAILED_DEAD_LETTER_ROUTING_KEY = "hot-stock.order.failed.dead";

    /**
     * 热点库存预占用最终失败事件死信队列。
     */
    public static final String FAILED_DEAD_LETTER_QUEUE = "mall.hot-stock.order.failed.dlq";

    /**
     * 热点库存预占用最终失败事件延迟重试交换机。
     */
    public static final String FAILED_RETRY_EXCHANGE = "mall.hot-stock.order.failed.retry.exchange";

    /**
     * 热点库存预占用最终失败事件延迟重试路由键。
     */
    public static final String FAILED_RETRY_ROUTING_KEY = "hot-stock.order.failed.retry";

    /**
     * 热点库存预占用最终失败事件延迟重试队列。
     */
    public static final String FAILED_RETRY_QUEUE = "mall.hot-stock.order.failed.retry.queue";

    private HotStockOrderMessageConstants() {
    }
}

package cn.fango.mall.common.messaging;

/**
 * 热点库存预占用 RabbitMQ 拓扑常量。
 *
 * <p>该拓扑只承载 Redis Stream Relay 产生的预占用事件，不能复用旧的
 * {@code ORDER_CREATED} 队列；两者的幂等键和状态转换语义不同。</p>
 */
public final class HotStockReservationMessageConstants {

    /**
     * 热点库存预占用直连交换机。
     */
    public static final String EXCHANGE =
            "mall.hot-stock.reservation.exchange";

    /**
     * 热点库存预占用事件路由键。
     */
    public static final String ROUTING_KEY =
            "hot-stock.reservation.accepted";

    /**
     * 热点库存预占用主队列。
     */
    public static final String QUEUE =
            "mall.hot-stock.reservation.queue";

    /**
     * 热点库存预占用死信交换机。
     */
    public static final String DEAD_LETTER_EXCHANGE =
            "mall.hot-stock.reservation.dlx";

    /**
     * 热点库存预占用死信路由键。
     */
    public static final String DEAD_LETTER_ROUTING_KEY =
            "hot-stock.reservation.dead";

    /**
     * 热点库存预占用死信队列。
     */
    public static final String DEAD_LETTER_QUEUE =
            "mall.hot-stock.reservation.dlq";

    /**
     * 热点库存预占用死信补偿延迟重试交换机。
     */
    public static final String DEAD_LETTER_RETRY_EXCHANGE =
            "mall.hot-stock.reservation.retry.exchange";

    /**
     * 热点库存预占用死信补偿延迟重试路由键。
     */
    public static final String DEAD_LETTER_RETRY_ROUTING_KEY =
            "hot-stock.reservation.retry";

    /**
     * 热点库存预占用死信补偿延迟重试队列。
     */
    public static final String DEAD_LETTER_RETRY_QUEUE =
            "mall.hot-stock.reservation.retry.queue";

    /**
     * 工具类不允许创建实例。
     */
    private HotStockReservationMessageConstants() {
    }
}

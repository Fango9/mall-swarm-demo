package cn.fango.mall.common.messaging;

/**
 * 商品搜索索引消费者的 RabbitMQ 拓扑常量。
 *
 * <p>搜索服务复用已有商品变更交换机与路由键，
 * 但拥有独立主队列和独立死信队列。</p>
 */
public final class ProductSearchMessageConstants {

    /**
     * 搜索服务消费商品变更事件的主队列。
     */
    public static final String PRODUCT_CHANGED_QUEUE =
            "mall.search.product.changed.queue";

    /**
     * 搜索服务商品变更事件的死信交换机。
     */
    public static final String PRODUCT_CHANGED_DEAD_LETTER_EXCHANGE =
            "mall.search.product.changed.dlx";

    /**
     * 搜索服务商品变更事件的死信路由键。
     */
    public static final String PRODUCT_CHANGED_DEAD_LETTER_ROUTING_KEY =
            "product.changed.dead";

    /**
     * 搜索服务商品变更事件的死信队列。
     */
    public static final String PRODUCT_CHANGED_DEAD_LETTER_QUEUE =
            "mall.search.product.changed.dlq";

    /**
     * 工具类不允许创建实例。
     */
    private ProductSearchMessageConstants() {
    }
}
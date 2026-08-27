package cn.fango.mall.common.messaging;

/**
 * 商品变更缓存失效事件的 RabbitMQ 拓扑常量。
 */
public final class ProductCacheMessageConstants {

    /** 商品变更事件直连交换机。 */
    public static final String PRODUCT_CACHE_EXCHANGE = "mall.product.cache.exchange";

    /** 商品变更事件路由键。 */
    public static final String PRODUCT_CHANGED_ROUTING_KEY = "product.changed";

    /** Portal 消费商品变更事件的主队列。 */
    public static final String PRODUCT_CHANGED_QUEUE = "mall.portal.product.changed.queue";

    /** 商品变更事件死信交换机。 */
    public static final String PRODUCT_CACHE_DEAD_LETTER_EXCHANGE = "mall.product.cache.dlx";

    /** 商品变更事件死信路由键。 */
    public static final String PRODUCT_CHANGED_DEAD_LETTER_ROUTING_KEY = "product.changed.dead";

    /** 商品变更事件死信队列。 */
    public static final String PRODUCT_CHANGED_DEAD_LETTER_QUEUE = "mall.portal.product.changed.dlq";

    /**
     * 工具类不允许创建实例。
     */
    private ProductCacheMessageConstants() {
    }
}
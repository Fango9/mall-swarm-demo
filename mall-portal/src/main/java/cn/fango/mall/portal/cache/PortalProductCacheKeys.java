package cn.fango.mall.portal.cache;

/**
 * Portal 商品 Cache Aside 使用的 Redis 键定义。
 *
 * <p>所有商品浏览缓存均由 Portal 持有；Admin 只通过消息通知其失效，
 * 不直接读写这些键。</p>
 */
public final class PortalProductCacheKeys {

    private static final String PREFIX = "mall:portal:product:";
    private static final String ALL_CATEGORY_LIST_KEY = PREFIX + "categories";
    private static final String ALL_PRODUCT_LIST_KEY = PREFIX + "list:all";
    private static final String CATEGORY_PRODUCT_LIST_PREFIX = PREFIX + "list:category:";
    private static final String PRODUCT_DETAIL_PREFIX = PREFIX + "detail:";
    private static final String PRODUCT_DETAIL_LOCK_PREFIX = PREFIX + "detail:lock:";
    private static final String PRODUCT_DETAIL_NULL_PREFIX = PREFIX + "detail:null:";

    /**
     * 工具类不允许创建实例。
     */
    private PortalProductCacheKeys() {
    }

    /**
     * 获取全部可展示分类的缓存键。
     *
     * @return Redis 分类列表缓存键
     */
    public static String categoryListKey() {
        return ALL_CATEGORY_LIST_KEY;
    }

    /**
     * 获取指定分类或全部分类的商品列表缓存键。
     *
     * @param categoryId 商品分类主键；为 {@code null} 时表示全部分类
     * @return Redis 商品列表缓存键
     */
    public static String productListKey(Long categoryId) {
        if (categoryId == null) {
            return ALL_PRODUCT_LIST_KEY;
        }

        return CATEGORY_PRODUCT_LIST_PREFIX + categoryId;
    }

    /**
     * 获取全部商品列表缓存键的 SCAN 匹配模式。
     *
     * @return Redis 商品列表缓存键匹配模式
     */
    public static String productListKeyPattern() {
        return PREFIX + "list:*";
    }

    /**
     * 获取指定商品详情的缓存键。
     *
     * @param productId 商品主键
     * @return Redis 商品详情缓存键
     */
    public static String productDetailKey(Long productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }

    /**
     * 获取指定不存在商品的空值缓存键。
     *
     * @param productId 商品主键
     * @return Redis 商品详情空值缓存键
     */
    public static String productDetailNullKey(Long productId) {
        return PRODUCT_DETAIL_NULL_PREFIX + productId;
    }

    /**
     * 获取指定商品详情缓存重建时使用的互斥锁键。
     *
     * @param productId 商品主键
     * @return Redis 互斥锁键
     */
    public static String productDetailLockKey(Long productId) {
        return PRODUCT_DETAIL_LOCK_PREFIX + productId;
    }
}
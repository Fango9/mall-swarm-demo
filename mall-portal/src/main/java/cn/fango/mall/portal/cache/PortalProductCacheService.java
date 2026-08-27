package cn.fango.mall.portal.cache;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.api.ResultCode;
import cn.fango.mall.common.product.ProductCategoryResponse;
import cn.fango.mall.common.product.ProductDetailResponse;
import cn.fango.mall.common.product.ProductSummaryResponse;
import cn.fango.mall.portal.api.PortalProductErrorCode;
import cn.fango.mall.portal.client.PortalProductClient;
import cn.fango.mall.portal.config.PortalProductCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Portal 商品浏览接口的 Cache Aside 服务。
 *
 * <p>Redis 只保存 Admin 查询成功后的业务数据。Redis 缓存异常或 JSON
 * 反序列化异常不会阻断商品浏览请求，服务会回源到 Admin 查询。</p>
 */
@Service
public class PortalProductCacheService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PortalProductCacheService.class);

    /**
     * 仅当锁值仍属于当前请求时才删除锁的 Lua 脚本。
     */
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then "
                            + "return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class
            );

    /**
     * 后台商品内部查询 Feign 客户端。
     */
    private final PortalProductClient portalProductClient;

    /**
     * Redis 字符串读写模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Spring Boot 配置的 JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * Portal 商品缓存时长配置。
     */
    private final PortalProductCacheProperties cacheProperties;

    /**
     * 单次 Redis 删除操作中允许携带的最大缓存键数量。
     */
    private static final int CACHE_DELETE_BATCH_SIZE = 100;

    /**
     * 创建 Portal 商品 Cache Aside 服务。
     *
     * @param portalProductClient 后台商品内部查询 Feign 客户端
     * @param stringRedisTemplate Redis 字符串读写模板
     * @param objectMapper JSON 序列化工具
     * @param cacheProperties 商品缓存时长配置
     */
    public PortalProductCacheService(
            PortalProductClient portalProductClient,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            PortalProductCacheProperties cacheProperties
    ) {
        this.portalProductClient = portalProductClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    /**
     * 失效指定商品变更影响的 Portal 浏览缓存。
     *
     * <p>商品本身变更不会改变分类列表，因此不删除分类缓存。
     * 商品可能出现在全部商品列表及多个分类列表中，故通过 SCAN 清理全部商品列表缓存。</p>
     *
     * @param productId 发生变化的商品主键
     */
    public void invalidateProductCaches(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品变更事件缺少有效 productId");
        }

        stringRedisTemplate.delete(List.of(
                PortalProductCacheKeys.productDetailKey(productId),
                PortalProductCacheKeys.productDetailNullKey(productId)
        ));

        deleteProductListCaches();
    }

    /**
     * 使用 SCAN 分批删除全部商品列表缓存。
     *
     * <p>不能使用 Redis KEYS 命令，因为键数量较大时会阻塞 Redis 主线程。
     * SCAN 是增量遍历；即使消息重复投递，重复删除也不会影响正确性。</p>
     */
    private void deleteProductListCaches() {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(PortalProductCacheKeys.productListKeyPattern())
                .count(CACHE_DELETE_BATCH_SIZE)
                .build();
        List<String> cacheKeys = new ArrayList<>();

        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                cacheKeys.add(cursor.next());

                if (cacheKeys.size() == CACHE_DELETE_BATCH_SIZE) {
                    stringRedisTemplate.delete(cacheKeys);
                    cacheKeys.clear();
                }
            }

            if (!cacheKeys.isEmpty()) {
                stringRedisTemplate.delete(cacheKeys);
            }
        }
    }

    /**
     * 使用 Cache Aside 查询全部可展示商品分类。
     *
     * @return 统一响应中的商品分类列表
     */
    public CommonResult<List<ProductCategoryResponse>> listVisibleCategories() {
        String cacheKey = PortalProductCacheKeys.categoryListKey();
        List<ProductCategoryResponse> cachedCategories = readCategoryList(cacheKey);

        if (cachedCategories != null) {
            return CommonResult.success(cachedCategories);
        }

        CommonResult<List<ProductCategoryResponse>> result =
                portalProductClient.listVisibleCategories();

        if (result.getCode() == ResultCode.SUCCESS.getCode()
                && result.getData() != null) {
            writeCategoryList(cacheKey, result.getData());
        }

        return result;
    }

    /**
     * 使用 Cache Aside 查询指定分类或全部分类的可展示商品。
     *
     * @param categoryId 商品分类主键；为 {@code null} 时查询全部分类
     * @return 统一响应中的商品列表
     */
    public CommonResult<List<ProductSummaryResponse>> listPublishedProducts(Long categoryId) {
        String cacheKey = PortalProductCacheKeys.productListKey(categoryId);
        List<ProductSummaryResponse> cachedProducts = readProductList(cacheKey);

        if (cachedProducts != null) {
            return CommonResult.success(cachedProducts);
        }

        CommonResult<List<ProductSummaryResponse>> result =
                portalProductClient.listPublishedProducts(categoryId);

        if (result.getCode() == ResultCode.SUCCESS.getCode()
                && result.getData() != null) {
            writeProductList(cacheKey, result.getData());
        }

        return result;
    }

    /**
     * 使用 Cache Aside 查询可展示商品详情及其 SKU。
     *
     * <p>详情缓存未命中时，只有获得互斥锁的请求可以回源查询 Admin；
     * 其他请求短暂等待后重新读取 Redis，避免大量并发请求同时穿透缓存。</p>
     *
     * @param productId 商品主键
     * @return 统一响应中的商品详情
     */
    public CommonResult<ProductDetailResponse> getPublishedProductDetail(Long productId) {
        String cacheKey = PortalProductCacheKeys.productDetailKey(productId);
        ProductDetailResponse cachedProductDetail = readProductDetail(cacheKey);

        if (cachedProductDetail != null) {
            return CommonResult.success(cachedProductDetail);
        }

        if (hasProductDetailNullCache(productId)) {
            return CommonResult.failed(PortalProductErrorCode.PRODUCT_NOT_FOUND);
        }

        String lockToken = UUID.randomUUID().toString();
        Boolean lockAcquired = tryAcquireProductDetailLock(productId, lockToken);

        if (lockAcquired == null) {
            return loadProductDetailFromAdmin(productId);
        }

        // 被其他请求占用锁，等待
        if (!lockAcquired) {
            CommonResult<ProductDetailResponse> waitedResult =
                    waitForProductDetailCache(productId);

            if (waitedResult != null) {
                return waitedResult;
            }

            return CommonResult.failed(PortalProductErrorCode.PRODUCT_DETAIL_LOADING);
        }

        try {
            ProductDetailResponse recheckedProductDetail = readProductDetail(cacheKey);
            if (recheckedProductDetail != null) {
                return CommonResult.success(recheckedProductDetail);
            }

            if (hasProductDetailNullCache(productId)) {
                return CommonResult.failed(PortalProductErrorCode.PRODUCT_NOT_FOUND);
            }

            return loadProductDetailFromAdmin(productId);
        } finally {
            releaseProductDetailLock(productId, lockToken);
        }
    }

    /**
     * 回源调用 Admin 查询商品详情，并按查询结果写入正常缓存或空值缓存。
     *
     * @param productId 商品主键
     * @return Admin 返回的商品详情查询结果
     */
    private CommonResult<ProductDetailResponse> loadProductDetailFromAdmin(Long productId) {
        String cacheKey = PortalProductCacheKeys.productDetailKey(productId);
        CommonResult<ProductDetailResponse> result =
                portalProductClient.getPublishedProductDetail(productId);

        if (result.getCode() == ResultCode.SUCCESS.getCode()
                && result.getData() != null) {
            writeProductDetail(cacheKey, result.getData());
        } else if (result.getCode() == PortalProductErrorCode.PRODUCT_NOT_FOUND.getCode()) {
            writeProductDetailNullCache(productId);
        }

        return result;
    }

    /**
     * 尝试获得指定商品详情缓存重建锁。
     *
     * @param productId 商品主键
     * @param lockToken 当前请求唯一的锁值
     * @return 获得锁时返回 {@code true}；锁已被占用时返回 {@code false}；
     * Redis 不可用时返回 {@code null}
     */
    private Boolean tryAcquireProductDetailLock(Long productId, String lockToken) {
        String lockKey = PortalProductCacheKeys.productDetailLockKey(productId);

        try {
            return stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    lockToken,
                    cacheProperties.getLockTtl()
            );
        } catch (Exception exception) {
            LOGGER.warn("获取商品详情缓存重建锁失败，将直接回源查询，lockKey={}", lockKey, exception);
            return null;
        }
    }

    /**
     * 等待其他请求完成商品详情缓存重建，并在每次等待后重新读取缓存。
     *
     * @param productId 商品主键
     * @return 命中正常缓存或空值缓存时的结果；等待结束仍未命中时返回 {@code null}
     */
    private CommonResult<ProductDetailResponse> waitForProductDetailCache(Long productId) {
        String cacheKey = PortalProductCacheKeys.productDetailKey(productId);

        for (int retryIndex = 0; retryIndex < cacheProperties.getLockRetryTimes(); retryIndex++) {
            try {
                Thread.sleep(cacheProperties.getLockWait().toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                LOGGER.warn("等待商品详情缓存重建时被中断，productId={}", productId, exception);
                return null;
            }

            ProductDetailResponse cachedProductDetail = readProductDetail(cacheKey);
            if (cachedProductDetail != null) {
                return CommonResult.success(cachedProductDetail);
            }

            if (hasProductDetailNullCache(productId)) {
                return CommonResult.failed(PortalProductErrorCode.PRODUCT_NOT_FOUND);
            }
        }

        return null;
    }

    /**
     * 释放当前请求持有的商品详情缓存重建锁。
     *
     * @param productId 商品主键
     * @param lockToken 当前请求唯一的锁值
     */
    private void releaseProductDetailLock(Long productId, String lockToken) {
        String lockKey = PortalProductCacheKeys.productDetailLockKey(productId);

        try {
            stringRedisTemplate.execute(
                    RELEASE_LOCK_SCRIPT,
                    List.of(lockKey),
                    lockToken
            );
        } catch (Exception exception) {
            LOGGER.warn("释放商品详情缓存重建锁失败，lockKey={}", lockKey, exception);
        }
    }

    /**
     * 判断指定商品是否存在未找到的空值缓存。
     *
     * @param productId 商品主键
     * @return 空值缓存存在时返回 {@code true}
     */
    private boolean hasProductDetailNullCache(Long productId) {
        String nullCacheKey = PortalProductCacheKeys.productDetailNullKey(productId);

        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(nullCacheKey));
        } catch (Exception exception) {
            LOGGER.warn("读取商品详情空值缓存失败，将回源查询，cacheKey={}", nullCacheKey, exception);
            return false;
        }
    }

    /**
     * 写入指定不存在商品的空值缓存。
     *
     * @param productId 不存在的商品主键
     */
    private void writeProductDetailNullCache(Long productId) {
        String nullCacheKey = PortalProductCacheKeys.productDetailNullKey(productId);

        try {
            stringRedisTemplate.opsForValue().set(
                    nullCacheKey,
                    "1",
                    cacheProperties.getNullTtl()
            );
        } catch (Exception exception) {
            LOGGER.warn("写入商品详情空值缓存失败，cacheKey={}", nullCacheKey, exception);
        }
    }

    /**
     * 从 Redis 读取商品详情。
     *
     * @param cacheKey 商品详情缓存键
     * @return 命中时返回商品详情；未命中或缓存不可用时返回 {@code null}
     */
    private ProductDetailResponse readProductDetail(String cacheKey) {
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson == null) {
                return null;
            }

            return objectMapper.readValue(cachedJson, ProductDetailResponse.class);
        } catch (Exception exception) {
            LOGGER.warn("读取商品详情缓存失败，将回源查询，cacheKey={}", cacheKey, exception);
            return null;
        }
    }

    /**
     * 将商品详情写入 Redis。
     *
     * @param cacheKey 商品详情缓存键
     * @param productDetail Admin 查询成功的商品详情
     */
    private void writeProductDetail(String cacheKey, ProductDetailResponse productDetail) {
        try {
            String productDetailJson = objectMapper.writeValueAsString(productDetail);

            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    productDetailJson,
                    addTtlJitter(cacheProperties.getDetailTtl())
            );
        } catch (JsonProcessingException exception) {
            LOGGER.warn("序列化商品详情缓存失败，cacheKey={}", cacheKey, exception);
        } catch (Exception exception) {
            LOGGER.warn("写入商品详情缓存失败，cacheKey={}", cacheKey, exception);
        }
    }

    /**
     * 从 Redis 读取商品列表。
     *
     * @param cacheKey 商品列表缓存键
     * @return 命中时返回商品列表；未命中或缓存不可用时返回 {@code null}
     */
    private List<ProductSummaryResponse> readProductList(String cacheKey) {
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson == null) {
                return null;
            }

            return objectMapper.readValue(
                    cachedJson,
                    new TypeReference<List<ProductSummaryResponse>>() {
                    }
            );
        } catch (Exception exception) {
            LOGGER.warn("读取商品列表缓存失败，将回源查询，cacheKey={}", cacheKey, exception);
            return null;
        }
    }

    /**
     * 将商品列表写入 Redis。
     *
     * @param cacheKey 商品列表缓存键
     * @param products Admin 查询成功的商品列表
     */
    private void writeProductList(
            String cacheKey,
            List<ProductSummaryResponse> products
    ) {
        try {
            String productsJson = objectMapper.writeValueAsString(products);

            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    productsJson,
                    addTtlJitter(cacheProperties.getListTtl())
            );
        } catch (JsonProcessingException exception) {
            LOGGER.warn("序列化商品列表缓存失败，cacheKey={}", cacheKey, exception);
        } catch (Exception exception) {
            LOGGER.warn("写入商品列表缓存失败，cacheKey={}", cacheKey, exception);
        }
    }

    /**
     * 从 Redis 读取商品分类列表。
     *
     * @param cacheKey 商品分类列表缓存键
     * @return 命中时返回商品分类列表；未命中或缓存不可用时返回 {@code null}
     */
    private List<ProductCategoryResponse> readCategoryList(String cacheKey) {
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson == null) {
                return null;
            }

            return objectMapper.readValue(
                    cachedJson,
                    new TypeReference<List<ProductCategoryResponse>>() {
                    }
            );
        } catch (Exception exception) {
            LOGGER.warn("读取商品分类缓存失败，将回源查询，cacheKey={}", cacheKey, exception);
            return null;
        }
    }

    /**
     * 将商品分类列表写入 Redis。
     *
     * @param cacheKey 商品分类列表缓存键
     * @param categories Admin 查询成功的商品分类列表
     */
    private void writeCategoryList(String cacheKey, List<ProductCategoryResponse> categories) {
        try {
            String categoriesJson = objectMapper.writeValueAsString(categories);

            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    categoriesJson,
                    addTtlJitter(cacheProperties.getCategoryTtl())
            );
        } catch (JsonProcessingException exception) {
            LOGGER.warn("序列化商品分类缓存失败，cacheKey={}", cacheKey, exception);
        } catch (Exception exception) {
            LOGGER.warn("写入商品分类缓存失败，cacheKey={}", cacheKey, exception);
        }
    }

    /**
     * 为正常商品浏览缓存的基础过期时间增加随机抖动。
     *
     * <p>空值缓存和互斥锁必须使用精确 TTL，因此不会调用此方法。</p>
     *
     * @param baseTtl 配置的基础过期时间
     * @return 加入随机抖动后的过期时间
     */
    private Duration addTtlJitter(Duration baseTtl) {
        long maxJitterMillis = cacheProperties.getTtlJitter().toMillis();

        if (maxJitterMillis <= 0) {
            return baseTtl;
        }

        long jitterMillis =
                ThreadLocalRandom.current().nextLong(maxJitterMillis);

        return baseTtl.plusMillis(jitterMillis);
    }
}
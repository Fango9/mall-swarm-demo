package cn.fango.mall.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Portal 商品 Cache Aside 的缓存时长配置。
 */
@ConfigurationProperties(prefix = "mall.portal.product-cache")
public class PortalProductCacheProperties {

    private Duration categoryTtl = Duration.ofMinutes(30);
    private Duration listTtl = Duration.ofMinutes(10);
    private Duration detailTtl = Duration.ofMinutes(15);
    private Duration nullTtl = Duration.ofMinutes(1);
    private Duration lockTtl = Duration.ofSeconds(10);
    /**
     * 未获得商品详情缓存重建锁时，单次等待时长。
     */
    private Duration lockWait = Duration.ofMillis(50);

    /**
     * 未获得商品详情缓存重建锁时的最大重试次数。
     */
    private int lockRetryTimes = 3;

    /**
     * 正常商品浏览缓存额外增加的最大随机过期时长。
     */
    private Duration ttlJitter = Duration.ofMinutes(2);

    public Duration getCategoryTtl() {
        return categoryTtl;
    }

    public void setCategoryTtl(Duration categoryTtl) {
        this.categoryTtl = categoryTtl;
    }

    public Duration getListTtl() {
        return listTtl;
    }

    public void setListTtl(Duration listTtl) {
        this.listTtl = listTtl;
    }

    public Duration getDetailTtl() {
        return detailTtl;
    }

    public void setDetailTtl(Duration detailTtl) {
        this.detailTtl = detailTtl;
    }

    public Duration getNullTtl() {
        return nullTtl;
    }

    public void setNullTtl(Duration nullTtl) {
        this.nullTtl = nullTtl;
    }

    public Duration getLockTtl() {
        return lockTtl;
    }

    public void setLockTtl(Duration lockTtl) {
        this.lockTtl = lockTtl;
    }

    /**
     * 获取未获得商品详情缓存重建锁时的单次等待时长。
     *
     * @return 单次等待时长
     */
    public Duration getLockWait() {
        return lockWait;
    }

    /**
     * 设置未获得商品详情缓存重建锁时的单次等待时长。
     *
     * @param lockWait 单次等待时长
     */
    public void setLockWait(Duration lockWait) {
        this.lockWait = lockWait;
    }

    /**
     * 获取未获得商品详情缓存重建锁时的最大重试次数。
     *
     * @return 最大重试次数
     */
    public int getLockRetryTimes() {
        return lockRetryTimes;
    }

    /**
     * 设置未获得商品详情缓存重建锁时的最大重试次数。
     *
     * @param lockRetryTimes 最大重试次数
     */
    public void setLockRetryTimes(int lockRetryTimes) {
        this.lockRetryTimes = lockRetryTimes;
    }

    /**
     * 获取正常商品浏览缓存额外增加的最大随机过期时长。
     *
     * @return 最大随机过期时长
     */
    public Duration getTtlJitter() {
        return ttlJitter;
    }

    /**
     * 设置正常商品浏览缓存额外增加的最大随机过期时长。
     *
     * @param ttlJitter 最大随机过期时长
     */
    public void setTtlJitter(Duration ttlJitter) {
        this.ttlJitter = ttlJitter;
    }
}
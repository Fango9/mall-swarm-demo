package cn.fango.mall.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin 热点 SKU Redis 快速库存过滤配置。
 *
 * <p>仅配置在 {@code skuIds} 中的 SKU 才使用 Redis 快速过滤；
 * 未配置或功能关闭时，保持原有 MySQL 条件更新预占流程。</p>
 */
@Component
@ConfigurationProperties(prefix = "mall.hot-sku-stock")
public class HotSkuStockProperties {

    /**
     * 是否启用热点 SKU Redis 快速库存过滤。
     */
    private boolean enabled = false;

    /**
     * 需要使用 Redis 快速过滤的热点 SKU 主键列表。
     */
    private List<Long> skuIds = new ArrayList<>();

    /**
     * 获取是否启用热点 SKU Redis 快速库存过滤。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用热点 SKU Redis 快速库存过滤。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取热点 SKU 主键列表。
     *
     * @return 热点 SKU 主键列表
     */
    public List<Long> getSkuIds() {
        return skuIds;
    }

    /**
     * 设置热点 SKU 主键列表。
     *
     * @param skuIds 热点 SKU 主键列表
     */
    public void setSkuIds(List<Long> skuIds) {
        this.skuIds = skuIds == null ? new ArrayList<>() : new ArrayList<>(skuIds);
    }
}
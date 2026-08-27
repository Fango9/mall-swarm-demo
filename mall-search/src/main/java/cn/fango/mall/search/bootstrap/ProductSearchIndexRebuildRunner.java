package cn.fango.mall.search.bootstrap;

import cn.fango.mall.search.service.ProductSearchIndexService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 商品搜索服务启动后的 Elasticsearch 索引重建任务。
 */
@Component
@Order(1)
public class ProductSearchIndexRebuildRunner implements ApplicationRunner {

    /**
     * 商品搜索索引同步服务。
     */
    private final ProductSearchIndexService productSearchIndexService;

    /**
     * 创建商品搜索索引重建任务。
     *
     * @param productSearchIndexService 商品搜索索引同步服务
     */
    public ProductSearchIndexRebuildRunner(
            ProductSearchIndexService productSearchIndexService
    ) {
        this.productSearchIndexService = productSearchIndexService;
    }

    /**
     * 在商品搜索服务启动后重建 Elasticsearch 商品索引。
     *
     * <p>重建失败会使服务启动失败，避免服务在索引不完整时对外提供搜索结果。</p>
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        productSearchIndexService.rebuildAllProducts();
    }
}
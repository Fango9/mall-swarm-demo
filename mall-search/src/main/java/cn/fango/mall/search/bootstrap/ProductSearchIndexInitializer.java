package cn.fango.mall.search.bootstrap;

import cn.fango.mall.search.document.ProductSearchDocument;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 商品搜索索引结构初始化任务。
 *
 * <p>仅在索引不存在时，根据 {@link ProductSearchDocument} 的映射创建索引；
 * 已存在的索引不会被删除或重建。</p>
 */
@Component
@Order(0)
public class ProductSearchIndexInitializer implements ApplicationRunner {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 创建商品搜索索引结构初始化任务。
     *
     * @param elasticsearchOperations Elasticsearch 操作入口
     */
    public ProductSearchIndexInitializer(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * 创建不存在的商品搜索索引及其字段映射。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        IndexOperations indexOperations =
                elasticsearchOperations.indexOps(ProductSearchDocument.class);

        if (indexOperations.exists()) {
            return;
        }

        boolean created = indexOperations.createWithMapping();
        if (!created && !indexOperations.exists()) {
            throw new IllegalStateException("无法创建 Elasticsearch 商品搜索索引");
        }
    }
}
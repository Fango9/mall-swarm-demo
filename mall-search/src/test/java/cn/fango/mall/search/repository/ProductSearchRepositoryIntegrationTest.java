package cn.fango.mall.search.repository;

import cn.fango.mall.search.document.ProductSearchDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基于本机 Docker Elasticsearch 的商品索引仓储集成测试。
 */
@DataElasticsearchTest(properties = {
        "spring.elasticsearch.uris=http://127.0.0.1:9200",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "RUN_ELASTICSEARCH_INTEGRATION_TESTS", matches = "true")
class ProductSearchRepositoryIntegrationTest {

    private static final Long TEST_PRODUCT_ID = 9223372036854775000L;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    /**
     * 清理本测试写入的固定测试文档，不影响其他商品文档。
     */
    @AfterEach
    void cleanUp() {
        productSearchRepository.deleteById(TEST_PRODUCT_ID);
    }

    /**
     * 验证可将商品索引文档写入真实 Elasticsearch 并按 ID 读取。
     */
    @Test
    void shouldSaveAndFindProductSearchDocument() {
        ProductSearchDocument document = new ProductSearchDocument(
                TEST_PRODUCT_ID,
                100L,
                "Elasticsearch 集成测试商品",
                "ES-IT-001",
                new BigDecimal("199.90"),
                10,
                "https://example.com/product.png",
                "用于验证真实 Elasticsearch 读写"
        );

        productSearchRepository.save(document);

        ProductSearchDocument foundDocument = productSearchRepository
                .findById(TEST_PRODUCT_ID)
                .orElseThrow();

        assertThat(foundDocument.name()).isEqualTo("Elasticsearch 集成测试商品");
        assertThat(foundDocument.productSn()).isEqualTo("ES-IT-001");
        assertThat(foundDocument.price()).isEqualByComparingTo("199.90");
        assertThat(foundDocument.stock()).isEqualTo(10);
    }

    /**
     * 验证使用相同文档 ID 重复写入时，Elasticsearch 会覆盖已有文档。
     */
    @Test
    void shouldOverwriteDocumentWhenSavingSameIdTwice() {
        ProductSearchDocument firstDocument = new ProductSearchDocument(
                TEST_PRODUCT_ID,
                100L,
                "首次写入商品",
                "ES-IT-002",
                new BigDecimal("99.90"),
                5,
                "https://example.com/first.png",
                "首次写入的详情"
        );
        ProductSearchDocument updatedDocument = new ProductSearchDocument(
                TEST_PRODUCT_ID,
                100L,
                "重复写入后的商品",
                "ES-IT-002",
                new BigDecimal("199.90"),
                20,
                "https://example.com/updated.png",
                "重复写入后的详情"
        );

        productSearchRepository.save(firstDocument);
        productSearchRepository.save(updatedDocument);

        ProductSearchDocument foundDocument = productSearchRepository
                .findById(TEST_PRODUCT_ID)
                .orElseThrow();

        assertThat(foundDocument.name()).isEqualTo("重复写入后的商品");
        assertThat(foundDocument.price()).isEqualByComparingTo("199.90");
        assertThat(foundDocument.stock()).isEqualTo(20);
    }
}
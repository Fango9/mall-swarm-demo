package cn.fango.mall.search.bootstrap;

import cn.fango.mall.search.document.ProductSearchDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ProductSearchIndexInitializer} 的单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ProductSearchIndexInitializerTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private IndexOperations indexOperations;

    /**
     * 验证索引已存在时不会重复创建。
     */
    @Test
    void shouldNotCreateIndexWhenIndexAlreadyExists() {
        when(elasticsearchOperations.indexOps(ProductSearchDocument.class))
                .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(true);

        ProductSearchIndexInitializer initializer =
                new ProductSearchIndexInitializer(elasticsearchOperations);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(indexOperations, never()).createWithMapping();
    }

    /**
     * 验证索引不存在时按文档映射创建索引。
     */
    @Test
    void shouldCreateIndexWithMappingWhenIndexDoesNotExist() {
        when(elasticsearchOperations.indexOps(ProductSearchDocument.class))
                .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);
        when(indexOperations.createWithMapping()).thenReturn(true);

        ProductSearchIndexInitializer initializer =
                new ProductSearchIndexInitializer(elasticsearchOperations);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(indexOperations).createWithMapping();
    }

    /**
     * 验证索引创建失败且最终仍不存在时会阻止应用继续启动。
     */
    @Test
    void shouldThrowExceptionWhenIndexCreationFailsAndIndexStillDoesNotExist() {
        when(elasticsearchOperations.indexOps(ProductSearchDocument.class))
                .thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);
        when(indexOperations.createWithMapping()).thenReturn(false);

        ProductSearchIndexInitializer initializer =
                new ProductSearchIndexInitializer(elasticsearchOperations);

        assertThatThrownBy(() -> initializer.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("无法创建 Elasticsearch 商品搜索索引");
    }
}
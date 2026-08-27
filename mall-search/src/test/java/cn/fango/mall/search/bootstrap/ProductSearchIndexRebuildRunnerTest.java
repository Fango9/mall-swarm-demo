package cn.fango.mall.search.bootstrap;

import cn.fango.mall.search.service.ProductSearchIndexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.verify;

/**
 * {@link ProductSearchIndexRebuildRunner} 的单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ProductSearchIndexRebuildRunnerTest {

    @Mock
    private ProductSearchIndexService productSearchIndexService;

    /**
     * 验证应用启动后会触发一次全量商品索引重建。
     */
    @Test
    void shouldRebuildAllProductsWhenApplicationStarts() {
        ProductSearchIndexRebuildRunner runner =
                new ProductSearchIndexRebuildRunner(productSearchIndexService);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(productSearchIndexService).rebuildAllProducts();
    }
}
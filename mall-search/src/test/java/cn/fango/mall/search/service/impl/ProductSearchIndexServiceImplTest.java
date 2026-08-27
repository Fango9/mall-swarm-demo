package cn.fango.mall.search.service.impl;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.search.ProductSearchSourceResponse;
import cn.fango.mall.search.client.SearchProductSourceClient;
import cn.fango.mall.search.document.ProductSearchDocument;
import cn.fango.mall.search.repository.ProductSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 商品搜索索引同步服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ProductSearchIndexServiceImplTest {

    @Mock
    private SearchProductSourceClient searchProductSourceClient;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @InjectMocks
    private ProductSearchIndexServiceImpl productSearchIndexService;

    /**
     * Admin 返回可索引商品时应保存对应 Elasticsearch 文档。
     */
    @Test
    void synchronizeProductSavesDocumentWhenSourceExists() {
        ProductSearchSourceResponse source = new ProductSearchSourceResponse(
                1L,
                10L,
                "测试手机",
                "PHONE-001",
                new BigDecimal("1999.00"),
                20,
                "https://example.com/phone.jpg",
                "一台用于测试搜索索引的手机"
        );

        when(searchProductSourceClient.getPublishedProductForIndex(1L))
                .thenReturn(CommonResult.success(source));

        productSearchIndexService.synchronizeProduct(1L);

        ArgumentCaptor<ProductSearchDocument> documentCaptor =
                ArgumentCaptor.forClass(ProductSearchDocument.class);
        verify(productSearchRepository).save(documentCaptor.capture());
        verify(productSearchRepository, never()).deleteById(1L);

        ProductSearchDocument document = documentCaptor.getValue();
        assertThat(document.id()).isEqualTo(1L);
        assertThat(document.productCategoryId()).isEqualTo(10L);
        assertThat(document.name()).isEqualTo("测试手机");
        assertThat(document.productSn()).isEqualTo("PHONE-001");
        assertThat(document.price()).isEqualByComparingTo("1999.00");
        assertThat(document.stock()).isEqualTo(20);
        assertThat(document.pic()).isEqualTo("https://example.com/phone.jpg");
        assertThat(document.detailDesc()).isEqualTo("一台用于测试搜索索引的手机");
    }

    /**
     * Admin 返回空数据时应删除已有 Elasticsearch 文档。
     */
    @Test
    void synchronizeProductDeletesDocumentWhenSourceDoesNotExist() {
        when(searchProductSourceClient.getPublishedProductForIndex(1L))
                .thenReturn(CommonResult.success(null));

        productSearchIndexService.synchronizeProduct(1L);

        verify(productSearchRepository).deleteById(1L);
        verify(productSearchRepository, never()).save(any());
    }

    /**
     * Admin 返回失败响应时应抛出异常且不得修改 Elasticsearch。
     */
    @Test
    void synchronizeProductThrowsExceptionWhenSourceQueryFails() {
        when(searchProductSourceClient.getPublishedProductForIndex(1L))
                .thenReturn(CommonResult.failed("查询失败"));

        assertThatThrownBy(() -> productSearchIndexService.synchronizeProduct(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("查询商品搜索索引源数据失败");

        verify(productSearchRepository, never()).save(any());
        verify(productSearchRepository, never()).deleteById(1L);
    }

    /**
     * 全量重建时应先清空旧索引，再批量写入 Admin 返回的全部商品。
     */
    @Test
    void rebuildAllProductsDeletesOldDocumentsBeforeSavingAllSources() {
        ProductSearchSourceResponse firstSource = new ProductSearchSourceResponse(
                1L,
                10L,
                "测试手机",
                "PHONE-001",
                new BigDecimal("1999.00"),
                20,
                "https://example.com/phone.jpg",
                "手机详情"
        );
        ProductSearchSourceResponse secondSource = new ProductSearchSourceResponse(
                2L,
                20L,
                "测试耳机",
                "HEADPHONE-001",
                new BigDecimal("299.00"),
                50,
                "https://example.com/headphone.jpg",
                "耳机详情"
        );

        when(searchProductSourceClient.listPublishedProductsForIndex())
                .thenReturn(CommonResult.success(List.of(firstSource, secondSource)));

        productSearchIndexService.rebuildAllProducts();

        ProductSearchDocument firstDocument = new ProductSearchDocument(
                1L,
                10L,
                "测试手机",
                "PHONE-001",
                new BigDecimal("1999.00"),
                20,
                "https://example.com/phone.jpg",
                "手机详情"
        );
        ProductSearchDocument secondDocument = new ProductSearchDocument(
                2L,
                20L,
                "测试耳机",
                "HEADPHONE-001",
                new BigDecimal("299.00"),
                50,
                "https://example.com/headphone.jpg",
                "耳机详情"
        );

        org.mockito.InOrder inOrder = inOrder(productSearchRepository);
        inOrder.verify(productSearchRepository).deleteAll();
        inOrder.verify(productSearchRepository)
                .saveAll(List.of(firstDocument, secondDocument));
    }

    /**
     * Admin 全量索引源查询失败时不得删除或写入 Elasticsearch 索引。
     */
    @Test
    void rebuildAllProductsDoesNotChangeIndexWhenSourceQueryFails() {
        when(searchProductSourceClient.listPublishedProductsForIndex())
                .thenReturn(CommonResult.failed("查询失败"));

        assertThatThrownBy(() -> productSearchIndexService.rebuildAllProducts())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("查询全部商品搜索索引源数据失败");

        verify(productSearchRepository, never()).deleteAll();
        verify(productSearchRepository, never()).saveAll(any());
    }
}
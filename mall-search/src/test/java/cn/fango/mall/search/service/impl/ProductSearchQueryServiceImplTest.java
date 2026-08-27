package cn.fango.mall.search.service.impl;

import cn.fango.mall.common.api.CommonPage;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.search.ProductSearchResponse;
import cn.fango.mall.search.document.ProductSearchDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ProductSearchQueryServiceImpl} 的单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ProductSearchQueryServiceImplTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SearchHits<ProductSearchDocument> searchHits;

    @Mock
    private SearchHit<ProductSearchDocument> searchHit;

    /**
     * 验证 ES 命中文档会转换为公开搜索结果及正确分页信息。
     */
    @Test
    void shouldReturnMappedSearchResults() {
        ProductSearchDocument document = new ProductSearchDocument(
                1L,
                100L,
                "测试手机",
                "PHONE-001",
                new BigDecimal("1999.00"),
                10,
                "https://example.com/phone.png",
                "测试商品详情"
        );

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductSearchDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(searchHits.getTotalHits()).thenReturn(11L);
        when(searchHit.getContent()).thenReturn(document);

        ProductSearchQueryServiceImpl service =
                new ProductSearchQueryServiceImpl(elasticsearchOperations);

        CommonPage<ProductSearchResponse> result =
                service.searchProducts(" 手机 ", 2, 10);

        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(11L);
        assertThat(result.getTotalPage()).isEqualTo(2);
        assertThat(result.getList()).containsExactly(new ProductSearchResponse(
                1L,
                100L,
                "测试手机",
                "PHONE-001",
                new BigDecimal("1999.00"),
                "https://example.com/phone.png"
        ));
    }

    /**
     * 验证空关键词不会发送 Elasticsearch 查询。
     */
    @Test
    void shouldRejectBlankKeywordWithoutSearchingElasticsearch() {
        ProductSearchQueryServiceImpl service =
                new ProductSearchQueryServiceImpl(elasticsearchOperations);

        assertThatThrownBy(() -> service.searchProducts("  ", 1, 10))
                .isInstanceOf(ApiException.class)
                .hasMessage("搜索关键词不能为空");

        verifyNoInteractions(elasticsearchOperations);
    }

    /**
     * 验证非法分页参数会被拒绝。
     */
    @Test
    void shouldRejectInvalidPageSize() {
        ProductSearchQueryServiceImpl service =
                new ProductSearchQueryServiceImpl(elasticsearchOperations);

        assertThatThrownBy(() -> service.searchProducts("手机", 1, 51))
                .isInstanceOf(ApiException.class)
                .hasMessage("每页数量必须在 1 到 50 之间");

        verifyNoInteractions(elasticsearchOperations);
    }
}
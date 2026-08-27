package cn.fango.mall.search.service.impl;

import cn.fango.mall.common.api.CommonPage;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.search.ProductSearchResponse;
import cn.fango.mall.search.document.ProductSearchDocument;
import cn.fango.mall.search.service.ProductSearchQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Elasticsearch 的商品关键词搜索服务实现。
 */
@Service
public class ProductSearchQueryServiceImpl implements ProductSearchQueryService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 创建商品关键词搜索服务。
     *
     * @param elasticsearchOperations Elasticsearch 操作入口
     */
    public ProductSearchQueryServiceImpl(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * 按关键词分页搜索商品。
     *
     * <p>商品名称权重高于详情描述；结果默认按 Elasticsearch 相关性得分排序。</p>
     *
     * @param keyword 商品关键词
     * @param pageNum 页码，从 1 开始；为空时使用 1
     * @param pageSize 每页数量；为空时使用 10，最大为 50
     * @return 商品搜索结果分页数据
     */
    @Override
    public CommonPage<ProductSearchResponse> searchProducts(
            String keyword,
            Integer pageNum,
            Integer pageSize
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int normalizedPageNum = normalizePageNum(pageNum);
        int normalizedPageSize = normalizePageSize(pageSize);
        Pageable pageable = PageRequest.of(normalizedPageNum - 1, normalizedPageSize);

        NativeQuery query = NativeQuery.builder()
                .withQuery(queryBuilder -> queryBuilder.multiMatch(multiMatch -> multiMatch
                        .fields("name^3", "detailDesc")
                        .query(normalizedKeyword)))
                .withPageable(pageable)
                .build();

        SearchHits<ProductSearchDocument> searchHits =
                elasticsearchOperations.search(query, ProductSearchDocument.class);

        List<ProductSearchResponse> products = new ArrayList<>();
        for (SearchHit<ProductSearchDocument> searchHit : searchHits.getSearchHits()) {
            products.add(toResponse(searchHit.getContent()));
        }

        CommonPage<ProductSearchResponse> result = new CommonPage<>();
        result.setPageNum(normalizedPageNum);
        result.setPageSize(normalizedPageSize);
        result.setTotal(searchHits.getTotalHits());
        result.setTotalPage(calculateTotalPage(searchHits.getTotalHits(), normalizedPageSize));
        result.setList(products);
        return result;
    }

    /**
     * 校验并规范化搜索关键词。
     *
     * @param keyword 原始关键词
     * @return 去除首尾空白后的关键词
     */
    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new ApiException("搜索关键词不能为空");
        }

        return keyword.trim();
    }

    /**
     * 校验并规范化页码。
     *
     * @param pageNum 原始页码
     * @return 可用页码
     */
    private int normalizePageNum(Integer pageNum) {
        if (pageNum == null) {
            return DEFAULT_PAGE_NUM;
        }

        if (pageNum < DEFAULT_PAGE_NUM) {
            throw new ApiException("页码必须大于或等于 1");
        }

        return pageNum;
    }

    /**
     * 校验并规范化每页数量。
     *
     * @param pageSize 原始每页数量
     * @return 可用每页数量
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ApiException("每页数量必须在 1 到 50 之间");
        }

        return pageSize;
    }

    /**
     * 计算总页数。
     *
     * @param total 总记录数
     * @param pageSize 每页数量
     * @return 总页数
     */
    private int calculateTotalPage(long total, int pageSize) {
        return Math.toIntExact((total + pageSize - 1) / pageSize);
    }

    /**
     * 将 Elasticsearch 文档转换为公开搜索结果。
     *
     * @param document Elasticsearch 商品搜索文档
     * @return 公开商品搜索结果
     */
    private ProductSearchResponse toResponse(ProductSearchDocument document) {
        return new ProductSearchResponse(
                document.id(),
                document.productCategoryId(),
                document.name(),
                document.productSn(),
                document.price(),
                document.pic()
        );
    }
}
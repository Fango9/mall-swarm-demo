package cn.fango.mall.search.service;

import cn.fango.mall.common.api.CommonPage;
import cn.fango.mall.common.search.ProductSearchResponse;

/**
 * 商品关键词搜索服务。
 *
 * <p>该服务只查询 Elasticsearch 商品派生索引，
 * 不读取或写入商品 MySQL 主数据。</p>
 */
public interface ProductSearchQueryService {

    /**
     * 按关键词分页搜索商品。
     *
     * @param keyword 商品关键词
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 商品搜索结果分页数据
     */
    CommonPage<ProductSearchResponse> searchProducts(String keyword, Integer pageNum, Integer pageSize);
}
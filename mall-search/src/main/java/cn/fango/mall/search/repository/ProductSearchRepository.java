package cn.fango.mall.search.repository;

import cn.fango.mall.search.document.ProductSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 商品搜索索引数据访问接口。
 *
 * <p>该接口只访问 Elasticsearch 派生索引，
 * 不参与商品 MySQL 主数据的读写。</p>
 */
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, Long> {
}
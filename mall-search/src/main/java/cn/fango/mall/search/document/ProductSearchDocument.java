package cn.fango.mall.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * 商品搜索索引文档。
 *
 * <p>该对象是商品 MySQL 数据的派生副本，只用于 Elasticsearch 检索；
 * 不得作为商品写入模型使用。</p>
 *
 * @param id 商品主键，同时作为 Elasticsearch 文档 ID
 * @param productCategoryId 商品分类主键
 * @param name 商品名称
 * @param productSn 商品货号
 * @param price 商品最低销售价格
 * @param stock 商品总库存
 * @param pic 商品主图
 * @param detailDesc 商品详情描述
 */
@Document(indexName = ProductSearchDocument.INDEX_NAME)
public record ProductSearchDocument(
        @Id
        Long id,

        @Field(type = FieldType.Long)
        Long productCategoryId,

        @Field(type = FieldType.Text, analyzer = "standard")
        String name,

        @Field(type = FieldType.Keyword)
        String productSn,

        @Field(type = FieldType.Double)
        BigDecimal price,

        @Field(type = FieldType.Integer)
        Integer stock,

        @Field(type = FieldType.Keyword, index = false)
        String pic,

        @Field(type = FieldType.Text, analyzer = "standard")
        String detailDesc
) {

    /**
     * 商品搜索索引名称。
     */
    public static final String INDEX_NAME = "mall_product";
}
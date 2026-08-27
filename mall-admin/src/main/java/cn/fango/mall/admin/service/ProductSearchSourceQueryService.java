package cn.fango.mall.admin.service;

import cn.fango.mall.common.search.ProductSearchSourceResponse;

import java.util.List;

/**
 * 为商品搜索索引提供只读源数据的查询服务。
 */
public interface ProductSearchSourceQueryService {

    /**
     * 查询指定商品当前可写入搜索索引的源数据。
     *
     * <p>商品不存在、已删除、未上架或所属分类不可见时返回 {@code null}。</p>
     *
     * @param productId 商品主键
     * @return 可索引商品源数据；不可索引时返回 {@code null}
     */
    ProductSearchSourceResponse getPublishedProductForIndex(Long productId);

    /**
     * 查询当前全部可写入搜索索引的商品源数据。
     *
     * @return 全部可索引商品源数据，按商品主键升序排列
     */
    List<ProductSearchSourceResponse> listPublishedProductsForIndex();
}
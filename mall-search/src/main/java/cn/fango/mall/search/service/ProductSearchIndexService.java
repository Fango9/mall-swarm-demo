package cn.fango.mall.search.service;

/**
 * 商品搜索索引同步服务。
 */
public interface ProductSearchIndexService {

    /**
     * 将指定商品的当前发布状态同步到 Elasticsearch。
     *
     * <p>商品当前可索引时写入或覆盖文档；不可索引时删除已有文档。</p>
     *
     * @param productId 商品主键
     */
    void synchronizeProduct(Long productId);

    /**
     * 使用 Admin 当前全部可索引商品重建 Elasticsearch 商品索引。
     *
     * <p>该操作用于搜索服务启动后的索引恢复，
     * 不会改变任何商品 MySQL 主数据。</p>
     */
    void rebuildAllProducts();

}
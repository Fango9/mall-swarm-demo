package cn.fango.mall.admin.service;

/**
 * 在商品本地事务中写入商品变更 Outbox 事件的服务。
 */
public interface ProductOutboxEventService {

    /**
     * 为指定商品写入一条待发布的变更事件。
     *
     * <p>调用方必须处于商品或 SKU 修改的同一事务中；写入失败会抛出异常，
     * 使外层商品修改一并回滚。</p>
     *
     * @param productId 发生变化的商品主键
     */
    void recordProductChanged(Long productId);
}
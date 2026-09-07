package cn.fango.mall.admin.service;

import cn.fango.mall.common.event.HotStockReservationEvent;

import java.util.List;

/**
 * 将热点库存预占用事件持久化到 MySQL 的服务。
 *
 * <p>实现必须以 {@code reservationNo} 作为幂等键，并在同一个 MySQL 本地事务中
 * 更新 {@code pms_sku_stock.lock_stock}、写入 {@code pms_stock_reservation}。
 * 该服务不确认 RabbitMQ 消息；只有方法正常提交后，监听器才允许确认消费。</p>
 */
public interface HotStockReservationPersistenceService {

    /**
     * 幂等地持久化一条热点库存预占用。
     *
     * @param event Redis Stream Relay 投递的热点库存预占用事件
     */
    void persist(HotStockReservationEvent event);

    /**
     * 在一个 MySQL 本地事务中幂等持久化一批热点库存预占用。
     *
     * <p>实现应按 SKU 聚合本批新增预占数量，避免同一 SKU 在一个批次内执行多次
     * {@code lock_stock} 更新；任一事件失败都必须回滚整个批次。</p>
     *
     * @param events Redis Stream Relay 经 RabbitMQ 投递的一批预占用事件
     */
    void persistBatch(List<HotStockReservationEvent> events);

}

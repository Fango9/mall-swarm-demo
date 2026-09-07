package cn.fango.mall.admin.service;

import cn.fango.mall.common.stock.HotStockReservationAcceptResult;
import cn.fango.mall.common.stock.StockReservationRequest;

/**
 * 热点 SKU 的 Redis 原子预占用服务。
 *
 * <p>实现必须在一个 Lua 脚本中完成库存校验和预扣、预占用状态写入、
 * Redis Stream 事件追加及超时 ZSET 记录；本服务不得直接发送 RabbitMQ。</p>
 */
public interface HotStockReservationService {

    /**
     * 尝试受理一个全热点 SKU 的库存预占用请求。
     *
     * <p>同一 reservationNo 与相同明细重复调用必须返回
     * {@link HotStockReservationAcceptResult#ALREADY_ACCEPTED}，
     * 并且不得重复扣减 Redis 或重复写入 Stream。</p>
     *
     * @param request 包含预占用编号和 SKU 预占用明细的请求
     * @return 热点预占用受理结果
     */
    HotStockReservationAcceptResult accept(
            StockReservationRequest request
    );
}

package cn.fango.mall.portal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 热点库存预占用确认事件的 Portal 数据访问对象。
 */
@Mapper
public interface OmsHotStockOrderEventConsumerMapper {

    /**
     * 尝试写入事件消费幂等日志。
     *
     * <p>重复事件因唯一键冲突返回 0，调用方必须跳过订单状态更新。</p>
     *
     * @param eventId Admin Outbox 事件 ID
     * @param consumerName 消费者名称
     * @param orderId Portal 订单主键
     * @param orderSn 订单编号
     * @return 1 表示首次获得消费权；0 表示重复事件
     */
    int insertIgnoreConsumeLog(
            @Param("eventId") String eventId,
            @Param("consumerName") String consumerName,
            @Param("orderId") Long orderId,
            @Param("orderSn") String orderSn
    );

    /**
     * 将指定热点订单从等待库存确认推进为待支付。
     *
     * @param orderId Portal 订单主键
     * @param orderSn 订单编号
     * @return 1 表示状态推进成功；0 表示订单不存在、编号不匹配或状态已变化
     */
    int promotePendingStockOrder(
            @Param("orderId") Long orderId,
            @Param("orderSn") String orderSn
    );
}
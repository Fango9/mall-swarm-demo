package cn.fango.mall.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单事件消费的幂等与库存确认数据访问对象。
 */
@Mapper
public interface PmsOrderEventConsumerMapper {

    /**
     * 尝试写入消息消费记录。
     *
     * <p>相同事件和相同消费者重复消费时不抛异常，而是返回 0。</p>
     *
     * @param eventId Outbox 事件 UUID
     * @param consumer 消费者标识
     * @return 1 表示首次消费；0 表示已消费过
     */
    int insertIgnoreConsumeLog(
            @Param("eventId") String eventId,
            @Param("consumer") String consumer
    );

    /**
     * 原子确认指定订单编号下全部仍处于 LOCKED 的库存预占。
     *
     * <p>确认后清空 expire_at，使其不再被超时释放任务处理。</p>
     *
     * @param reservationNo 库存预占编号，即订单编号
     * @return 被确认的预占记录数
     */
    int confirmLockedReservations(
            @Param("reservationNo") String reservationNo
    );

}
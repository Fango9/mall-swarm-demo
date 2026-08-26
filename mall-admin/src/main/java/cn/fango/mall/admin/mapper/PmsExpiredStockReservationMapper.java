package cn.fango.mall.admin.mapper;

import cn.fango.mall.mbg.model.PmsStockReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 已过期库存预占的数据访问对象。
 */
@Mapper
public interface PmsExpiredStockReservationMapper {

    /**
     * 查询指定时间前已过期且仍处于 LOCKED 状态的库存预占。
     *
     * @param now 当前时间；expire_at 小于等于该时间的预占视为已过期
     * @param limit 本次最多查询的记录数
     * @return 按过期时间升序排列的已过期库存预占
     */
    List<PmsStockReservation> selectExpiredLockedReservations(
            @Param("now") Date now,
            @Param("limit") int limit
    );

    /**
     * 将一条仍处于 LOCKED 且已过期的预占原子标记为 RELEASED。
     *
     * <p>返回 0 表示该记录已被订单消息确认，或已被其他过期任务处理；
     * 调用方必须跳过库存释放。</p>
     *
     * @param reservationId 库存预占主键
     * @param now 当前时间
     * @return 1 表示本次获得释放权；0 表示不应继续释放库存
     */
    int markReleasedIfExpired(
            @Param("reservationId") Long reservationId,
            @Param("now") Date now
    );

}
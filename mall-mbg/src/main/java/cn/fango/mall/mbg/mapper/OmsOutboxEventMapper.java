package cn.fango.mall.mbg.mapper;

import cn.fango.mall.mbg.model.OmsOutboxEvent;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 事务外盒事件数据访问对象。
 */
public interface OmsOutboxEventMapper {

    /**
     * 新增一条事务外盒事件。
     *
     * @param event 待新增的事务外盒事件
     * @return 受影响行数
     */
    int insertSelective(OmsOutboxEvent event);

    /**
     * 查询当前允许发布的待发布事件。
     *
     * @param now 当前时间
     * @param limit 最大查询数量
     * @return 待发布事件列表
     */
    List<OmsOutboxEvent> selectPendingForPublish(
            @Param("now") Date now,
            @Param("limit") int limit
    );

    /**
     * 在 Publisher Confirm 成功后标记事件已发布。
     *
     * @param eventId Outbox 事件 UUID
     * @param publishedAt Broker 确认接收的时间
     * @return 受影响行数
     */
    int markPublished(
            @Param("eventId") String eventId,
            @Param("publishedAt") Date publishedAt
    );

    /**
     * 记录发布失败，并安排后续重新发布。
     *
     * @param eventId Outbox 事件 UUID
     * @param nextRetryAt 下一次允许重试的时间
     * @param lastError 最近一次失败原因
     * @return 受影响行数
     */
    int markPublishFailed(
            @Param("eventId") String eventId,
            @Param("nextRetryAt") Date nextRetryAt,
            @Param("lastError") String lastError
    );
}
package cn.fango.mall.admin.mapper;

import cn.fango.mall.mbg.model.PmsOutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 商品 Outbox 事件发布状态的数据访问对象。
 */
@Mapper
public interface PmsProductOutboxMapper {

    /**
     * 查询当前允许投递到 RabbitMQ 的待发布事件。
     *
     * @param now 当前时间
     * @param limit 单次最大查询数量
     * @return 待发布事件列表
     */
    List<PmsOutboxEvent> selectPendingForPublish(@Param("now") Date now, @Param("limit") int limit);

    /**
     * 在 RabbitMQ Publisher Confirm 成功后标记事件已发布。
     *
     * @param eventId Outbox 事件唯一标识
     * @param publishedAt Broker 确认接收时间
     * @return 更新行数
     */
    int markPublished(@Param("eventId") String eventId, @Param("publishedAt") Date publishedAt);

    /**
     * 记录一次发布失败，并安排下一次重试。
     *
     * @param eventId Outbox 事件唯一标识
     * @param nextRetryAt 下次允许发布的时间
     * @param lastError 最近一次失败原因
     * @return 更新行数
     */
    int markPublishFailed(@Param("eventId") String eventId, @Param("nextRetryAt") Date nextRetryAt, @Param("lastError") String lastError);
}
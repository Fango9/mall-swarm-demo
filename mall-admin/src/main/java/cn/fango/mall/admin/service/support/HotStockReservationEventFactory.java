package cn.fango.mall.admin.service.support;

import cn.fango.mall.admin.api.StockReservationErrorCode;
import cn.fango.mall.common.event.HotStockReservationEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.stock.StockReservationItem;
import cn.fango.mall.common.stock.StockReservationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建热点库存预占用事件及其 Redis Stream JSON 载荷。
 */
@Component
public class HotStockReservationEventFactory {

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建热点库存预占用事件工厂。
     *
     * @param objectMapper JSON 序列化工具
     */
    public HotStockReservationEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 根据已校验的库存预占用请求创建事件。
     *
     * @param request 已校验的库存预占用请求
     * @param expireAtMillis 预占用超时时间戳，单位毫秒
     * @return 热点库存预占用事件
     */
    public HotStockReservationEvent create(StockReservationRequest request, long expireAtMillis) {
        List<HotStockReservationEvent.Item> eventItems = new ArrayList<>();

        for (StockReservationItem item : request.items()) {
            eventItems.add(
                    new HotStockReservationEvent.Item(
                            item.skuId(),
                            item.quantity()
                    )
            );
        }

        return new HotStockReservationEvent(
                request.reservationNo(),
                request.reservationNo(),
                expireAtMillis,
                eventItems
        );
    }

    /**
     * 将热点库存预占用事件序列化为 Redis Stream 和 RabbitMQ 共用 JSON。
     *
     * @param event 热点库存预占用事件
     * @return JSON 字符串
     */
    public String serialize(HotStockReservationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    StockReservationErrorCode.RESERVATION_CREATE_FAILED,
                    exception
            );
        }
    }
}

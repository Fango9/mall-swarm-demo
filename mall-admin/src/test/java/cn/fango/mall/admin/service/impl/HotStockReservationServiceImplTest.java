package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.config.HotStockReservationProperties;
import cn.fango.mall.admin.redis.HotStockReservationLuaExecutor;
import cn.fango.mall.admin.service.support.HotStockReservationEventFactory;
import cn.fango.mall.admin.service.support.HotStockReservationRequestValidator;
import cn.fango.mall.common.stock.HotStockReservationAcceptResult;
import cn.fango.mall.common.stock.StockReservationItem;
import cn.fango.mall.common.stock.StockReservationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link HotStockReservationServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HotStockReservationServiceImplTest {

    /**
     * 热点库存预占用运行配置模拟对象。
     */
    @Mock
    private HotStockReservationProperties properties;

    /**
     * 请求校验与热点范围判定模拟对象。
     */
    @Mock
    private HotStockReservationRequestValidator requestValidator;

    /**
     * 预占用事件工厂模拟对象。
     */
    @Mock
    private HotStockReservationEventFactory eventFactory;

    /**
     * Redis Lua 执行器模拟对象。
     */
    @Mock
    private HotStockReservationLuaExecutor luaExecutor;

    /**
     * 被测试的热点库存预占用服务。
     */
    @InjectMocks
    private HotStockReservationServiceImpl service;

    /**
     * 非全热点订单必须直接回退，且绝不能执行 Redis Lua。
     */
    @Test
    void shouldReturnNotHotWithoutCallingRedisForMixedSkuOrder() {
        StockReservationRequest request = new StockReservationRequest(
                "O-test-mixed-sku",
                List.of(
                        new StockReservationItem(1L, 1),
                        new StockReservationItem(2L, 1)
                )
        );
        doNothing().when(requestValidator).validate(request);
        when(properties.isEnabled()).thenReturn(true);
        when(requestValidator.isAllHotSku(request)).thenReturn(false);

        HotStockReservationAcceptResult result =
                service.accept(request);

        assertEquals(
                HotStockReservationAcceptResult.NOT_HOT,
                result
        );
        verifyNoInteractions(eventFactory, luaExecutor);
    }
}

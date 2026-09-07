package cn.fango.mall.admin.support;

import cn.fango.mall.common.stock.StockReservationItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link HotStockReservationFingerprint} 单元测试。
 */
class HotStockReservationFingerprintTest {

    /**
     * 同一组 SKU 明细即使输入顺序不同，也必须得到相同指纹。
     */
    @Test
    void shouldCreateSameFingerprintForItemsInDifferentOrders() {
        List<StockReservationItem> firstItems = List.of(
                new StockReservationItem(2L, 1),
                new StockReservationItem(1L, 3)
        );
        List<StockReservationItem> secondItems = List.of(
                new StockReservationItem(1L, 3),
                new StockReservationItem(2L, 1)
        );

        String firstFingerprint =
                HotStockReservationFingerprint.create(firstItems);
        String secondFingerprint =
                HotStockReservationFingerprint.create(secondItems);

        assertEquals(firstFingerprint, secondFingerprint);
    }
}
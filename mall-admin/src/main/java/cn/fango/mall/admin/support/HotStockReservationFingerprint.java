package cn.fango.mall.admin.support;

import cn.fango.mall.common.stock.StockReservationItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * 生成热点库存预占用明细的规范化指纹。
 *
 * <p>指纹只描述 SKU 与数量，不包含 reservationNo、订单号、超时时间或消息 ID。
 * 因此同一请求的幂等重试总能得到相同指纹；相同 reservationNo 携带不同明细时，
 * 会得到不同指纹并被 Lua 拒绝。</p>
 */
public final class HotStockReservationFingerprint {

    /**
     * 工具类不允许创建实例。
     */
    private HotStockReservationFingerprint() {
    }

    /**
     * 根据 SKU 主键升序后的明细生成 SHA-256 十六进制指纹。
     *
     * @param items 本次库存预占用明细
     * @return 64 位小写十六进制 SHA-256 指纹
     */
    public static String create(List<StockReservationItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items 不能为空");
        }

        for (StockReservationItem item : items) {
            if (item == null
                    || item.skuId() == null
                    || item.skuId() <= 0
                    || item.quantity() == null
                    || item.quantity() <= 0) {
                throw new IllegalArgumentException("库存预约明细非法");
            }
        }

        List<StockReservationItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(StockReservationItem::skuId));

        StringBuilder canonicalText = new StringBuilder();

        for (StockReservationItem item : sortedItems) {
            canonicalText.append(item.skuId())
                    .append(':')
                    .append(item.quantity())
                    .append(';');
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    canonicalText.toString()
                            .getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "当前 JDK 不支持 SHA-256",
                    exception
            );
        }
    }
}

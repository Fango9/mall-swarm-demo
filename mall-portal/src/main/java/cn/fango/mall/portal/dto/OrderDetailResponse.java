package cn.fango.mall.portal.dto;

import cn.fango.mall.portal.api.OrderStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单详情响应。
 *
 * @param id 订单主键
 * @param orderSn 平台生成的订单编号
 * @param status 订单状态
 * @param totalAmount 订单总金额
 * @param createTime 订单创建时间
 * @param items 订单商品明细快照
 */
public record OrderDetailResponse(
        Long id,
        String orderSn,
        OrderStatus status,
        BigDecimal totalAmount,
        Date createTime,
        List<OrderItemResponse> items
) {
}
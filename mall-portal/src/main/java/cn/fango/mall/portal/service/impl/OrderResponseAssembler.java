package cn.fango.mall.portal.service.impl;

import cn.fango.mall.mbg.model.OmsOrder;
import cn.fango.mall.mbg.model.OmsOrderItem;
import cn.fango.mall.portal.api.OrderStatus;
import cn.fango.mall.portal.dto.OrderDetailResponse;
import cn.fango.mall.portal.dto.OrderItemResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 将订单持久化实体组装为门户订单响应。
 */
final class OrderResponseAssembler {

    /**
     * 私有构造方法，禁止创建工具类实例。
     */
    private OrderResponseAssembler() {
    }

    /**
     * 将订单主记录与订单明细记录组装为订单详情。
     *
     * @param order 订单主记录
     * @param orderItems 订单明细记录
     * @return 面向门户的订单详情响应
     */
    static OrderDetailResponse toDetailResponse(
            OmsOrder order,
            List<OmsOrderItem> orderItems
    ) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (OmsOrderItem orderItem : orderItems) {
            itemResponses.add(toItemResponse(orderItem));
        }

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderSn(),
                OrderStatus.valueOf(order.getStatus()),
                order.getTotalAmount(),
                order.getCreateTime(),
                itemResponses
        );
    }

    /**
     * 将订单明细持久化实体转换为订单明细响应。
     *
     * @param orderItem 订单明细持久化实体
     * @return 门户订单明细响应
     */
    private static OrderItemResponse toItemResponse(OmsOrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getProductPic(),
                orderItem.getProductSkuId(),
                orderItem.getProductSkuCode(),
                orderItem.getProductSkuAttrs(),
                orderItem.getProductPrice(),
                orderItem.getProductQuantity(),
                orderItem.getProductTotalAmount()
        );
    }
}
package cn.fango.mall.portal.service.impl;

import cn.fango.mall.mbg.model.OmsCartItem;
import cn.fango.mall.mbg.model.OmsOrder;
import cn.fango.mall.mbg.model.OmsOrderItem;
import cn.fango.mall.portal.api.OrderStatus;
import cn.fango.mall.portal.dto.OrderDetailResponse;
import cn.fango.mall.portal.dto.OrderItemResponse;

import java.math.BigDecimal;
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
     * 使用本次下单的购物车快照组装新建订单响应。
     *
     * <p>购物车快照也是订单明细写入的数据来源。事务成功后可以直接
     * 使用该快照生成响应，避免重新查询订单和订单明细。</p>
     *
     * @param order 已创建成功的订单主记录
     * @param cartItems 本次下单使用的购物车快照
     * @return 新创建订单的详情响应
     */
    static OrderDetailResponse toCreatedDetailResponse(
            OmsOrder order,
            List<OmsCartItem> cartItems
    ) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (OmsCartItem cartItem : cartItems) {
            BigDecimal itemTotalAmount =
                    cartItem.getPrice().multiply(
                            BigDecimal.valueOf(cartItem.getQuantity())
                    );

            OrderItemResponse itemResponse = new OrderItemResponse(
                    cartItem.getProductId(),
                    cartItem.getProductName(),
                    cartItem.getProductPic(),
                    cartItem.getProductSkuId(),
                    cartItem.getProductSkuCode(),
                    cartItem.getProductSkuAttrs(),
                    cartItem.getPrice(),
                    cartItem.getQuantity(),
                    itemTotalAmount
            );

            itemResponses.add(itemResponse);
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
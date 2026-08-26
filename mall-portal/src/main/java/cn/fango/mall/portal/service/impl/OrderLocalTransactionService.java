package cn.fango.mall.portal.service.impl;

import cn.fango.mall.common.event.OrderCreatedEvent;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.OmsCartItemMapper;
import cn.fango.mall.mbg.mapper.OmsOrderItemMapper;
import cn.fango.mall.mbg.mapper.OmsOrderMapper;
import cn.fango.mall.mbg.mapper.OmsOutboxEventMapper;
import cn.fango.mall.mbg.model.*;
import cn.fango.mall.portal.api.OrderErrorCode;
import cn.fango.mall.portal.api.OrderStatus;
import cn.fango.mall.portal.api.OutboxEventStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 订单本地数据事务服务。
 *
 * <p>远程库存调用不能纳入 MySQL 本地事务，因此库存预占由外层订单服务负责；
 * 本类只保证 Portal 自己数据库中的订单写入和购物车清理具有原子性。</p>
 */
@Service
public class OrderLocalTransactionService {

    /**
     * 购物车项数据访问对象。
     */
    private final OmsCartItemMapper omsCartItemMapper;

    /**
     * 订单主记录数据访问对象。
     */
    private final OmsOrderMapper omsOrderMapper;

    /**
     * 订单明细数据访问对象。
     */
    private final OmsOrderItemMapper omsOrderItemMapper;

    private static final String ORDER_AGGREGATE_TYPE = "ORDER";

    private static final String ORDER_CREATED_EVENT_TYPE = "ORDER_CREATED";

    /**
     * 事务外盒事件数据访问对象。
     */
    private final OmsOutboxEventMapper omsOutboxEventMapper;

    /**
     * JSON 序列化对象。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建订单本地数据事务服务。
     *
     * @param omsCartItemMapper 购物车项数据访问对象
     * @param omsOrderMapper 订单主记录数据访问对象
     * @param omsOrderItemMapper 订单明细数据访问对象
     * @param omsOutboxEventMapper 事务外盒事件数据访问对象
     * @param objectMapper JSON 序列化对象
     */
    public OrderLocalTransactionService(
            OmsCartItemMapper omsCartItemMapper,
            OmsOrderMapper omsOrderMapper,
            OmsOrderItemMapper omsOrderItemMapper,
            OmsOutboxEventMapper omsOutboxEventMapper,
            ObjectMapper objectMapper
    ) {
        this.omsCartItemMapper = omsCartItemMapper;
        this.omsOrderMapper = omsOrderMapper;
        this.omsOrderItemMapper = omsOrderItemMapper;
        this.omsOutboxEventMapper = omsOutboxEventMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 在单个本地事务中创建订单、创建订单明细并清理已结算购物车项。
     *
     * @param memberId 当前登录会员主键
     * @param idempotencyKey 本次下单幂等键
     * @param orderSn 已生成的唯一订单编号
     * @param cartItems 已确认归属于当前会员的购物车快照
     * @return 已保存的订单主记录
     */
    @Transactional
    public OmsOrder createOrder(Long memberId, String idempotencyKey, String orderSn, List<OmsCartItem> cartItems) {
        BigDecimal totalAmount = calculateTotalAmount(cartItems);

        OmsOrder order = new OmsOrder();
        order.setOrderSn(orderSn);
        order.setMemberId(memberId);
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setTotalAmount(totalAmount);

        int orderInserted = omsOrderMapper.insertSelective(order);
        if (orderInserted != 1 || order.getId() == null) {
            throw new ApiException(OrderErrorCode.ORDER_CREATE_FAILED);
        }

        for (OmsCartItem cartItem : cartItems) {
            OmsOrderItem orderItem = createOrderItem(order.getId(), cartItem);

            int orderItemInserted =
                    omsOrderItemMapper.insertSelective(orderItem);
            if (orderItemInserted != 1) {
                throw new ApiException(OrderErrorCode.ORDER_ITEM_CREATE_FAILED);
            }
        }

        createOrderCreatedOutboxEvent(order);

        clearCartItems(memberId, cartItems);

        return order;
    }

    /**
     * 为已保存订单创建待发布的订单创建事件。
     *
     * @param order 已保存且已有主键的订单
     */
    private void createOrderCreatedOutboxEvent(OmsOrder order) {
        String eventId = UUID.randomUUID().toString();

        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                eventId,
                order.getId(),
                order.getOrderSn()
        );

        OmsOutboxEvent outboxEvent = new OmsOutboxEvent();
        outboxEvent.setEventId(eventId);
        outboxEvent.setAggregateType(ORDER_AGGREGATE_TYPE);
        outboxEvent.setAggregateId(order.getId());
        outboxEvent.setEventType(ORDER_CREATED_EVENT_TYPE);
        outboxEvent.setStatus(OutboxEventStatus.PENDING.name());

        try {
            outboxEvent.setPayload(
                    objectMapper.writeValueAsString(orderCreatedEvent)
            );
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    OrderErrorCode.OUTBOX_EVENT_CREATE_FAILED,
                    exception
            );
        }

        int inserted = omsOutboxEventMapper.insertSelective(outboxEvent);
        if (inserted != 1 || outboxEvent.getId() == null) {
            throw new ApiException(OrderErrorCode.OUTBOX_EVENT_CREATE_FAILED);
        }
    }

    /**
     * 计算全部购物车快照的订单总金额。
     *
     * @param cartItems 已确认归属于当前会员的购物车快照
     * @return 订单总金额
     */
    private BigDecimal calculateTotalAmount(List<OmsCartItem> cartItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OmsCartItem cartItem : cartItems) {
            if (cartItem.getPrice() == null
                    || cartItem.getQuantity() == null
                    || cartItem.getQuantity() <= 0) {
                throw new ApiException(OrderErrorCode.ORDER_CREATE_FAILED);
            }

            BigDecimal itemAmount = cartItem.getPrice().multiply(
                    BigDecimal.valueOf(cartItem.getQuantity())
            );
            totalAmount = totalAmount.add(itemAmount);
        }

        return totalAmount;
    }

    /**
     * 将购物车快照转换为订单明细快照。
     *
     * @param orderId 订单主键
     * @param cartItem 购物车项快照
     * @return 待保存的订单明细
     */
    private OmsOrderItem createOrderItem(
            Long orderId,
            OmsCartItem cartItem
    ) {
        BigDecimal itemAmount = cartItem.getPrice().multiply(
                BigDecimal.valueOf(cartItem.getQuantity())
        );

        OmsOrderItem orderItem = new OmsOrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setProductId(cartItem.getProductId());
        orderItem.setProductName(cartItem.getProductName());
        orderItem.setProductPic(cartItem.getProductPic());
        orderItem.setProductSkuId(cartItem.getProductSkuId());
        orderItem.setProductSkuCode(cartItem.getProductSkuCode());
        orderItem.setProductSkuAttrs(cartItem.getProductSkuAttrs());
        orderItem.setProductPrice(cartItem.getPrice());
        orderItem.setProductQuantity(cartItem.getQuantity());
        orderItem.setProductTotalAmount(itemAmount);

        return orderItem;
    }

    /**
     * 清理本次已成功结算的购物车项。
     *
     * <p>删除条件同时包含会员主键和购物车项主键，避免删除其他会员的数据。</p>
     *
     * @param memberId 当前登录会员主键
     * @param cartItems 已结算的购物车项
     */
    private void clearCartItems(
            Long memberId,
            List<OmsCartItem> cartItems
    ) {
        List<Long> cartItemIds = cartItems.stream()
                .map(OmsCartItem::getId)
                .toList();

        OmsCartItemExample example = new OmsCartItemExample();
        example.createCriteria()
                .andMemberIdEqualTo(memberId)
                .andIdIn(cartItemIds);

        int deleted = omsCartItemMapper.deleteByExample(example);
        if (deleted != cartItemIds.size()) {
            throw new ApiException(OrderErrorCode.CART_ITEM_CLEAR_FAILED);
        }
    }
}
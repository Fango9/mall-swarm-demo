package cn.fango.mall.portal.service.impl;

import cn.fango.mall.common.event.HotStockOrderCreatedEvent;
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
import cn.fango.mall.portal.mapper.OmsHotStockOrderFailureEventConsumerMapper;
import cn.fango.mall.portal.performance.OrderTimingRecorder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
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

    private static final String HOT_STOCK_ORDER_CREATED_EVENT_TYPE = "HOT_STOCK_ORDER_CREATED";

    /**
     * 事务外盒事件数据访问对象。
     */
    private final OmsOutboxEventMapper omsOutboxEventMapper;

    /**
     * 热点库存失败标记数据访问对象。
     */
    private final OmsHotStockOrderFailureEventConsumerMapper
            failureEventConsumerMapper;

    /**
     * JSON 序列化对象。
     */
    private final ObjectMapper objectMapper;

    /** P5 请求细分计时器。 */
    private final OrderTimingRecorder orderTimingRecorder;

    /**
     * 创建订单本地数据事务服务。
     *
     * @param omsCartItemMapper 购物车项数据访问对象
     * @param omsOrderMapper 订单主记录数据访问对象
     * @param omsOrderItemMapper 订单明细数据访问对象
     * @param omsOutboxEventMapper 事务外盒事件数据访问对象
     * @param failureEventConsumerMapper 热点库存失败标记数据访问对象
     * @param objectMapper JSON 序列化对象
     */
    public OrderLocalTransactionService(
            OmsCartItemMapper omsCartItemMapper,
            OmsOrderMapper omsOrderMapper,
            OmsOrderItemMapper omsOrderItemMapper,
            OmsOutboxEventMapper omsOutboxEventMapper,
            OmsHotStockOrderFailureEventConsumerMapper
                    failureEventConsumerMapper,
            ObjectMapper objectMapper,
            OrderTimingRecorder orderTimingRecorder
    ) {
        this.omsCartItemMapper = omsCartItemMapper;
        this.omsOrderMapper = omsOrderMapper;
        this.omsOrderItemMapper = omsOrderItemMapper;
        this.omsOutboxEventMapper = omsOutboxEventMapper;
        this.failureEventConsumerMapper = failureEventConsumerMapper;
        this.objectMapper = objectMapper;
        this.orderTimingRecorder = orderTimingRecorder;
    }

    /**
     * 在一个短只读事务中查询幂等订单和购物车快照。
     *
     * @param memberId 当前会员主键
     * @param idempotencyKey 已标准化的幂等键
     * @param cartItemIds 本次结算的购物车项主键
     * @return 已有订单或购物车快照
     */
    @Transactional(readOnly = true)
    public CreationData loadCreationData(
            Long memberId,
            String idempotencyKey,
            List<Long> cartItemIds
    ) {
        OmsOrderExample orderExample = new OmsOrderExample();
        orderExample.createCriteria()
                .andMemberIdEqualTo(memberId)
                .andIdempotencyKeyEqualTo(idempotencyKey);

        List<OmsOrder> orders = orderTimingRecorder.recordStage(
                "idempotency_lookup",
                () -> omsOrderMapper.selectByExample(orderExample)
        );
        if (!orders.isEmpty()) {
            return new CreationData(orders.get(0), List.of());
        }

        OmsCartItemExample cartExample = new OmsCartItemExample();
        cartExample.createCriteria()
                .andMemberIdEqualTo(memberId)
                .andIdIn(cartItemIds);

        List<OmsCartItem> cartItems = orderTimingRecorder.recordStage(
                "cart_load",
                () -> omsCartItemMapper.selectByExample(cartExample)
        );
        if (cartItems.size() != cartItemIds.size()) {
            throw new ApiException(OrderErrorCode.ORDER_CART_ITEM_NOT_FOUND);
        }

        return new CreationData(null, cartItems);
    }

    /**
     * 创建订单前置读取结果。
     *
     * @param existingOrder 已有幂等订单；未命中时为 {@code null}
     * @param cartItems 购物车快照；命中已有订单时为空
     */
    public record CreationData(
            OmsOrder existingOrder,
            List<OmsCartItem> cartItems
    ) {
    }

    /**
     * 在单个本地事务中创建订单、创建订单明细并清理已结算购物车项。
     *
     * @param memberId 当前登录会员主键
     * @param idempotencyKey 本次下单幂等键
     * @param orderSn 已生成的唯一订单编号
     * @param cartItems 已确认归属于当前会员的购物车快照
     * @param hotReservationAccepted 是否已由 Redis Stream 热点预占用链路受理
     * @return 已保存的订单主记录
     */
    @Transactional
    public OmsOrder createOrder(
            Long memberId,
            String idempotencyKey,
            String orderSn,
            List<OmsCartItem> cartItems,
            boolean hotReservationAccepted
    ) {
        BigDecimal totalAmount = calculateTotalAmount(cartItems);
        boolean hotStockFailedBeforeOrderCreated = false;

        if (hotReservationAccepted) {
            // 与失败事件消费者使用同一行/间隙锁，消除“失败事件先到但订单稍后才提交”导致订单永久停在 PENDING_STOCK 的竞态。
            String failureEventId = orderTimingRecorder.recordStage(
                    "failure_event_lock_select",
                    () -> failureEventConsumerMapper
                            .selectFailureEventIdForUpdate(orderSn)
            );

            hotStockFailedBeforeOrderCreated = failureEventId != null;
        }

        // 创建 oreder 并插入
        OmsOrder order = new OmsOrder();
        order.setOrderSn(orderSn);
        order.setMemberId(memberId);
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(
                hotReservationAccepted
                        ? (hotStockFailedBeforeOrderCreated
                                ? OrderStatus.STOCK_FAILED.name()
                                : OrderStatus.PENDING_STOCK.name())
                        : OrderStatus.PENDING_PAYMENT.name()
        );
        order.setTotalAmount(totalAmount);

        /*
         * oms_order.create_time 为 DATETIME，不保存毫秒。
         * 在应用侧设置后，插入对象可以直接用于响应组装。
         */
        long currentTimeMillis = System.currentTimeMillis();
        long createTimeMillis = currentTimeMillis / 1000L * 1000L;
        order.setCreateTime(new Date(createTimeMillis));

        int orderInserted = orderTimingRecorder.recordStage(
                "order_insert",
                () -> omsOrderMapper.insertSelective(order)
        );
        if (orderInserted != 1 || order.getId() == null) {
            throw new ApiException(OrderErrorCode.ORDER_CREATE_FAILED);
        }

        // 插入订单明细
        for (OmsCartItem cartItem : cartItems) {
            OmsOrderItem orderItem = createOrderItem(order.getId(), cartItem);

            int orderItemInserted = orderTimingRecorder.recordStage(
                    "order_item_insert",
                    () -> omsOrderItemMapper.insertSelective(orderItem)
            );
            if (orderItemInserted != 1) {
                throw new ApiException(OrderErrorCode.ORDER_ITEM_CREATE_FAILED);
            }
        }

        // 热点 sku 和非全部热点 sku 走不同的路径，之后交由 outbox publish  定时扫描，发送消息到 rabbitMQ
        if (hotReservationAccepted && !hotStockFailedBeforeOrderCreated) {
            // 创建热点 sku 订单事件
            createHotStockOrderCreatedOutboxEvent(order);
        } else if (!hotReservationAccepted) {
            // 创建非全部热点 sku 订单事件
            createOrderCreatedOutboxEvent(order);
        }

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

        outboxEvent.setPayload(orderTimingRecorder.recordStage(
                "outbox_payload_serialize",
                () -> writePayload(orderCreatedEvent)
        ));

        int inserted = orderTimingRecorder.recordStage(
                "outbox_insert",
                () -> omsOutboxEventMapper.insertSelective(outboxEvent)
        );
        if (inserted != 1 || outboxEvent.getId() == null) {
            throw new ApiException(OrderErrorCode.OUTBOX_EVENT_CREATE_FAILED);
        }
    }

    /**
     * 为热点库存预占用订单创建待发布的确认事件。
     *
     * <p>该事件证明 Portal 订单本地事务已提交。它与 Redis Stream 预占用事件
     * 分属不同可靠链路，不能复用普通订单创建事件。</p>
     *
     * @param order 已保存且已有主键的热点订单
     */
    private void createHotStockOrderCreatedOutboxEvent(OmsOrder order) {
        String eventId = UUID.randomUUID().toString();

        HotStockOrderCreatedEvent event = new HotStockOrderCreatedEvent(
                        eventId,
                        order.getId(),
                        order.getOrderSn()
                );

        OmsOutboxEvent outboxEvent = new OmsOutboxEvent();
        outboxEvent.setEventId(eventId);
        outboxEvent.setAggregateType(ORDER_AGGREGATE_TYPE);
        outboxEvent.setAggregateId(order.getId());
        outboxEvent.setEventType(HOT_STOCK_ORDER_CREATED_EVENT_TYPE);
        outboxEvent.setStatus(OutboxEventStatus.PENDING.name());

        outboxEvent.setPayload(orderTimingRecorder.recordStage(
                "outbox_payload_serialize",
                () -> writePayload(event)
        ));

        int inserted = orderTimingRecorder.recordStage(
                "outbox_insert",
                () -> omsOutboxEventMapper.insertSelective(outboxEvent)
        );
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

        int deleted = orderTimingRecorder.recordStage(
                "cart_delete",
                () -> omsCartItemMapper.deleteByExample(example)
        );
        if (deleted != cartItemIds.size()) {
            throw new ApiException(OrderErrorCode.CART_ITEM_CLEAR_FAILED);
        }
    }

    /**
     * 将 Outbox 事件序列化为 JSON，并把受检异常转换为业务异常。
     *
     * @param event 待序列化的事件
     * @return JSON payload
     */
    private String writePayload(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    OrderErrorCode.OUTBOX_EVENT_CREATE_FAILED,
                    exception
            );
        }
    }
}

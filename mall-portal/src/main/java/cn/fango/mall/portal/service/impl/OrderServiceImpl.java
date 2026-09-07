package cn.fango.mall.portal.service.impl;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.api.ResultCode;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.stock.HotStockReservationAcceptResult;
import cn.fango.mall.common.stock.StockReleaseRequest;
import cn.fango.mall.common.stock.StockReservationItem;
import cn.fango.mall.common.stock.StockReservationRequest;
import cn.fango.mall.mbg.mapper.OmsOrderItemMapper;
import cn.fango.mall.mbg.mapper.OmsOrderMapper;
import cn.fango.mall.mbg.model.OmsCartItem;
import cn.fango.mall.mbg.model.OmsOrder;
import cn.fango.mall.mbg.model.OmsOrderExample;
import cn.fango.mall.mbg.model.OmsOrderItem;
import cn.fango.mall.mbg.model.OmsOrderItemExample;
import cn.fango.mall.portal.api.OrderErrorCode;
import cn.fango.mall.portal.client.PortalStockClient;
import cn.fango.mall.portal.dto.OrderCreateRequest;
import cn.fango.mall.portal.dto.OrderDetailResponse;
import cn.fango.mall.portal.service.OrderService;
import cn.fango.mall.portal.performance.OrderTimingRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 会员订单服务实现。
 *
 * <p>本类编排跨服务下单流程；库存预占由 mall-admin 独占写入，
 * Portal 只负责自己的订单与购物车数据。</p>
 */
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 订单下单流程日志记录器。
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(OrderServiceImpl.class);

    /**
     * 订单主记录数据访问对象。
     */
    private final OmsOrderMapper omsOrderMapper;

    /**
     * 订单明细数据访问对象。
     */
    private final OmsOrderItemMapper omsOrderItemMapper;

    /**
     * 后台库存内部接口客户端。
     */
    private final PortalStockClient portalStockClient;

    /**
     * Portal 本地订单事务服务。
     */
    private final OrderLocalTransactionService orderLocalTransactionService;

    /**
     * 热点 Redis 预扣和同步库存预占的耗时指标。
     */
    private final Timer stockReservationTimer;

    /**
     * Portal 本地订单事务耗时指标。
     */
    private final Timer localTransactionTimer;

    /** P5 请求细分计时器。 */
    private final OrderTimingRecorder orderTimingRecorder;

    /**
     * 创建会员订单服务。
     *
     * @param omsOrderMapper 订单主记录数据访问对象
     * @param omsOrderItemMapper 订单明细数据访问对象
     * @param portalStockClient 后台库存内部接口客户端
     * @param orderLocalTransactionService Portal 本地订单事务服务
     * @param meterRegistry 指标注册表
     */
    public OrderServiceImpl(
            OmsOrderMapper omsOrderMapper,
            OmsOrderItemMapper omsOrderItemMapper,
            PortalStockClient portalStockClient,
            OrderLocalTransactionService orderLocalTransactionService,
            MeterRegistry meterRegistry,
            OrderTimingRecorder orderTimingRecorder
    ) {
        this.omsOrderMapper = omsOrderMapper;
        this.omsOrderItemMapper = omsOrderItemMapper;
        this.portalStockClient = portalStockClient;
        this.orderLocalTransactionService = orderLocalTransactionService;
        this.stockReservationTimer = Timer.builder("p5.portal.order.stock_reservation.duration")
                .register(meterRegistry);
        this.localTransactionTimer = Timer.builder("p5.portal.order.local_transaction.duration")
                .register(meterRegistry);
        this.orderTimingRecorder = orderTimingRecorder;
    }

    /**
     * 从当前会员选中的购物车项创建待支付订单。
     *
     * <p>执行顺序为：幂等查询、购物车快照读取、库存预占、本地订单事务。
     * 库存预占已经成功但本地事务失败时，必须调用库存释放接口进行补偿。</p>
     *
     * @param memberId 当前登录会员主键
     * @param idempotencyKey HTTP 请求头 {@code Idempotency-Key} 的值
     * @param request 下单请求
     * @return 已创建或幂等命中的订单详情
     */
    @Override
    public OrderDetailResponse createOrder(Long memberId, String idempotencyKey, OrderCreateRequest request) {
        // 格式化幂等键
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        orderTimingRecorder.recordStage(
                "request_validation",
                () -> validateCreateOrderRequest(memberId, normalizedIdempotencyKey, request)
        );

        OrderLocalTransactionService.CreationData creationData =
                orderLocalTransactionService.loadCreationData(
                        memberId,
                        normalizedIdempotencyKey,
                        request.cartItemIds()
                );

        OmsOrder existingOrder = creationData.existingOrder();
        if (existingOrder != null) {
            return getOrderDetail(memberId, existingOrder.getId());
        }

        List<OmsCartItem> cartItems = creationData.cartItems();

        // 根据用户 id 和幂等键生成唯一订单编号，后续预占用也使用此编号
        String orderSn = generateOrderSn(
                memberId,
                normalizedIdempotencyKey
        );

        // 使用订单编号、skuid、数量，生成库存预占用请求，
        StockReservationRequest stockReservationRequest = orderTimingRecorder.recordStage(
                "reservation_request_build",
                () -> createStockReservationRequest(orderSn, cartItems)
        );

        // 尝试使用热点库存 sku 预占用
        Timer.Sample stockReservationSample = Timer.start();
        boolean hotReservationAccepted;
        try {
            hotReservationAccepted = orderTimingRecorder.recordStage(
                    "hot_reservation_http",
                    () -> tryAcceptHotReservation(stockReservationRequest)
            );

            if (!hotReservationAccepted) {
                // 非全部热点 sku，预占库存
                reserveStock(stockReservationRequest);
            }
        } finally {
            stockReservationSample.stop(stockReservationTimer);
        }

        OmsOrder createdOrder;
        try {
            // 构建订单
            Timer.Sample localTransactionSample = Timer.start();
            try {
                createdOrder = orderTimingRecorder.recordStage(
                        "local_transaction_call",
                        () -> orderLocalTransactionService.createOrder(
                                memberId,
                                normalizedIdempotencyKey,
                                orderSn,
                                cartItems,
                                hotReservationAccepted
                        )
                );
            } finally {
                localTransactionSample.stop(localTransactionTimer);
            }
        } catch (DuplicateKeyException exception) {
            // 按会员 id 和幂等键查询已存在的幂等订单，避免并发重复下单被唯一索引冲突拦截，但实际订单已创建未返回
            OmsOrder idempotentOrder = findOrderByIdempotencyKey(
                    memberId,
                    normalizedIdempotencyKey
            );
            if (idempotentOrder != null) {
                return getOrderDetail(memberId, idempotentOrder.getId());
            }

            if (!hotReservationAccepted) {
                // 非全部热点 sku，释放 redis 预占库存
                releaseStockAfterLocalFailure(orderSn, exception);
            }

            LOGGER.error("订单唯一索引冲突，且未查询到幂等订单，orderSn={}", orderSn, exception);
            throw new ApiException(OrderErrorCode.ORDER_CREATE_FAILED, exception);

        } catch (ApiException exception) {
            if (!hotReservationAccepted) {
                // 非全部热点 sku，释放 redis 预占库存
                releaseStockAfterLocalFailure(orderSn, exception);
            }
            throw exception;
        } catch (RuntimeException exception) {
            if (!hotReservationAccepted) {
                // 非全部热点 sku，释放 redis 预占库存
                releaseStockAfterLocalFailure(orderSn, exception);
            }

            LOGGER.error(hotReservationAccepted
                            ? "本地订单事务失败，热点预约等待超时补偿，orderSn={}"
                            : "本地订单事务失败，已执行库存释放补偿，orderSn={}",
                    orderSn,
                    exception
            );
            throw new ApiException(OrderErrorCode.ORDER_CREATE_FAILED, exception);
        }

        return OrderResponseAssembler.toCreatedDetailResponse(
                createdOrder,
                cartItems
        );
    }

    /**
     * 查询当前会员拥有的订单及其订单明细。
     *
     * @param memberId 当前登录会员主键
     * @param orderId 订单主键
     * @return 订单及其明细快照
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
        validateMemberAndOrderId(memberId, orderId);

        OmsOrder order = orderTimingRecorder.recordStage(
                "order_detail_select",
                () -> getOwnedOrder(memberId, orderId)
        );
        List<OmsOrderItem> orderItems = orderTimingRecorder.recordStage(
                "order_items_select",
                () -> listOrderItems(order.getId())
        );

        return OrderResponseAssembler.toDetailResponse(order, orderItems);
    }

    /**
     * 去除幂等键两端的空白字符，避免同一逻辑键因尾随空格失效。
     *
     * @param idempotencyKey 原始幂等键
     * @return 标准化后的幂等键；原始值为 {@code null} 时返回 {@code null}
     */
    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }

        return idempotencyKey.trim();
    }

    /**
     * 根据会员和幂等键稳定生成订单编号，同时作为热点库存预占用编号。
     *
     * <p>若 Portal 在 Redis Lua 成功后、订单本地事务前崩溃，客户端以相同
     * Idempotency-Key 重试时必须使用相同预占用编号，Lua 才能识别为幂等重试，
     * 而不是再次扣减 Redis 库存。</p>
     *
     * @param memberId 当前登录会员主键
     * @param idempotencyKey 已标准化的下单幂等键
     * @return 稳定生成的订单编号
     */
    private String generateOrderSn(
            Long memberId,
            String idempotencyKey
    ) {
        String stableInput = memberId + ":" + idempotencyKey;
        String uniquePart = UUID.nameUUIDFromBytes(
                stableInput.getBytes(StandardCharsets.UTF_8)
        ).toString().replace("-", "");

        return "O" + uniquePart;
    }

    /**
     * 调用 mall-admin 同步预占库存。
     *
     * <p>本方法只用于非热点订单；热点订单一旦 Redis 已受理，
     * 不允许再调用该方法，否则会产生重复库存预占。</p>
     *
     * @param request 已由购物车快照构造完成的库存预占用请求
     */
    private void reserveStock(StockReservationRequest request) {
        CommonResult<Boolean> result;

        try {
            result = portalStockClient.reserveStock(request);
        } catch (RuntimeException exception) {
            throw new ApiException(OrderErrorCode.STOCK_RESERVATION_FAILED);
        }

        if (result == null
                || result.getCode() != ResultCode.SUCCESS.getCode()
                || !Boolean.TRUE.equals(result.getData())) {
            throw new ApiException(OrderErrorCode.STOCK_RESERVATION_FAILED);
        }
    }

    /**
     * 根据当前购物车快照构造库存预占用请求。
     *
     * <p>热点 Redis 预占用与普通 MySQL 预占必须使用同一份 SKU/数量快照，
     * 不能在两条链路中分别构造并产生不一致的库存明细。</p>
     *
     * @param orderSn 同时作为库存预占用编号的订单编号
     * @param cartItems 本次结算的购物车快照
     * @return 库存预占用请求
     */
    private StockReservationRequest createStockReservationRequest(String orderSn, List<OmsCartItem> cartItems) {
        List<StockReservationItem> items = new ArrayList<>();

        for (OmsCartItem cartItem : cartItems) {
            items.add(
                    new StockReservationItem(
                            cartItem.getProductSkuId(),
                            cartItem.getQuantity()
                    )
            );
        }

        return new StockReservationRequest(orderSn, items);
    }

    /**
     * 尝试由 Admin 的 Redis Lua 热点预占用链路受理库存请求。
     *
     * <p>只有 {@code ACCEPTED} 和 {@code ALREADY_ACCEPTED} 返回 {@code true}；
     * {@code NOT_HOT} 返回 {@code false}，调用方才可以走既有 MySQL 预占。
     * 远程调用异常或未知结果必须失败关闭，不能擅自回退 MySQL。</p>
     *
     * @param request 已由购物车快照构造完成的库存预占用请求
     * @return Redis 已可靠受理预占用时返回 {@code true}
     */
    private boolean tryAcceptHotReservation(StockReservationRequest request) {
        CommonResult<HotStockReservationAcceptResult> result;

        try {
            result = portalStockClient.acceptHotReservation(request);
        } catch (RuntimeException exception) {
            throw new ApiException(
                    OrderErrorCode.STOCK_RESERVATION_FAILED,
                    exception
            );
        }

        if (result == null
                || result.getCode() != ResultCode.SUCCESS.getCode()
                || result.getData() == null) {
            throw new ApiException(
                    OrderErrorCode.STOCK_RESERVATION_FAILED
            );
        }

        if (result.getData()
                == HotStockReservationAcceptResult.ACCEPTED
                || result.getData()
                == HotStockReservationAcceptResult.ALREADY_ACCEPTED) {
            return true;
        }

        if (result.getData()
                == HotStockReservationAcceptResult.NOT_HOT) {
            return false;
        }

        throw new ApiException(
                OrderErrorCode.STOCK_RESERVATION_FAILED
        );
    }

    /**
     * 在本地订单事务失败后释放已预占库存。
     *
     * <p>释放失败不会覆盖最初的本地失败异常，而是作为 suppressed exception
     * 附加到原异常中，保留完整故障上下文。</p>
     *
     * @param orderSn 库存预占编号
     * @param originalException 导致本地事务失败的原始异常
     */
    private void releaseStockAfterLocalFailure(String orderSn, RuntimeException originalException) {
        try {
            releaseStock(orderSn);
        } catch (RuntimeException releaseException) {
            originalException.addSuppressed(releaseException);
        }
    }

    /**
     * 调用 mall-admin 释放指定预占编号下的全部库存。
     *
     * @param orderSn 库存预占编号
     */
    private void releaseStock(String orderSn) {
        StockReleaseRequest request = new StockReleaseRequest(orderSn);
        CommonResult<Boolean> result;

        try {
            result = portalStockClient.releaseStock(request);
        } catch (RuntimeException exception) {
            throw new ApiException(
                    OrderErrorCode.STOCK_RELEASE_COMPENSATION_FAILED
            );
        }

        if (result == null
                || result.getCode() != ResultCode.SUCCESS.getCode()
                || !Boolean.TRUE.equals(result.getData())) {
            throw new ApiException(
                    OrderErrorCode.STOCK_RELEASE_COMPENSATION_FAILED
            );
        }
    }

    /**
     * 校验创建订单所需的会员、幂等键和购物车项主键列表。
     *
     * @param memberId 当前登录会员主键
     * @param idempotencyKey 已标准化的幂等键
     * @param request 下单请求
     */
    private void validateCreateOrderRequest(
            Long memberId,
            String idempotencyKey,
            OrderCreateRequest request
    ) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(OrderErrorCode.ORDER_CREATE_REQUEST_INVALID);
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 64) {
            throw new ApiException(OrderErrorCode.IDEMPOTENCY_KEY_INVALID);
        }

        if (request == null
                || request.cartItemIds() == null
                || request.cartItemIds().isEmpty()) {
            throw new ApiException(OrderErrorCode.ORDER_CREATE_REQUEST_INVALID);
        }

        Set<Long> cartItemIds = new HashSet<>();

        for (Long cartItemId : request.cartItemIds()) {
            if (cartItemId == null
                    || cartItemId <= 0
                    || !cartItemIds.add(cartItemId)) {
                throw new ApiException(
                        OrderErrorCode.ORDER_CREATE_REQUEST_INVALID
                );
            }
        }
    }

    /**
     * 按会员和幂等键查询已经成功创建的订单。
     *
     * @param memberId 当前登录会员主键
     * @param idempotencyKey 已标准化的幂等键
     * @return 已创建的订单；不存在时返回 {@code null}
     */
    private OmsOrder findOrderByIdempotencyKey(Long memberId, String idempotencyKey) {
        OmsOrderExample example = new OmsOrderExample();
        example.createCriteria()
                .andMemberIdEqualTo(memberId)
                .andIdempotencyKeyEqualTo(idempotencyKey);

        List<OmsOrder> orders = omsOrderMapper.selectByExample(example);
        if (orders.isEmpty()) {
            return null;
        }

        return orders.get(0);
    }

    /**
     * 校验会员主键和订单主键。
     *
     * @param memberId 当前登录会员主键
     * @param orderId 订单主键
     */
    private void validateMemberAndOrderId(Long memberId, Long orderId) {
        if (memberId == null || memberId <= 0
                || orderId == null || orderId <= 0) {
            throw new ApiException(OrderErrorCode.ORDER_NOT_FOUND);
        }
    }

    /**
     * 查询指定会员拥有的订单。
     *
     * @param memberId 当前登录会员主键
     * @param orderId 订单主键
     * @return 当前会员拥有的订单
     */
    private OmsOrder getOwnedOrder(Long memberId, Long orderId) {
        OmsOrderExample example = new OmsOrderExample();
        example.createCriteria()
                .andIdEqualTo(orderId)
                .andMemberIdEqualTo(memberId);

        List<OmsOrder> orders = omsOrderMapper.selectByExample(example);
        if (orders.isEmpty()) {
            throw new ApiException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        return orders.get(0);
    }

    /**
     * 按主键升序查询订单的全部明细。
     *
     * @param orderId 订单主键
     * @return 订单明细列表
     */
    private List<OmsOrderItem> listOrderItems(Long orderId) {
        OmsOrderItemExample example = new OmsOrderItemExample();
        example.createCriteria().andOrderIdEqualTo(orderId);
        example.setOrderByClause("id asc");

        return omsOrderItemMapper.selectByExample(example);
    }
}

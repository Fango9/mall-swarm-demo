package cn.fango.mall.portal.service.impl;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.api.ResultCode;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.stock.StockReleaseRequest;
import cn.fango.mall.common.stock.StockReservationItem;
import cn.fango.mall.common.stock.StockReservationRequest;
import cn.fango.mall.mbg.mapper.OmsCartItemMapper;
import cn.fango.mall.mbg.mapper.OmsOrderItemMapper;
import cn.fango.mall.mbg.mapper.OmsOrderMapper;
import cn.fango.mall.mbg.model.OmsCartItem;
import cn.fango.mall.mbg.model.OmsCartItemExample;
import cn.fango.mall.mbg.model.OmsOrder;
import cn.fango.mall.mbg.model.OmsOrderExample;
import cn.fango.mall.mbg.model.OmsOrderItem;
import cn.fango.mall.mbg.model.OmsOrderItemExample;
import cn.fango.mall.portal.api.OrderErrorCode;
import cn.fango.mall.portal.client.PortalStockClient;
import cn.fango.mall.portal.dto.OrderCreateRequest;
import cn.fango.mall.portal.dto.OrderDetailResponse;
import cn.fango.mall.portal.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 后台库存内部接口客户端。
     */
    private final PortalStockClient portalStockClient;

    /**
     * Portal 本地订单事务服务。
     */
    private final OrderLocalTransactionService orderLocalTransactionService;

    /**
     * 创建会员订单服务。
     *
     * @param omsCartItemMapper 购物车项数据访问对象
     * @param omsOrderMapper 订单主记录数据访问对象
     * @param omsOrderItemMapper 订单明细数据访问对象
     * @param portalStockClient 后台库存内部接口客户端
     * @param orderLocalTransactionService Portal 本地订单事务服务
     */
    public OrderServiceImpl(
            OmsCartItemMapper omsCartItemMapper,
            OmsOrderMapper omsOrderMapper,
            OmsOrderItemMapper omsOrderItemMapper,
            PortalStockClient portalStockClient,
            OrderLocalTransactionService orderLocalTransactionService
    ) {
        this.omsCartItemMapper = omsCartItemMapper;
        this.omsOrderMapper = omsOrderMapper;
        this.omsOrderItemMapper = omsOrderItemMapper;
        this.portalStockClient = portalStockClient;
        this.orderLocalTransactionService = orderLocalTransactionService;
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
    public OrderDetailResponse createOrder(
            Long memberId,
            String idempotencyKey,
            OrderCreateRequest request
    ) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        validateCreateOrderRequest(memberId, normalizedIdempotencyKey, request);

        OmsOrder existingOrder = findOrderByIdempotencyKey(memberId, normalizedIdempotencyKey);
        if (existingOrder != null) {
            return getOrderDetail(memberId, existingOrder.getId());
        }

        List<OmsCartItem> cartItems = loadSelectedCartItems(
                memberId,
                request.cartItemIds()
        );
        String orderSn = generateOrderSn();

        reserveStock(orderSn, cartItems);

        OmsOrder createdOrder;
        try {
            createdOrder = orderLocalTransactionService.createOrder(
                    memberId,
                    normalizedIdempotencyKey,
                    orderSn,
                    cartItems
            );
        } catch (DuplicateKeyException exception) {
            releaseStockAfterLocalFailure(orderSn, exception);

            OmsOrder idempotentOrder = findOrderByIdempotencyKey(
                    memberId,
                    normalizedIdempotencyKey
            );
            if (idempotentOrder != null) {
                return getOrderDetail(memberId, idempotentOrder.getId());
            }

            LOGGER.error(
                    "订单唯一索引冲突，且未查询到幂等订单，orderSn={}",
                    orderSn,
                    exception
            );
            throw new ApiException(
                    OrderErrorCode.ORDER_CREATE_FAILED,
                    exception
            );
        } catch (ApiException exception) {
            releaseStockAfterLocalFailure(orderSn, exception);
            throw exception;
        } catch (RuntimeException exception) {
            releaseStockAfterLocalFailure(orderSn, exception);

            LOGGER.error(
                    "本地订单事务失败，已执行库存释放补偿，orderSn={}",
                    orderSn,
                    exception
            );
            throw new ApiException(
                    OrderErrorCode.ORDER_CREATE_FAILED,
                    exception
            );
        }

        return getOrderDetail(memberId, createdOrder.getId());
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

        OmsOrder order = getOwnedOrder(memberId, orderId);
        List<OmsOrderItem> orderItems = listOrderItems(order.getId());

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
     * 生成长度小于订单编号字段上限的订单编号。
     *
     * @return 新生成的订单编号
     */
    private String generateOrderSn() {
        String uniquePart = UUID.randomUUID().toString().replace("-", "");

        return "O" + uniquePart;
    }

    /**
     * 调用 mall-admin 原子预占当前购物车快照中的全部 SKU 库存。
     *
     * @param orderSn 同时作为库存预占编号的订单编号
     * @param cartItems 本次结算的购物车快照
     */
    private void reserveStock(
            String orderSn,
            List<OmsCartItem> cartItems
    ) {
        List<StockReservationItem> items = new ArrayList<>();

        for (OmsCartItem cartItem : cartItems) {
            items.add(new StockReservationItem(
                    cartItem.getProductSkuId(),
                    cartItem.getQuantity()
            ));
        }

        StockReservationRequest request = new StockReservationRequest(
                orderSn,
                items
        );
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
     * 在本地订单事务失败后释放已预占库存。
     *
     * <p>释放失败不会覆盖最初的本地失败异常，而是作为 suppressed exception
     * 附加到原异常中，保留完整故障上下文。</p>
     *
     * @param orderSn 库存预占编号
     * @param originalException 导致本地事务失败的原始异常
     */
    private void releaseStockAfterLocalFailure(
            String orderSn,
            RuntimeException originalException
    ) {
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
    private OmsOrder findOrderByIdempotencyKey(
            Long memberId,
            String idempotencyKey
    ) {
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
     * 查询当前会员选中的购物车项，并确认每个请求主键均属于该会员。
     *
     * @param memberId 当前登录会员主键
     * @param cartItemIds 本次结算的购物车项主键列表
     * @return 购物车项快照
     */
    private List<OmsCartItem> loadSelectedCartItems(
            Long memberId,
            List<Long> cartItemIds
    ) {
        OmsCartItemExample example = new OmsCartItemExample();
        example.createCriteria()
                .andMemberIdEqualTo(memberId)
                .andIdIn(cartItemIds);

        List<OmsCartItem> cartItems =
                omsCartItemMapper.selectByExample(example);

        if (cartItems.size() != cartItemIds.size()) {
            throw new ApiException(OrderErrorCode.ORDER_CART_ITEM_NOT_FOUND);
        }

        return cartItems;
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
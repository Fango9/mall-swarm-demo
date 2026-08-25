package cn.fango.mall.portal.service.impl;

import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.common.api.ResultCode;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.product.ProductDetailResponse;
import cn.fango.mall.common.product.SkuStockResponse;
import cn.fango.mall.mbg.mapper.OmsCartItemMapper;
import cn.fango.mall.mbg.model.OmsCartItem;
import cn.fango.mall.mbg.model.OmsCartItemExample;
import cn.fango.mall.portal.api.CartErrorCode;
import cn.fango.mall.portal.client.PortalProductClient;
import cn.fango.mall.portal.dto.CartItemAddRequest;
import cn.fango.mall.portal.dto.CartItemQuantityUpdateRequest;
import cn.fango.mall.portal.dto.CartItemResponse;
import cn.fango.mall.portal.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 会员购物车服务实现。
 */
@Service
public class CartServiceImpl implements CartService {

    /**
     * 购物车项数据访问对象。
     */
    private final OmsCartItemMapper omsCartItemMapper;

    /**
     * 后台商品内部查询 Feign 客户端。
     */
    private final PortalProductClient portalProductClient;

    /**
     * 创建会员购物车服务。
     *
     * @param omsCartItemMapper 购物车项数据访问对象
     * @param portalProductClient 后台商品内部查询 Feign 客户端
     */
    public CartServiceImpl(
            OmsCartItemMapper omsCartItemMapper,
            PortalProductClient portalProductClient
    ) {
        this.omsCartItemMapper = omsCartItemMapper;
        this.portalProductClient = portalProductClient;
    }

    /**
     * 将商品和 SKU 的当前快照保存到购物车。
     * 同一会员再次加入同一 SKU 时，合并购买数量并刷新快照。
     *
     * @param memberId 当前登录会员主键
     * @param request 新增购物车项请求
     * @return 新增或合并后的购物车项主键
     */
    @Override
    @Transactional
    public Long addCartItem(Long memberId, CartItemAddRequest request) {
        validateAddRequest(memberId, request);

        ProductDetailResponse product =
                getPublishedProduct(request.productId());
        SkuStockResponse sku = getSku(product, request.skuId());

        OmsCartItem existingCartItem = findCartItemByMemberAndSku(
                memberId,
                request.skuId()
        );

        if (existingCartItem == null) {
            OmsCartItem cartItem = createCartItem(
                    memberId,
                    product,
                    sku,
                    request.quantity()
            );

            int inserted = omsCartItemMapper.insertSelective(cartItem);
            if (inserted != 1 || cartItem.getId() == null) {
                throw new ApiException(CartErrorCode.CART_ITEM_CREATE_FAILED);
            }

            return cartItem.getId();
        }

        int mergedQuantity = addQuantity(
                existingCartItem.getQuantity(),
                request.quantity()
        );

        OmsCartItem updatedCartItem = createCartItem(
                memberId,
                product,
                sku,
                mergedQuantity
        );
        updatedCartItem.setId(existingCartItem.getId());

        int updated =
                omsCartItemMapper.updateByPrimaryKeySelective(updatedCartItem);
        if (updated != 1) {
            throw new ApiException(CartErrorCode.CART_ITEM_UPDATE_FAILED);
        }

        return existingCartItem.getId();
    }

    /**
     * 查询当前会员的全部购物车项。
     *
     * @param memberId 当前登录会员主键
     * @return 按最近修改时间倒序排列的购物车项列表
     */
    @Override
    public List<CartItemResponse> listCartItems(Long memberId) {
        validateMemberId(memberId);

        OmsCartItemExample example = new OmsCartItemExample();
        example.createCriteria().andMemberIdEqualTo(memberId);
        example.setOrderByClause("modify_time desc, id desc");

        List<OmsCartItem> cartItems =
                omsCartItemMapper.selectByExample(example);
        List<CartItemResponse> responses = new ArrayList<>();

        for (OmsCartItem cartItem : cartItems) {
            responses.add(toResponse(cartItem));
        }

        return responses;
    }

    /**
     * 修改当前会员指定购物车项的购买数量。
     *
     * @param memberId 当前登录会员主键
     * @param cartItemId 购物车项主键
     * @param request 修改数量请求
     * @return 是否修改成功
     */
    @Override
    @Transactional
    public boolean updateCartItemQuantity(
            Long memberId,
            Long cartItemId,
            CartItemQuantityUpdateRequest request
    ) {
        validateQuantityUpdateRequest(memberId, cartItemId, request);
        getOwnedCartItem(memberId, cartItemId);

        OmsCartItem updatedCartItem = new OmsCartItem();
        updatedCartItem.setId(cartItemId);
        updatedCartItem.setQuantity(request.quantity());

        int updated =
                omsCartItemMapper.updateByPrimaryKeySelective(updatedCartItem);
        if (updated != 1) {
            throw new ApiException(CartErrorCode.CART_ITEM_UPDATE_FAILED);
        }

        return true;
    }

    /**
     * 删除当前会员指定购物车项。
     *
     * @param memberId 当前登录会员主键
     * @param cartItemId 购物车项主键
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteCartItem(Long memberId, Long cartItemId) {
        validateCartItemId(memberId, cartItemId);
        getOwnedCartItem(memberId, cartItemId);

        int deleted = omsCartItemMapper.deleteByPrimaryKey(cartItemId);
        if (deleted != 1) {
            throw new ApiException(CartErrorCode.CART_ITEM_DELETE_FAILED);
        }

        return true;
    }

    /**
     * 调用后台服务查询可展示商品详情。
     *
     * @param productId 商品主键
     * @return 商品详情及 SKU 列表
     */
    private ProductDetailResponse getPublishedProduct(Long productId) {
        CommonResult<ProductDetailResponse> result;

        try {
            result = portalProductClient.getPublishedProductDetail(productId);
        } catch (RuntimeException exception) {
            throw new ApiException(CartErrorCode.PRODUCT_QUERY_FAILED);
        }

        if (result == null
                || result.getCode() != ResultCode.SUCCESS.getCode()
                || result.getData() == null) {
            throw new ApiException(CartErrorCode.PRODUCT_QUERY_FAILED);
        }

        return result.getData();
    }

    /**
     * 在商品详情中查找指定 SKU。
     *
     * @param product 商品详情
     * @param skuId SKU 主键
     * @return 匹配的 SKU 快照
     */
    private SkuStockResponse getSku(
            ProductDetailResponse product,
            Long skuId
    ) {
        if (product.skuStocks() == null) {
            throw new ApiException(CartErrorCode.SKU_NOT_FOUND);
        }

        for (SkuStockResponse sku : product.skuStocks()) {
            if (skuId.equals(sku.id())) {
                return sku;
            }
        }

        throw new ApiException(CartErrorCode.SKU_NOT_FOUND);
    }

    /**
     * 创建用于新增或刷新购物车快照的实体。
     *
     * @param memberId 当前登录会员主键
     * @param product 商品详情快照
     * @param sku SKU 快照
     * @param quantity 购买数量
     * @return 待保存的购物车项实体
     */
    private OmsCartItem createCartItem(
            Long memberId,
            ProductDetailResponse product,
            SkuStockResponse sku,
            Integer quantity
    ) {
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(memberId);
        cartItem.setProductId(product.id());
        cartItem.setProductName(product.name());
        cartItem.setProductPic(product.pic());
        cartItem.setProductSkuId(sku.id());
        cartItem.setProductSkuCode(sku.skuCode());
        cartItem.setProductSkuAttrs(sku.saleAttrs());
        cartItem.setPrice(sku.price());
        cartItem.setQuantity(quantity);

        return cartItem;
    }

    /**
     * 查询当前会员指定 SKU 的购物车项。
     *
     * @param memberId 当前登录会员主键
     * @param skuId SKU 主键
     * @return 已有购物车项；不存在时返回 {@code null}
     */
    private OmsCartItem findCartItemByMemberAndSku(
            Long memberId,
            Long skuId
    ) {
        OmsCartItemExample example = new OmsCartItemExample();
        example.createCriteria()
                .andMemberIdEqualTo(memberId)
                .andProductSkuIdEqualTo(skuId);

        List<OmsCartItem> cartItems =
                omsCartItemMapper.selectByExample(example);
        if (cartItems.isEmpty()) {
            return null;
        }

        return cartItems.get(0);
    }

    /**
     * 查询并校验购物车项属于当前会员。
     *
     * @param memberId 当前登录会员主键
     * @param cartItemId 购物车项主键
     * @return 当前会员拥有的购物车项
     */
    private OmsCartItem getOwnedCartItem(Long memberId, Long cartItemId) {
        OmsCartItem cartItem = omsCartItemMapper.selectByPrimaryKey(cartItemId);
        if (cartItem == null || !memberId.equals(cartItem.getMemberId())) {
            throw new ApiException(CartErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cartItem;
    }

    /**
     * 校验新增购物车项请求。
     *
     * @param memberId 当前登录会员主键
     * @param request 新增购物车项请求
     */
    private void validateAddRequest(Long memberId, CartItemAddRequest request) {
        validateMemberId(memberId);

        if (request == null
                || request.productId() == null
                || request.productId() <= 0
                || request.skuId() == null
                || request.skuId() <= 0
                || request.quantity() == null
                || request.quantity() <= 0) {
            throw new ApiException(CartErrorCode.CART_ITEM_INVALID);
        }
    }

    /**
     * 校验修改购物车数量请求。
     *
     * @param memberId 当前登录会员主键
     * @param cartItemId 购物车项主键
     * @param request 修改数量请求
     */
    private void validateQuantityUpdateRequest(
            Long memberId,
            Long cartItemId,
            CartItemQuantityUpdateRequest request
    ) {
        validateCartItemId(memberId, cartItemId);

        if (request == null
                || request.quantity() == null
                || request.quantity() <= 0) {
            throw new ApiException(CartErrorCode.CART_ITEM_INVALID);
        }
    }

    /**
     * 校验当前会员主键。
     *
     * @param memberId 当前登录会员主键
     */
    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(CartErrorCode.CART_ITEM_INVALID);
        }
    }

    /**
     * 校验购物车项主键。
     *
     * @param memberId 当前登录会员主键
     * @param cartItemId 购物车项主键
     */
    private void validateCartItemId(Long memberId, Long cartItemId) {
        validateMemberId(memberId);

        if (cartItemId == null || cartItemId <= 0) {
            throw new ApiException(CartErrorCode.CART_ITEM_INVALID);
        }
    }

    /**
     * 安全合并已有数量和新增数量。
     *
     * @param existingQuantity 已有购买数量
     * @param addedQuantity 新增购买数量
     * @return 合并后的购买数量
     */
    private int addQuantity(
            Integer existingQuantity,
            Integer addedQuantity
    ) {
        try {
            return Math.addExact(existingQuantity, addedQuantity);
        } catch (ArithmeticException exception) {
            throw new ApiException(CartErrorCode.CART_ITEM_INVALID);
        }
    }

    /**
     * 将购物车实体转换为对外响应。
     *
     * @param cartItem 购物车项实体
     * @return 购物车项响应
     */
    private CartItemResponse toResponse(OmsCartItem cartItem) {
        BigDecimal totalAmount = cartItem.getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProductId(),
                cartItem.getProductName(),
                cartItem.getProductPic(),
                cartItem.getProductSkuId(),
                cartItem.getProductSkuCode(),
                cartItem.getPrice(),
                cartItem.getQuantity(),
                totalAmount
        );
    }
}
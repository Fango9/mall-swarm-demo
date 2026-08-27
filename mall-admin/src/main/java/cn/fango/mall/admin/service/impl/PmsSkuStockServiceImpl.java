package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.ProductErrorCode;
import cn.fango.mall.admin.api.SkuStockErrorCode;
import cn.fango.mall.admin.dto.SkuStockCreateRequest;
import cn.fango.mall.admin.dto.SkuStockUpdateRequest;
import cn.fango.mall.admin.service.PmsSkuStockService;
import cn.fango.mall.admin.service.ProductOutboxEventService;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsProductMapper;
import cn.fango.mall.mbg.mapper.PmsSkuStockMapper;
import cn.fango.mall.mbg.model.PmsProduct;
import cn.fango.mall.mbg.model.PmsSkuStock;
import cn.fango.mall.mbg.model.PmsSkuStockExample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 SKU 管理服务实现。
 */
@Service
public class PmsSkuStockServiceImpl implements PmsSkuStockService {

    private final PmsProductMapper pmsProductMapper;
    private final PmsSkuStockMapper pmsSkuStockMapper;
    /**
     * 商品变更 Outbox 事件写入服务。
     */
    private final ProductOutboxEventService productOutboxEventService;

    /**
     * 创建商品 SKU 管理服务。
     *
     * @param pmsProductMapper 商品数据访问对象
     * @param pmsSkuStockMapper SKU 数据访问对象
     * @param productOutboxEventService 商品变更 Outbox 事件写入服务
     */
    public PmsSkuStockServiceImpl(PmsProductMapper pmsProductMapper, PmsSkuStockMapper pmsSkuStockMapper, ProductOutboxEventService productOutboxEventService) {
        this.pmsProductMapper = pmsProductMapper;
        this.pmsSkuStockMapper = pmsSkuStockMapper;
        this.productOutboxEventService = productOutboxEventService;
    }

    /**
     * 查询指定商品下的指定 SKU。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @return SKU 详情
     */
    @Override
    public PmsSkuStock getSku(Long productId, Long skuId) {
        getProduct(productId);

        PmsSkuStock skuStock = pmsSkuStockMapper.selectByPrimaryKey(skuId);
        if (skuStock == null || !productId.equals(skuStock.getProductId())) {
            throw new ApiException(SkuStockErrorCode.SKU_NOT_FOUND);
        }

        return skuStock;
    }

    /**
     * 查询指定商品下的全部 SKU。
     *
     * @param productId 商品主键
     * @return SKU 列表
     */
    @Override
    public List<PmsSkuStock> listSkus(Long productId) {
        getProduct(productId);

        PmsSkuStockExample example = new PmsSkuStockExample();
        example.createCriteria().andProductIdEqualTo(productId);
        example.setOrderByClause("id asc");

        return pmsSkuStockMapper.selectByExample(example);
    }

    /**
     * 为指定商品创建 SKU、刷新商品汇总价格与库存，并在同一事务中写入商品变更 Outbox 事件。
     *
     * @param productId 商品主键
     * @param request 创建 SKU 请求
     * @return 新创建 SKU 的主键
     */
    @Override
    @Transactional
    public Long createSku(Long productId, SkuStockCreateRequest request) {
        getProduct(productId);
        validateCreateRequest(request);
        checkSkuCodeExists(productId, request.skuCode(), null);

        BigDecimal price = request.price() == null ? BigDecimal.ZERO : request.price();
        Integer stock = request.stock() == null ? 0 : request.stock();

        PmsSkuStock skuStock = new PmsSkuStock();
        skuStock.setProductId(productId);
        skuStock.setSkuCode(request.skuCode());
        skuStock.setPrice(price);
        skuStock.setStock(stock);
        skuStock.setPic(request.pic());
        skuStock.setSaleAttrs(request.saleAttrs());

        int count = pmsSkuStockMapper.insertSelective(skuStock);
        if (count != 1 || skuStock.getId() == null) {
            throw new ApiException(SkuStockErrorCode.SKU_CREATE_FAILED);
        }

        refreshProductPriceAndStock(productId);
        productOutboxEventService.recordProductChanged(productId);
        return skuStock.getId();
    }

    /**
     * 更新指定商品下的指定 SKU、刷新商品汇总价格与库存，并在同一事务中写入商品变更 Outbox 事件。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @param request 更新 SKU 请求
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateSku(Long productId, Long skuId, SkuStockUpdateRequest request) {
        getSku(productId, skuId);
        validateUpdateRequest(request);
        checkSkuCodeExists(productId, request.skuCode(), skuId);

        PmsSkuStock skuStock = new PmsSkuStock();
        skuStock.setId(skuId);
        skuStock.setSkuCode(request.skuCode());
        skuStock.setPrice(request.price());
        skuStock.setStock(request.stock());
        skuStock.setPic(request.pic());
        skuStock.setSaleAttrs(request.saleAttrs());

        int count = pmsSkuStockMapper.updateByPrimaryKeySelective(skuStock);
        if (count != 1) {
            throw new ApiException(SkuStockErrorCode.SKU_UPDATE_FAILED);
        }

        refreshProductPriceAndStock(productId);
        productOutboxEventService.recordProductChanged(productId);
        return true;
    }

    /**
     * 删除指定商品下的指定 SKU、刷新商品汇总价格与库存，并在同一事务中写入商品变更 Outbox 事件。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteSku(Long productId, Long skuId) {
        getSku(productId, skuId);

        int count = pmsSkuStockMapper.deleteByPrimaryKey(skuId);
        if (count != 1) {
            throw new ApiException(SkuStockErrorCode.SKU_DELETE_FAILED);
        }

        refreshProductPriceAndStock(productId);
        productOutboxEventService.recordProductChanged(productId);
        return true;
    }

    /**
     * 查询商品，不存在时抛出商品不存在业务异常。
     *
     * @param productId 商品主键
     * @return 商品实体
     */
    private PmsProduct getProduct(Long productId) {
        PmsProduct product = pmsProductMapper.selectByPrimaryKey(productId);
        if (product == null) {
            throw new ApiException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        return product;
    }

    /**
     * 校验创建 SKU 请求。
     *
     * @param request 创建 SKU 请求
     */
    private void validateCreateRequest(SkuStockCreateRequest request) {
        if (request == null) {
            throw new ApiException(SkuStockErrorCode.SKU_CODE_REQUIRED);
        }

        validateSkuFields(request.skuCode(), request.price(), request.stock());
    }

    /**
     * 校验更新 SKU 请求。
     *
     * @param request 更新 SKU 请求
     */
    private void validateUpdateRequest(SkuStockUpdateRequest request) {
        if (request == null) {
            throw new ApiException(SkuStockErrorCode.SKU_CODE_REQUIRED);
        }

        validateSkuFields(request.skuCode(), request.price(), request.stock());
    }

    /**
     * 校验 SKU 编码、价格和库存。
     *
     * @param skuCode SKU 编码
     * @param price SKU 价格
     * @param stock SKU 库存
     */
    private void validateSkuFields(String skuCode, BigDecimal price, Integer stock) {
        if (!StringUtils.hasText(skuCode)) {
            throw new ApiException(SkuStockErrorCode.SKU_CODE_REQUIRED);
        }
        if (price != null && price.signum() < 0) {
            throw new ApiException(SkuStockErrorCode.SKU_PRICE_INVALID);
        }
        if (stock != null && stock < 0) {
            throw new ApiException(SkuStockErrorCode.SKU_STOCK_INVALID);
        }
    }

    /**
     * 校验同一商品下的 SKU 编码是否重复。
     *
     * @param productId 商品主键
     * @param skuCode SKU 编码
     * @param excludedSkuId 更新时排除的 SKU 主键，创建时传 null
     */
    private void checkSkuCodeExists(Long productId, String skuCode, Long excludedSkuId) {
        PmsSkuStockExample example = new PmsSkuStockExample();
        PmsSkuStockExample.Criteria criteria = example.createCriteria();
        criteria.andProductIdEqualTo(productId);
        criteria.andSkuCodeEqualTo(skuCode);

        if (excludedSkuId != null) {
            criteria.andIdNotEqualTo(excludedSkuId);
        }

        long count = pmsSkuStockMapper.countByExample(example);
        if (count > 0) {
            throw new ApiException(SkuStockErrorCode.SKU_CODE_EXISTS);
        }
    }

    /**
     * 按当前全部 SKU 重新计算并回写商品最低价格和总库存。
     *
     * @param productId 商品主键
     */
    private void refreshProductPriceAndStock(Long productId) {
        PmsSkuStockExample example = new PmsSkuStockExample();
        example.createCriteria().andProductIdEqualTo(productId);

        List<PmsSkuStock> skuStocks = pmsSkuStockMapper.selectByExample(example);
        BigDecimal minPrice = null;
        int totalStock = 0;

        for (PmsSkuStock skuStock : skuStocks) {
            if (minPrice == null || skuStock.getPrice().compareTo(minPrice) < 0) {
                minPrice = skuStock.getPrice();
            }

            totalStock += skuStock.getStock();
        }

        if (minPrice == null) {
            minPrice = BigDecimal.ZERO;
        }

        PmsProduct product = new PmsProduct();
        product.setId(productId);
        product.setPrice(minPrice);
        product.setStock(totalStock);

        int count = pmsProductMapper.updateByPrimaryKeySelective(product);
        if (count != 1) {
            throw new ApiException(ProductErrorCode.PRODUCT_UPDATE_FAILED);
        }
    }
}
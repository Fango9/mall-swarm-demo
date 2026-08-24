package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.ProductErrorCode;
import cn.fango.mall.admin.service.PortalProductQueryService;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.product.ProductCategoryResponse;
import cn.fango.mall.common.product.ProductDetailResponse;
import cn.fango.mall.common.product.ProductSummaryResponse;
import cn.fango.mall.common.product.SkuStockResponse;
import cn.fango.mall.mbg.mapper.PmsProductCategoryMapper;
import cn.fango.mall.mbg.mapper.PmsProductMapper;
import cn.fango.mall.mbg.mapper.PmsSkuStockMapper;
import cn.fango.mall.mbg.model.PmsProduct;
import cn.fango.mall.mbg.model.PmsProductCategory;
import cn.fango.mall.mbg.model.PmsProductCategoryExample;
import cn.fango.mall.mbg.model.PmsProductExample;
import cn.fango.mall.mbg.model.PmsSkuStock;
import cn.fango.mall.mbg.model.PmsSkuStockExample;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 为商城门户提供的商品只读查询服务实现。
 */
@Service
public class PortalProductQueryServiceImpl implements PortalProductQueryService {

    private final PmsProductCategoryMapper pmsProductCategoryMapper;
    private final PmsProductMapper pmsProductMapper;
    private final PmsSkuStockMapper pmsSkuStockMapper;

    /**
     * 创建商城门户商品查询服务。
     *
     * @param pmsProductCategoryMapper 商品分类数据访问对象
     * @param pmsProductMapper 商品数据访问对象
     * @param pmsSkuStockMapper SKU 数据访问对象
     */
    public PortalProductQueryServiceImpl(
            PmsProductCategoryMapper pmsProductCategoryMapper,
            PmsProductMapper pmsProductMapper,
            PmsSkuStockMapper pmsSkuStockMapper
    ) {
        this.pmsProductCategoryMapper = pmsProductCategoryMapper;
        this.pmsProductMapper = pmsProductMapper;
        this.pmsSkuStockMapper = pmsSkuStockMapper;
    }

    /**
     * 查询全部可展示商品分类。
     *
     * @return 可展示商品分类列表
     */
    @Override
    public List<ProductCategoryResponse> listVisibleCategories() {
        PmsProductCategoryExample example = new PmsProductCategoryExample();
        example.createCriteria().andShowStatusEqualTo((byte) 1);
        example.setOrderByClause("level asc, sort desc, id desc");

        List<PmsProductCategory> categories =
                pmsProductCategoryMapper.selectByExample(example);
        List<ProductCategoryResponse> responses = new ArrayList<>();

        for (PmsProductCategory category : categories) {
            responses.add(toCategoryResponse(category));
        }

        return responses;
    }

    /**
     * 查询可展示商品。
     *
     * @param categoryId 商品分类主键；传 null 时查询全部分类
     * @return 可展示商品列表
     */
    @Override
    public List<ProductSummaryResponse> listPublishedProducts(Long categoryId) {
        if (categoryId != null && !isVisibleCategory(categoryId)) {
            return List.of();
        }

        PmsProductExample example = new PmsProductExample();
        PmsProductExample.Criteria criteria = example.createCriteria();
        criteria.andDeleteStatusEqualTo((byte) 0);
        criteria.andPublishStatusEqualTo((byte) 1);

        if (categoryId != null) {
            criteria.andProductCategoryIdEqualTo(categoryId);
        }

        example.setOrderByClause("sort desc, id desc");

        List<PmsProduct> products = pmsProductMapper.selectByExample(example);
        List<ProductSummaryResponse> responses = new ArrayList<>();

        for (PmsProduct product : products) {
            if (isVisibleCategory(product.getProductCategoryId())) {
                responses.add(toSummaryResponse(product));
            }
        }

        return responses;
    }

    /**
     * 查询可展示商品详情及其 SKU。
     *
     * @param productId 商品主键
     * @return 商品详情及 SKU 列表
     */
    @Override
    public ProductDetailResponse getPublishedProductDetail(Long productId) {
        PmsProductExample example = new PmsProductExample();
        PmsProductExample.Criteria criteria = example.createCriteria();
        criteria.andIdEqualTo(productId);
        criteria.andDeleteStatusEqualTo((byte) 0);
        criteria.andPublishStatusEqualTo((byte) 1);

        List<PmsProduct> products = pmsProductMapper.selectByExampleWithBLOBs(example);
        if (products.isEmpty() || !isVisibleCategory(products.get(0).getProductCategoryId())) {
            throw new ApiException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        PmsProduct product = products.get(0);

        PmsSkuStockExample skuExample = new PmsSkuStockExample();
        skuExample.createCriteria().andProductIdEqualTo(productId);
        skuExample.setOrderByClause("id asc");

        List<PmsSkuStock> skuStocks = pmsSkuStockMapper.selectByExample(skuExample);
        List<SkuStockResponse> skuResponses = new ArrayList<>();

        for (PmsSkuStock skuStock : skuStocks) {
            skuResponses.add(toSkuStockResponse(skuStock));
        }

        return new ProductDetailResponse(
                product.getId(),
                product.getProductCategoryId(),
                product.getName(),
                product.getProductSn(),
                product.getPrice(),
                product.getStock(),
                product.getPic(),
                product.getDetailDesc(),
                skuResponses
        );
    }

    /**
     * 判断商品分类是否可展示。
     *
     * @param categoryId 商品分类主键
     * @return 是否可展示
     */
    private boolean isVisibleCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }

        PmsProductCategory category =
                pmsProductCategoryMapper.selectByPrimaryKey(categoryId);

        return category != null && category.getShowStatus() != null
                && category.getShowStatus() == 1;
    }

    /**
     * 将商品分类实体转换为查询响应。
     *
     * @param category 商品分类实体
     * @return 商品分类查询响应
     */
    private ProductCategoryResponse toCategoryResponse(PmsProductCategory category) {
        return new ProductCategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getLevel(),
                category.getSort()
        );
    }

    /**
     * 将商品实体转换为商品列表查询响应。
     *
     * @param product 商品实体
     * @return 商品列表查询响应
     */
    private ProductSummaryResponse toSummaryResponse(PmsProduct product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getProductCategoryId(),
                product.getName(),
                product.getProductSn(),
                product.getPrice(),
                product.getStock(),
                product.getPic()
        );
    }

    /**
     * 将 SKU 实体转换为 SKU 查询响应。
     *
     * @param skuStock SKU 实体
     * @return SKU 查询响应
     */
    private SkuStockResponse toSkuStockResponse(PmsSkuStock skuStock) {
        return new SkuStockResponse(
                skuStock.getId(),
                skuStock.getSkuCode(),
                skuStock.getPrice(),
                skuStock.getStock(),
                skuStock.getPic(),
                skuStock.getSaleAttrs()
        );
    }
}
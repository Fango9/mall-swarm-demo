package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.service.ProductSearchSourceQueryService;
import cn.fango.mall.common.search.ProductSearchSourceResponse;
import cn.fango.mall.mbg.mapper.PmsProductCategoryMapper;
import cn.fango.mall.mbg.mapper.PmsProductMapper;
import cn.fango.mall.mbg.model.PmsProduct;
import cn.fango.mall.mbg.model.PmsProductCategory;
import cn.fango.mall.mbg.model.PmsProductExample;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 为商品搜索索引提供只读源数据的查询服务实现。
 */
@Service
public class ProductSearchSourceQueryServiceImpl implements ProductSearchSourceQueryService {

    /**
     * 商品数据访问对象。
     */
    private final PmsProductMapper pmsProductMapper;

    /**
     * 商品分类数据访问对象。
     */
    private final PmsProductCategoryMapper pmsProductCategoryMapper;

    /**
     * 创建商品搜索索引源数据查询服务。
     *
     * @param pmsProductMapper 商品数据访问对象
     * @param pmsProductCategoryMapper 商品分类数据访问对象
     */
    public ProductSearchSourceQueryServiceImpl(PmsProductMapper pmsProductMapper, PmsProductCategoryMapper pmsProductCategoryMapper) {
        this.pmsProductMapper = pmsProductMapper;
        this.pmsProductCategoryMapper = pmsProductCategoryMapper;
    }

    /**
     * 查询指定商品当前可写入搜索索引的源数据。
     *
     * @param productId 商品主键
     * @return 可索引商品源数据；不可索引时返回 {@code null}
     */
    @Override
    public ProductSearchSourceResponse getPublishedProductForIndex(Long productId) {
        if (productId == null || productId <= 0) {
            return null;
        }

        PmsProductExample example = new PmsProductExample();
        PmsProductExample.Criteria criteria = example.createCriteria();
        criteria.andIdEqualTo(productId);
        criteria.andDeleteStatusEqualTo((byte) 0);
        criteria.andPublishStatusEqualTo((byte) 1);

        List<PmsProduct> products = pmsProductMapper.selectByExampleWithBLOBs(example);
        if (products.isEmpty()) {
            return null;
        }

        PmsProduct product = products.get(0);
        if (!isVisibleCategory(product.getProductCategoryId())) {
            return null;
        }

        return toSearchSourceResponse(product);
    }

    /**
     * 查询当前全部可写入搜索索引的商品源数据。
     *
     * @return 全部可索引商品源数据，按商品主键升序排列
     */
    @Override
    public List<ProductSearchSourceResponse> listPublishedProductsForIndex() {
        PmsProductExample example = new PmsProductExample();
        PmsProductExample.Criteria criteria = example.createCriteria();
        criteria.andDeleteStatusEqualTo((byte) 0);
        criteria.andPublishStatusEqualTo((byte) 1);
        example.setOrderByClause("id asc");

        List<PmsProduct> products =
                pmsProductMapper.selectByExampleWithBLOBs(example);
        List<ProductSearchSourceResponse> responses = new ArrayList<>();

        for (PmsProduct product : products) {
            if (isVisibleCategory(product.getProductCategoryId())) {
                responses.add(toSearchSourceResponse(product));
            }
        }

        return responses;
    }

    /**
     * 将商品实体转换为搜索索引源数据。
     *
     * @param product 商品实体
     * @return 商品搜索索引源数据
     */
    private ProductSearchSourceResponse toSearchSourceResponse(PmsProduct product) {
        return new ProductSearchSourceResponse(
                product.getId(),
                product.getProductCategoryId(),
                product.getName(),
                product.getProductSn(),
                product.getPrice(),
                product.getStock(),
                product.getPic(),
                product.getDetailDesc()
        );
    }

    /**
     * 判断商品分类是否可展示。
     *
     * @param categoryId 商品分类主键
     * @return 分类可展示时返回 {@code true}
     */
    private boolean isVisibleCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }

        PmsProductCategory category =
                pmsProductCategoryMapper.selectByPrimaryKey(categoryId);

        return category != null
                && category.getShowStatus() != null
                && category.getShowStatus() == 1;
    }
}
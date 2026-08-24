package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.ProductErrorCode;
import cn.fango.mall.admin.dto.ProductCreateRequest;
import cn.fango.mall.admin.dto.ProductUpdateRequest;
import cn.fango.mall.admin.service.PmsProductService;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsProductCategoryMapper;
import cn.fango.mall.mbg.mapper.PmsProductMapper;
import cn.fango.mall.mbg.model.PmsProduct;
import cn.fango.mall.mbg.model.PmsProductCategory;
import cn.fango.mall.mbg.model.PmsProductExample;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品管理服务实现。
 */
@Service
public class PmsProductServiceImpl implements PmsProductService {

    private final PmsProductMapper pmsProductMapper;
    private final PmsProductCategoryMapper pmsProductCategoryMapper;

    /**
     * 创建商品管理服务。
     *
     * @param pmsProductMapper 商品数据访问对象
     * @param pmsProductCategoryMapper 商品分类数据访问对象
     */
    public PmsProductServiceImpl(
            PmsProductMapper pmsProductMapper,
            PmsProductCategoryMapper pmsProductCategoryMapper
    ) {
        this.pmsProductMapper = pmsProductMapper;
        this.pmsProductCategoryMapper = pmsProductCategoryMapper;
    }

    /**
     * 查询指定商品详情，不存在时抛出商品不存在业务异常。
     *
     * @param id 商品主键
     * @return 商品详情
     */
    @Override
    public PmsProduct getProduct(Long id) {
        PmsProduct product = pmsProductMapper.selectByPrimaryKey(id);
        if (product == null) {
            throw new ApiException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        return product;
    }

    /**
     * 创建商品，并初始化删除状态和审核状态。
     *
     * @param request 创建商品请求
     * @return 新创建商品的主键
     */
    @Override
    public Long createProduct(ProductCreateRequest request) {
        validateCreateRequest(request);
        checkProductSnExists(request.productSn(), null);

        Long brandId = request.brandId() == null ? 0L : request.brandId();
        Byte publishStatus = request.publishStatus() == null ? (byte) 0 : request.publishStatus();
        Integer sort = request.sort() == null ? 0 : request.sort();
        BigDecimal price = request.price() == null ? BigDecimal.ZERO : request.price();
        Integer stock = request.stock() == null ? 0 : request.stock();
        Integer lowStock = request.lowStock() == null ? 0 : request.lowStock();

        PmsProduct product = new PmsProduct();
        product.setBrandId(brandId);
        product.setProductCategoryId(request.productCategoryId());
        product.setName(request.name());
        product.setProductSn(request.productSn());
        product.setDeleteStatus((byte) 0);
        product.setPublishStatus(publishStatus);
        product.setVerifyStatus((byte) 0);
        product.setSort(sort);
        product.setPrice(price);
        product.setStock(stock);
        product.setLowStock(lowStock);
        product.setPic(request.pic());
        product.setDetailDesc(request.detailDesc());

        int count = pmsProductMapper.insertSelective(product);
        if (count != 1 || product.getId() == null) {
            throw new ApiException(ProductErrorCode.PRODUCT_CREATE_FAILED);
        }

        return product.getId();
    }

    /**
     * 分页查询未删除商品，不返回详情描述字段。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页记录数
     * @return 当前页商品列表
     */
    @Override
    public List<PmsProduct> listProducts(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        PmsProductExample example = new PmsProductExample();
        example.createCriteria().andDeleteStatusEqualTo((byte) 0);
        example.setOrderByClause("sort desc, id desc");

        return pmsProductMapper.selectByExample(example);
    }

    /**
     * 更新指定商品，不允许修改删除状态和审核状态。
     *
     * @param id 商品主键
     * @param request 更新商品请求
     * @return 是否更新成功
     */
    @Override
    public boolean updateProduct(Long id, ProductUpdateRequest request) {
        getProduct(id);
        validateUpdateRequest(request);
        checkProductSnExists(request.productSn(), id);

        PmsProduct product = new PmsProduct();
        product.setId(id);
        product.setBrandId(request.brandId());
        product.setProductCategoryId(request.productCategoryId());
        product.setName(request.name());
        product.setProductSn(request.productSn());
        product.setPublishStatus(request.publishStatus());
        product.setSort(request.sort());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setLowStock(request.lowStock());
        product.setPic(request.pic());
        product.setDetailDesc(request.detailDesc());

        int count = pmsProductMapper.updateByPrimaryKeySelective(product);
        if (count != 1) {
            throw new ApiException(ProductErrorCode.PRODUCT_UPDATE_FAILED);
        }

        return true;
    }

    /**
     * 软删除指定商品，并同步强制下架。
     *
     * @param id 商品主键
     * @return 是否删除成功
     */
    @Override
    public boolean deleteProduct(Long id) {
        getProduct(id);

        PmsProduct product = new PmsProduct();
        product.setId(id);
        product.setDeleteStatus((byte) 1);
        product.setPublishStatus((byte) 0);

        int count = pmsProductMapper.updateByPrimaryKeySelective(product);
        if (count != 1) {
            throw new ApiException(ProductErrorCode.PRODUCT_DELETE_FAILED);
        }

        return true;
    }

    /**
     * 校验创建商品请求。
     *
     * @param request 创建商品请求
     */
    private void validateCreateRequest(ProductCreateRequest request) {
        if (request == null) {
            throw new ApiException(ProductErrorCode.PRODUCT_NAME_REQUIRED);
        }

        validateProductFields(
                request.productCategoryId(),
                request.name(),
                request.productSn(),
                request.publishStatus(),
                request.price(),
                request.stock(),
                request.lowStock()
        );
    }

    /**
     * 校验更新商品请求。
     *
     * @param request 更新商品请求
     */
    private void validateUpdateRequest(ProductUpdateRequest request) {
        if (request == null) {
            throw new ApiException(ProductErrorCode.PRODUCT_NAME_REQUIRED);
        }

        validateProductFields(
                request.productCategoryId(),
                request.name(),
                request.productSn(),
                request.publishStatus(),
                request.price(),
                request.stock(),
                request.lowStock()
        );
    }

    /**
     * 校验商品字段及关联商品分类。
     *
     * @param productCategoryId 商品分类主键
     * @param name 商品名称
     * @param productSn 商品货号
     * @param publishStatus 上架状态
     * @param price 商品价格
     * @param stock 商品库存
     * @param lowStock 库存预警值
     */
    private void validateProductFields(
            Long productCategoryId,
            String name,
            String productSn,
            Byte publishStatus,
            BigDecimal price,
            Integer stock,
            Integer lowStock
    ) {
        if (productCategoryId == null || productCategoryId <= 0) {
            throw new ApiException(ProductErrorCode.PRODUCT_CATEGORY_REQUIRED);
        }

        PmsProductCategory category =
                pmsProductCategoryMapper.selectByPrimaryKey(productCategoryId);
        if (category == null) {
            throw new ApiException(ProductErrorCode.PRODUCT_CATEGORY_NOT_FOUND);
        }

        if (!StringUtils.hasText(name)) {
            throw new ApiException(ProductErrorCode.PRODUCT_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(productSn)) {
            throw new ApiException(ProductErrorCode.PRODUCT_SN_REQUIRED);
        }
        if (publishStatus != null && publishStatus != 0 && publishStatus != 1) {
            throw new ApiException(ProductErrorCode.PRODUCT_PUBLISH_STATUS_INVALID);
        }
        if (price != null && price.signum() < 0) {
            throw new ApiException(ProductErrorCode.PRODUCT_PRICE_INVALID);
        }
        if ((stock != null && stock < 0) || (lowStock != null && lowStock < 0)) {
            throw new ApiException(ProductErrorCode.PRODUCT_STOCK_INVALID);
        }
    }

    /**
     * 校验商品货号是否已被其他商品使用。
     *
     * @param productSn 商品货号
     * @param excludedProductId 更新时排除的商品主键，创建时传 null
     */
    private void checkProductSnExists(String productSn, Long excludedProductId) {
        PmsProductExample example = new PmsProductExample();
        PmsProductExample.Criteria criteria = example.createCriteria();
        criteria.andProductSnEqualTo(productSn);

        if (excludedProductId != null) {
            criteria.andIdNotEqualTo(excludedProductId);
        }

        long count = pmsProductMapper.countByExample(example);
        if (count > 0) {
            throw new ApiException(ProductErrorCode.PRODUCT_SN_EXISTS);
        }
    }
}
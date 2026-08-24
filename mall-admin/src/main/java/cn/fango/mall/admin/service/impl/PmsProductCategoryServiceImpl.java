package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.ProductCategoryErrorCode;
import cn.fango.mall.admin.dto.ProductCategoryCreateRequest;
import cn.fango.mall.admin.dto.ProductCategoryUpdateRequest;
import cn.fango.mall.admin.service.PmsProductCategoryService;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsProductCategoryMapper;
import cn.fango.mall.mbg.model.PmsProductCategory;
import cn.fango.mall.mbg.model.PmsProductCategoryExample;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品分类管理服务实现。
 */
@Service
public class PmsProductCategoryServiceImpl implements PmsProductCategoryService {

    private final PmsProductCategoryMapper pmsProductCategoryMapper;

    /**
     * 创建商品分类管理服务。
     *
     * @param pmsProductCategoryMapper 商品分类数据访问对象
     */
    public PmsProductCategoryServiceImpl(PmsProductCategoryMapper pmsProductCategoryMapper) {
        this.pmsProductCategoryMapper = pmsProductCategoryMapper;
    }

    /**
     * 查询指定商品分类，不存在时抛出分类不存在业务异常。
     *
     * @param id 商品分类主键
     * @return 商品分类详情
     */
    @Override
    public PmsProductCategory getCategory(Long id) {
        PmsProductCategory category = pmsProductCategoryMapper.selectByPrimaryKey(id);
        if (category == null) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_NOT_FOUND);
        }

        return category;
    }

    /**
     * 创建商品分类，并校验一级、二级分类的父子层级关系。
     *
     * @param request 创建商品分类请求
     * @return 新创建商品分类的主键
     */
    @Override
    public Long createCategory(ProductCategoryCreateRequest request) {
        validateCreateRequest(request);

        Long parentId = request.parentId() == null ? 0L : request.parentId();
        Integer sort = request.sort() == null ? 0 : request.sort();
        Byte showStatus = request.showStatus() == null ? (byte) 1 : request.showStatus();

        PmsProductCategory category = new PmsProductCategory();
        category.setParentId(parentId);
        category.setName(request.name());
        category.setLevel(request.level());
        category.setSort(sort);
        category.setShowStatus(showStatus);

        int count = pmsProductCategoryMapper.insertSelective(category);
        if (count != 1 || category.getId() == null) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_CREATE_FAILED);
        }

        return category.getId();
    }

    /**
     * 查询全部商品分类，先按层级升序，再按排序值和主键降序排列。
     *
     * @return 商品分类列表
     */
    @Override
    public List<PmsProductCategory> listCategories() {
        PmsProductCategoryExample example = new PmsProductCategoryExample();
        example.setOrderByClause("level asc, sort desc, id desc");

        return pmsProductCategoryMapper.selectByExample(example);
    }

    /**
     * 更新指定商品分类的名称、排序值和显示状态。
     * 分类层级及父分类在创建后保持不变。
     *
     * @param id 商品分类主键
     * @param request 更新商品分类请求
     * @return 是否更新成功
     */
    @Override
    public boolean updateCategory(Long id, ProductCategoryUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_NAME_REQUIRED);
        }

        PmsProductCategory category = new PmsProductCategory();
        category.setId(id);
        category.setName(request.name());
        category.setSort(request.sort());
        category.setShowStatus(request.showStatus());

        int count = pmsProductCategoryMapper.updateByPrimaryKeySelective(category);
        if (count == 0) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_NOT_FOUND);
        }
        if (count != 1) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_UPDATE_FAILED);
        }

        return true;
    }

    /**
     * 删除指定商品分类。
     * 存在子分类时拒绝删除，避免形成无父分类的二级分类。
     *
     * @param id 商品分类主键
     * @return 是否删除成功
     */
    @Override
    public boolean deleteCategory(Long id) {
        getCategory(id);

        PmsProductCategoryExample childExample = new PmsProductCategoryExample();
        childExample.createCriteria().andParentIdEqualTo(id);

        long childCount = pmsProductCategoryMapper.countByExample(childExample);
        if (childCount > 0) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_HAS_CHILDREN);
        }

        int count = pmsProductCategoryMapper.deleteByPrimaryKey(id);
        if (count != 1) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_DELETE_FAILED);
        }

        return true;
    }

    /**
     * 校验创建商品分类请求及父子层级关系。
     *
     * @param request 创建商品分类请求
     */
    private void validateCreateRequest(ProductCategoryCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_NAME_REQUIRED);
        }

        Byte level = request.level();
        if (level == null || (level != 0 && level != 1)) {
            throw new ApiException(ProductCategoryErrorCode.CATEGORY_LEVEL_INVALID);
        }

        Long parentId = request.parentId() == null ? 0L : request.parentId();
        if (level == 0) {
            if (parentId != 0) {
                throw new ApiException(ProductCategoryErrorCode.PARENT_CATEGORY_LEVEL_INVALID);
            }
            return;
        }

        if (parentId <= 0) {
            throw new ApiException(ProductCategoryErrorCode.PARENT_CATEGORY_NOT_FOUND);
        }

        PmsProductCategory parentCategory = pmsProductCategoryMapper.selectByPrimaryKey(parentId);
        if (parentCategory == null) {
            throw new ApiException(ProductCategoryErrorCode.PARENT_CATEGORY_NOT_FOUND);
        }
        if (parentCategory.getLevel() == null || parentCategory.getLevel() != 0) {
            throw new ApiException(ProductCategoryErrorCode.PARENT_CATEGORY_LEVEL_INVALID);
        }
    }
}
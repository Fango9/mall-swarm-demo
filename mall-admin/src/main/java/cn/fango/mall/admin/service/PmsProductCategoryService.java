package cn.fango.mall.admin.service;

import cn.fango.mall.admin.dto.ProductCategoryCreateRequest;
import cn.fango.mall.admin.dto.ProductCategoryUpdateRequest;
import cn.fango.mall.mbg.model.PmsProductCategory;

import java.util.List;

/**
 * 商品分类管理服务。
 */
public interface PmsProductCategoryService {

    /**
     * 根据主键查询商品分类详情。
     *
     * @param id 商品分类主键
     * @return 商品分类详情
     */
    PmsProductCategory getCategory(Long id);

    /**
     * 创建商品分类。
     *
     * @param request 创建商品分类请求
     * @return 新创建商品分类的主键
     */
    Long createCategory(ProductCategoryCreateRequest request);

    /**
     * 查询全部商品分类。
     *
     * @return 按层级、排序和主键排序的商品分类列表
     */
    List<PmsProductCategory> listCategories();

    /**
     * 更新商品分类。
     *
     * @param id 商品分类主键
     * @param request 更新商品分类请求
     * @return 是否更新成功
     */
    boolean updateCategory(Long id, ProductCategoryUpdateRequest request);

    /**
     * 删除商品分类。
     *
     * @param id 商品分类主键
     * @return 是否删除成功
     */
    boolean deleteCategory(Long id);
}
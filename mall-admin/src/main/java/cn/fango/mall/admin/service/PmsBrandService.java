package cn.fango.mall.admin.service;

import cn.fango.mall.admin.dto.BrandCreateRequest;
import cn.fango.mall.admin.dto.BrandUpdateRequest;
import cn.fango.mall.mbg.model.PmsBrand;

import java.util.List;

/**
 * 品牌管理服务。
 */
public interface PmsBrandService {

    /**
     * 根据主键查询品牌详情。
     *
     * @param id 品牌主键
     * @return 品牌详情
     */
    PmsBrand getBrand(Long id);

    /**
     * 创建品牌。
     *
     * @param request 创建品牌请求
     * @return 新创建品牌的主键
     */
    Long createBrand(BrandCreateRequest request);

    /**
     * 分页查询品牌。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页记录数
     * @return 当前页品牌列表
     */
    List<PmsBrand> listBrands(Integer pageNum, Integer pageSize);

    /**
     * 更新品牌。
     *
     * @param id 品牌主键
     * @param request 更新品牌请求
     * @return 是否更新成功
     */
    boolean updateBrand(Long id, BrandUpdateRequest request);

    /**
     * 删除品牌。
     *
     * @param id 品牌主键
     * @return 是否删除成功
     */
    boolean deleteBrand(Long id);

}
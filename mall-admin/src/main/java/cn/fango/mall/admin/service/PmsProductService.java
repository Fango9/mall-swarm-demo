package cn.fango.mall.admin.service;

import cn.fango.mall.admin.dto.ProductCreateRequest;
import cn.fango.mall.admin.dto.ProductUpdateRequest;
import cn.fango.mall.mbg.model.PmsProduct;

import java.util.List;

/**
 * 商品管理服务。
 */
public interface PmsProductService {

    /**
     * 根据主键查询商品详情。
     *
     * @param id 商品主键
     * @return 商品详情
     */
    PmsProduct getProduct(Long id);

    /**
     * 创建商品。
     *
     * @param request 创建商品请求
     * @return 新创建商品的主键
     */
    Long createProduct(ProductCreateRequest request);

    /**
     * 分页查询未删除商品。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页记录数
     * @return 当前页商品列表
     */
    List<PmsProduct> listProducts(Integer pageNum, Integer pageSize);

    /**
     * 更新商品。
     *
     * @param id 商品主键
     * @param request 更新商品请求
     * @return 是否更新成功
     */
    boolean updateProduct(Long id, ProductUpdateRequest request);

    /**
     * 软删除商品。
     *
     * @param id 商品主键
     * @return 是否删除成功
     */
    boolean deleteProduct(Long id);

}
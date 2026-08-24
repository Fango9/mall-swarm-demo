package cn.fango.mall.admin.service;

import cn.fango.mall.admin.dto.SkuStockCreateRequest;
import cn.fango.mall.admin.dto.SkuStockUpdateRequest;
import cn.fango.mall.mbg.model.PmsSkuStock;

import java.util.List;

/**
 * 商品 SKU 管理服务。
 */
public interface PmsSkuStockService {

    /**
     * 查询指定商品下的指定 SKU。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @return SKU 详情
     */
    PmsSkuStock getSku(Long productId, Long skuId);

    /**
     * 查询指定商品下的全部 SKU。
     *
     * @param productId 商品主键
     * @return SKU 列表
     */
    List<PmsSkuStock> listSkus(Long productId);

    /**
     * 为指定商品创建 SKU。
     *
     * @param productId 商品主键
     * @param request 创建 SKU 请求
     * @return 新创建 SKU 的主键
     */
    Long createSku(Long productId, SkuStockCreateRequest request);

    /**
     * 更新指定商品下的指定 SKU。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @param request 更新 SKU 请求
     * @return 是否更新成功
     */
    boolean updateSku(Long productId, Long skuId, SkuStockUpdateRequest request);

    /**
     * 删除指定商品下的指定 SKU。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @return 是否删除成功
     */
    boolean deleteSku(Long productId, Long skuId);
}
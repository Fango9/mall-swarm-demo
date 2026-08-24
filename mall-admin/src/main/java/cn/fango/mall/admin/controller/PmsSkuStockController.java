package cn.fango.mall.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.fango.mall.admin.dto.SkuStockCreateRequest;
import cn.fango.mall.admin.dto.SkuStockUpdateRequest;
import cn.fango.mall.admin.service.PmsSkuStockService;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.mbg.model.PmsSkuStock;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台商品 SKU 管理接口。
 */
@RestController
@RequestMapping("/admin/products/{productId}/skus")
@SaCheckRole("ADMIN")
public class PmsSkuStockController {

    private final PmsSkuStockService pmsSkuStockService;

    /**
     * 创建后台商品 SKU 管理接口。
     *
     * @param pmsSkuStockService 商品 SKU 管理服务
     */
    public PmsSkuStockController(PmsSkuStockService pmsSkuStockService) {
        this.pmsSkuStockService = pmsSkuStockService;
    }

    /**
     * 查询指定商品下的指定 SKU。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @return 统一响应中的 SKU 详情
     */
    @GetMapping("/{skuId}")
    public CommonResult<PmsSkuStock> getSku(
            @PathVariable Long productId,
            @PathVariable Long skuId
    ) {
        PmsSkuStock skuStock = pmsSkuStockService.getSku(productId, skuId);
        return CommonResult.success(skuStock);
    }

    /**
     * 查询指定商品下的全部 SKU。
     *
     * @param productId 商品主键
     * @return 统一响应中的 SKU 列表
     */
    @GetMapping
    public CommonResult<List<PmsSkuStock>> listSkus(@PathVariable Long productId) {
        List<PmsSkuStock> skuStocks = pmsSkuStockService.listSkus(productId);
        return CommonResult.success(skuStocks);
    }

    /**
     * 为指定商品创建 SKU。
     *
     * @param productId 商品主键
     * @param request 创建 SKU 请求
     * @return 统一响应中的新 SKU 主键
     */
    @PostMapping
    public CommonResult<Long> createSku(
            @PathVariable Long productId,
            @RequestBody SkuStockCreateRequest request
    ) {
        Long skuId = pmsSkuStockService.createSku(productId, request);
        return CommonResult.success(skuId);
    }

    /**
     * 更新指定商品下的指定 SKU。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @param request 更新 SKU 请求
     * @return 统一响应中的更新结果
     */
    @PutMapping("/{skuId}")
    public CommonResult<Boolean> updateSku(
            @PathVariable Long productId,
            @PathVariable Long skuId,
            @RequestBody SkuStockUpdateRequest request
    ) {
        boolean updated = pmsSkuStockService.updateSku(productId, skuId, request);
        return CommonResult.success(updated);
    }

    /**
     * 删除指定商品下的指定 SKU。
     *
     * @param productId 商品主键
     * @param skuId SKU 主键
     * @return 统一响应中的删除结果
     */
    @DeleteMapping("/{skuId}")
    public CommonResult<Boolean> deleteSku(
            @PathVariable Long productId,
            @PathVariable Long skuId
    ) {
        boolean deleted = pmsSkuStockService.deleteSku(productId, skuId);
        return CommonResult.success(deleted);
    }
}
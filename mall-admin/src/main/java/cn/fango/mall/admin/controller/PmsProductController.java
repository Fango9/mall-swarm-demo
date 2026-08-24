package cn.fango.mall.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.fango.mall.admin.dto.ProductCreateRequest;
import cn.fango.mall.admin.dto.ProductUpdateRequest;
import cn.fango.mall.admin.service.PmsProductService;
import cn.fango.mall.common.api.CommonPage;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.mbg.model.PmsProduct;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台商品管理接口。
 */
@RestController
@RequestMapping("/admin/products")
@SaCheckRole("ADMIN")
public class PmsProductController {

    private final PmsProductService pmsProductService;

    /**
     * 创建后台商品管理接口。
     *
     * @param pmsProductService 商品管理服务
     */
    public PmsProductController(PmsProductService pmsProductService) {
        this.pmsProductService = pmsProductService;
    }

    /**
     * 查询商品详情。
     *
     * @param id 商品主键
     * @return 统一响应中的商品详情
     */
    @GetMapping("/{id}")
    public CommonResult<PmsProduct> getProduct(@PathVariable Long id) {
        PmsProduct product = pmsProductService.getProduct(id);
        return CommonResult.success(product);
    }

    /**
     * 创建商品。
     *
     * @param request 创建商品请求
     * @return 统一响应中的新商品主键
     */
    @PostMapping
    public CommonResult<Long> createProduct(@RequestBody ProductCreateRequest request) {
        Long productId = pmsProductService.createProduct(request);
        return CommonResult.success(productId);
    }

    /**
     * 分页查询未删除商品。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页记录数
     * @return 统一响应中的商品分页数据
     */
    @GetMapping
    public CommonResult<CommonPage<PmsProduct>> listProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        List<PmsProduct> products = pmsProductService.listProducts(pageNum, pageSize);
        CommonPage<PmsProduct> page = CommonPage.restPage(products);

        return CommonResult.success(page);
    }

    /**
     * 更新商品。
     *
     * @param id 商品主键
     * @param request 更新商品请求
     * @return 统一响应中的更新结果
     */
    @PutMapping("/{id}")
    public CommonResult<Boolean> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest request
    ) {
        boolean updated = pmsProductService.updateProduct(id, request);
        return CommonResult.success(updated);
    }

    /**
     * 软删除商品。
     *
     * @param id 商品主键
     * @return 统一响应中的删除结果
     */
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> deleteProduct(@PathVariable Long id) {
        boolean deleted = pmsProductService.deleteProduct(id);
        return CommonResult.success(deleted);
    }
}
package cn.fango.mall.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.fango.mall.admin.dto.ProductCategoryCreateRequest;
import cn.fango.mall.admin.dto.ProductCategoryUpdateRequest;
import cn.fango.mall.admin.service.PmsProductCategoryService;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.mbg.model.PmsProductCategory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台商品分类管理接口。
 */
@RestController
@RequestMapping("/admin/product-categories")
@SaCheckRole("ADMIN")
public class PmsProductCategoryController {

    private final PmsProductCategoryService pmsProductCategoryService;

    /**
     * 创建后台商品分类管理接口。
     *
     * @param pmsProductCategoryService 商品分类管理服务
     */
    public PmsProductCategoryController(PmsProductCategoryService pmsProductCategoryService) {
        this.pmsProductCategoryService = pmsProductCategoryService;
    }

    /**
     * 查询商品分类详情。
     *
     * @param id 商品分类主键
     * @return 统一响应中的商品分类详情
     */
    @GetMapping("/{id}")
    public CommonResult<PmsProductCategory> getCategory(@PathVariable Long id) {
        PmsProductCategory category = pmsProductCategoryService.getCategory(id);
        return CommonResult.success(category);
    }

    /**
     * 创建商品分类。
     *
     * @param request 创建商品分类请求
     * @return 统一响应中的新商品分类主键
     */
    @PostMapping
    public CommonResult<Long> createCategory(@RequestBody ProductCategoryCreateRequest request) {
        Long categoryId = pmsProductCategoryService.createCategory(request);
        return CommonResult.success(categoryId);
    }

    /**
     * 查询全部商品分类。
     *
     * @return 统一响应中的商品分类列表
     */
    @GetMapping
    public CommonResult<List<PmsProductCategory>> listCategories() {
        List<PmsProductCategory> categories = pmsProductCategoryService.listCategories();
        return CommonResult.success(categories);
    }

    /**
     * 更新商品分类。
     *
     * @param id 商品分类主键
     * @param request 更新商品分类请求
     * @return 统一响应中的更新结果
     */
    @PutMapping("/{id}")
    public CommonResult<Boolean> updateCategory(
            @PathVariable Long id,
            @RequestBody ProductCategoryUpdateRequest request
    ) {
        boolean updated = pmsProductCategoryService.updateCategory(id, request);
        return CommonResult.success(updated);
    }

    /**
     * 删除商品分类。
     *
     * @param id 商品分类主键
     * @return 统一响应中的删除结果
     */
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> deleteCategory(@PathVariable Long id) {
        boolean deleted = pmsProductCategoryService.deleteCategory(id);
        return CommonResult.success(deleted);
    }
}
package cn.fango.mall.admin.controller;

import cn.fango.mall.admin.dto.BrandCreateRequest;
import cn.fango.mall.admin.dto.BrandUpdateRequest;
import cn.fango.mall.admin.service.PmsBrandService;
import cn.fango.mall.common.api.CommonPage;
import cn.fango.mall.common.api.CommonResult;
import cn.fango.mall.mbg.model.PmsBrand;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台品牌管理接口。
 */
@RestController
@RequestMapping("/admin/brands")
public class PmsBrandController {

    private final PmsBrandService pmsBrandService;

    /**
     * 创建后台品牌管理接口。
     *
     * @param pmsBrandService 品牌管理服务
     */
    public PmsBrandController(PmsBrandService pmsBrandService) {
        this.pmsBrandService = pmsBrandService;
    }

    /**
     * 查询品牌详情。
     *
     * @param id 品牌主键
     * @return 统一响应中的品牌详情
     */
    @GetMapping("/{id}")
    public CommonResult<PmsBrand> getBrand(@PathVariable Long id) {
        PmsBrand brand = pmsBrandService.getBrand(id);
        return CommonResult.success(brand);
    }

    /**
     * 创建品牌。
     *
     * @param request 创建品牌请求
     * @return 统一响应中的新品牌主键
     */
    @PostMapping
    public CommonResult<Long> createBrand(@RequestBody BrandCreateRequest request) {
        Long brandId = pmsBrandService.createBrand(request);
        return CommonResult.success(brandId);
    }

    /**
     * 分页查询品牌。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页记录数
     * @return 统一响应中的品牌分页数据
     */
    @GetMapping
    public CommonResult<CommonPage<PmsBrand>> listBrands(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        List<PmsBrand> brands = pmsBrandService.listBrands(pageNum, pageSize);
        CommonPage<PmsBrand> page = CommonPage.restPage(brands);

        return CommonResult.success(page);
    }

    /**
     * 更新品牌。
     *
     * @param id 品牌主键
     * @param request 更新品牌请求
     * @return 统一响应中的更新结果
     */
    @PutMapping("/{id}")
    public CommonResult<Boolean> updateBrand(
            @PathVariable Long id,
            @RequestBody BrandUpdateRequest request
    ) {
        boolean updated = pmsBrandService.updateBrand(id, request);
        return CommonResult.success(updated);
    }

    /**
     * 删除品牌。
     *
     * @param id 品牌主键
     * @return 统一响应中的删除结果
     */
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> deleteBrand(@PathVariable Long id) {
        boolean deleted = pmsBrandService.deleteBrand(id);
        return CommonResult.success(deleted);
    }

}
package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.BrandErrorCode;
import cn.fango.mall.admin.dto.BrandCreateRequest;
import cn.fango.mall.admin.dto.BrandUpdateRequest;
import cn.fango.mall.admin.service.PmsBrandService;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsBrandMapper;
import cn.fango.mall.mbg.model.PmsBrand;
import cn.fango.mall.mbg.model.PmsBrandExample;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 品牌管理服务实现。
 */
@Service
public class PmsBrandServiceImpl implements PmsBrandService {

    private final PmsBrandMapper pmsBrandMapper;

    /**
     * 创建品牌管理服务。
     *
     * @param pmsBrandMapper 品牌数据访问对象
     */
    public PmsBrandServiceImpl(PmsBrandMapper pmsBrandMapper) {
        this.pmsBrandMapper = pmsBrandMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PmsBrand getBrand(Long id) {
        PmsBrand brand = pmsBrandMapper.selectByPrimaryKey(id);
        if (brand == null) {
            throw new ApiException(BrandErrorCode.BRAND_NOT_FOUND);
        }

        return brand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long createBrand(BrandCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new ApiException(BrandErrorCode.BRAND_NAME_REQUIRED);
        }

        Integer sort = request.sort() == null ? 0 : request.sort();
        Byte factoryStatus = request.factoryStatus() == null ? (byte) 0 : request.factoryStatus();
        Byte showStatus = request.showStatus() == null ? (byte) 1 : request.showStatus();

        PmsBrand brand = new PmsBrand();
        brand.setName(request.name());
        brand.setFirstLetter(request.firstLetter());
        brand.setSort(sort);
        brand.setFactoryStatus(factoryStatus);
        brand.setShowStatus(showStatus);
        brand.setProductCount(0);
        brand.setProductCommentCount(0);
        brand.setLogo(request.logo());
        brand.setBigPic(request.bigPic());
        brand.setBrandStory(request.brandStory());

        int count = pmsBrandMapper.insertSelective(brand);
        if (count != 1 || brand.getId() == null) {
            throw new ApiException(BrandErrorCode.BRAND_CREATE_FAILED);
        }

        return brand.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PmsBrand> listBrands(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        PmsBrandExample example = new PmsBrandExample();
        example.setOrderByClause("sort desc, id desc");

        return pmsBrandMapper.selectByExampleWithBLOBs(example);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateBrand(Long id, BrandUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new ApiException(BrandErrorCode.BRAND_NAME_REQUIRED);
        }

        PmsBrand brand = new PmsBrand();
        brand.setId(id);
        brand.setName(request.name());
        brand.setFirstLetter(request.firstLetter());
        brand.setSort(request.sort());
        brand.setFactoryStatus(request.factoryStatus());
        brand.setShowStatus(request.showStatus());
        brand.setLogo(request.logo());
        brand.setBigPic(request.bigPic());
        brand.setBrandStory(request.brandStory());

        int count = pmsBrandMapper.updateByPrimaryKeySelective(brand);
        if (count == 0) {
            throw new ApiException(BrandErrorCode.BRAND_NOT_FOUND);
        }
        if (count != 1) {
            throw new ApiException(BrandErrorCode.BRAND_UPDATE_FAILED);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteBrand(Long id) {
        int count = pmsBrandMapper.deleteByPrimaryKey(id);
        if (count == 0) {
            throw new ApiException(BrandErrorCode.BRAND_NOT_FOUND);
        }
        if (count != 1) {
            throw new ApiException(BrandErrorCode.BRAND_DELETE_FAILED);
        }

        return true;
    }

}
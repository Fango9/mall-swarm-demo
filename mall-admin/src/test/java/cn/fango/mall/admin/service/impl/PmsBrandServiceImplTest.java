package cn.fango.mall.admin.service.impl;

import cn.fango.mall.admin.api.BrandErrorCode;
import cn.fango.mall.admin.dto.BrandCreateRequest;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsBrandMapper;
import cn.fango.mall.mbg.model.PmsBrand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 品牌服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PmsBrandServiceImplTest {

    @Mock
    private PmsBrandMapper pmsBrandMapper;

    @InjectMocks
    private PmsBrandServiceImpl pmsBrandService;

    /**
     * 查询到品牌时应直接返回该品牌。
     */
    @Test
    void getBrandReturnsBrandWhenFound() {
        PmsBrand expectedBrand = new PmsBrand();
        expectedBrand.setId(1L);
        expectedBrand.setName("Apple");

        when(pmsBrandMapper.selectByPrimaryKey(1L)).thenReturn(expectedBrand);

        PmsBrand actualBrand = pmsBrandService.getBrand(1L);

        assertThat(actualBrand).isSameAs(expectedBrand);
    }

    /**
     * 查询不到品牌时应抛出品牌不存在异常。
     */
    @Test
    void getBrandThrowsExceptionWhenNotFound() {
        when(pmsBrandMapper.selectByPrimaryKey(1L)).thenReturn(null);

        assertThatThrownBy(() -> pmsBrandService.getBrand(1L))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
                );
    }

    /**
     * 创建品牌时应补齐默认值并返回数据库生成的主键。
     */
    @Test
    void createBrandAppliesDefaultsAndReturnsGeneratedId() {
        BrandCreateRequest request = new BrandCreateRequest(
                "Apple",
                "A",
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(pmsBrandMapper.insertSelective(any(PmsBrand.class))).thenAnswer(invocation -> {
            PmsBrand brand = invocation.getArgument(0);
            brand.setId(1L);
            return 1;
        });

        Long brandId = pmsBrandService.createBrand(request);

        ArgumentCaptor<PmsBrand> captor = ArgumentCaptor.forClass(PmsBrand.class);
        org.mockito.Mockito.verify(pmsBrandMapper).insertSelective(captor.capture());

        PmsBrand savedBrand = captor.getValue();
        assertThat(brandId).isEqualTo(1L);
        assertThat(savedBrand.getName()).isEqualTo("Apple");
        assertThat(savedBrand.getSort()).isZero();
        assertThat(savedBrand.getFactoryStatus()).isZero();
        assertThat(savedBrand.getShowStatus()).isEqualTo((byte) 1);
        assertThat(savedBrand.getProductCount()).isZero();
        assertThat(savedBrand.getProductCommentCount()).isZero();
    }
}
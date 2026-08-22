package cn.fango.mall.admin.service;

import cn.fango.mall.admin.dto.BrandCreateRequest;
import cn.fango.mall.admin.dto.BrandUpdateRequest;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.mbg.mapper.PmsBrandMapper;
import cn.fango.mall.mbg.model.PmsBrand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 品牌服务真实 MySQL 集成测试。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.cloud.nacos.discovery.enabled=false"
)
class PmsBrandMySqlIntegrationTest {

    @Autowired
    private PmsBrandService pmsBrandService;

    @Autowired
    private PmsBrandMapper pmsBrandMapper;

    private Long createdBrandId;

    /**
     * 清理测试创建的品牌，避免污染本地开发数据库。
     */
    @AfterEach
    void cleanUp() {
        if (createdBrandId != null) {
            pmsBrandMapper.deleteByPrimaryKey(createdBrandId);
        }
    }

    /**
     * 在真实 MySQL 中验证品牌的创建、查询、修改和删除。
     */
    @Test
    void createUpdateGetAndDeleteBrandAgainstMySql() {
        String brandName = "integration-brand-" + UUID.randomUUID();

        BrandCreateRequest createRequest = new BrandCreateRequest(
                brandName,
                "I",
                10,
                (byte) 0,
                (byte) 1,
                null,
                null,
                "MySQL 集成测试品牌"
        );

        createdBrandId = pmsBrandService.createBrand(createRequest);

        PmsBrand createdBrand = pmsBrandService.getBrand(createdBrandId);
        assertThat(createdBrand.getName()).isEqualTo(brandName);
        assertThat(createdBrand.getProductCount()).isZero();
        assertThat(createdBrand.getProductCommentCount()).isZero();

        BrandUpdateRequest updateRequest = new BrandUpdateRequest(
                brandName + "-updated",
                "U",
                20,
                null,
                (byte) 0,
                null,
                null,
                null
        );

        boolean updated = pmsBrandService.updateBrand(createdBrandId, updateRequest);

        PmsBrand updatedBrand = pmsBrandService.getBrand(createdBrandId);
        assertThat(updated).isTrue();
        assertThat(updatedBrand.getName()).isEqualTo(brandName + "-updated");
        assertThat(updatedBrand.getSort()).isEqualTo(20);
        assertThat(updatedBrand.getShowStatus()).isZero();
        assertThat(updatedBrand.getProductCount()).isZero();
        assertThat(updatedBrand.getProductCommentCount()).isZero();

        boolean deleted = pmsBrandService.deleteBrand(createdBrandId);
        createdBrandId = null;

        assertThat(deleted).isTrue();
        assertThatThrownBy(() -> pmsBrandService.getBrand(updatedBrand.getId()))
                .isInstanceOf(ApiException.class);
    }
}
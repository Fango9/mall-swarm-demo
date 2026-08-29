package cn.fango.mall.admin.service;

import cn.fango.mall.admin.api.StockReservationStatus;
import cn.fango.mall.common.exception.ApiException;
import cn.fango.mall.common.stock.StockReleaseRequest;
import cn.fango.mall.common.stock.StockReservationItem;
import cn.fango.mall.common.stock.StockReservationRequest;
import cn.fango.mall.mbg.mapper.PmsProductMapper;
import cn.fango.mall.mbg.mapper.PmsSkuStockMapper;
import cn.fango.mall.mbg.mapper.PmsStockReservationMapper;
import cn.fango.mall.mbg.model.PmsProduct;
import cn.fango.mall.mbg.model.PmsSkuStock;
import cn.fango.mall.mbg.model.PmsStockReservation;
import cn.fango.mall.mbg.model.PmsStockReservationExample;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 库存预占服务真实 MySQL 集成测试。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false"
        }
)
class StockReservationMySqlIntegrationTest {

    /**
     * 待测试的库存预占服务。
     */
    @Autowired
    private StockReservationService stockReservationService;

    /**
     * 商品数据访问对象，用于准备和清理测试商品。
     */
    @Autowired
    private PmsProductMapper pmsProductMapper;

    /**
     * SKU 数据访问对象，用于验证锁定库存。
     */
    @Autowired
    private PmsSkuStockMapper pmsSkuStockMapper;

    /**
     * 库存预占记录数据访问对象，用于验证幂等状态。
     */
    @Autowired
    private PmsStockReservationMapper pmsStockReservationMapper;

    /**
     * 当前测试创建的商品主键。
     */
    private Long productId;

    /**
     * 当前测试创建的 SKU 主键。
     */
    private Long skuId;

    /**
     * 每个测试前准备库存为 5 的独立 SKU。
     */
    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        PmsProduct product = new PmsProduct();
        product.setProductCategoryId(1L);
        product.setName("库存预占测试商品-" + suffix);
        product.setProductSn("stock-test-" + suffix);

        int productInserted = pmsProductMapper.insertSelective(product);
        assertThat(productInserted).isEqualTo(1);
        productId = product.getId();

        PmsSkuStock skuStock = new PmsSkuStock();
        skuStock.setProductId(productId);
        skuStock.setSkuCode("stock-test-sku-" + suffix);
        skuStock.setPrice(new BigDecimal("99.00"));
        skuStock.setStock(5);
        skuStock.setLockStock(0);

        int skuInserted = pmsSkuStockMapper.insertSelective(skuStock);
        assertThat(skuInserted).isEqualTo(1);
        skuId = skuStock.getId();
    }

    /**
     * 每个测试后清理预占记录、SKU 和商品，避免污染本地开发数据库。
     */
    @AfterEach
    void cleanUp() {
        if (skuId != null) {
            PmsStockReservationExample reservationExample =
                    new PmsStockReservationExample();
            reservationExample.createCriteria().andSkuIdEqualTo(skuId);
            pmsStockReservationMapper.deleteByExample(reservationExample);

            pmsSkuStockMapper.deleteByPrimaryKey(skuId);
        }

        if (productId != null) {
            pmsProductMapper.deleteByPrimaryKey(productId);
        }
    }

    /**
     * 验证库存预占、预占重试、库存不足、释放和重复释放。
     */
    @Test
    void reserveRetryInsufficientAndReleaseAgainstMySql() {
        String reservationNo = "reservation-" + UUID.randomUUID();
        StockReservationRequest reservationRequest =
                new StockReservationRequest(
                        reservationNo,
                        List.of(new StockReservationItem(skuId, 2))
                );

        Date beforeFirstReservation = new Date();

        boolean firstReserved =
                stockReservationService.reserveStock(reservationRequest);
        PmsSkuStock firstLockedSku =
                pmsSkuStockMapper.selectByPrimaryKey(skuId);

        assertThat(firstReserved).isTrue();
        assertThat(firstLockedSku.getLockStock()).isEqualTo(2);

        boolean retriedReserved =
                stockReservationService.reserveStock(reservationRequest);
        PmsSkuStock retriedLockedSku =
                pmsSkuStockMapper.selectByPrimaryKey(skuId);
        List<PmsStockReservation> reservations =
                findReservations(reservationNo);

        assertThat(retriedReserved).isTrue();
        assertThat(retriedLockedSku.getLockStock()).isEqualTo(2);
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getStatus())
                .isEqualTo(StockReservationStatus.LOCKED.name());
        assertThat(reservations.get(0).getExpireAt())
                .isAfter(beforeFirstReservation);

        StockReservationRequest insufficientRequest =
                new StockReservationRequest(
                        "insufficient-" + UUID.randomUUID(),
                        List.of(new StockReservationItem(skuId, 4))
                );

        assertThatThrownBy(
                () -> stockReservationService.reserveStock(insufficientRequest)
        ).isInstanceOf(ApiException.class);

        PmsSkuStock insufficientCheckedSku =
                pmsSkuStockMapper.selectByPrimaryKey(skuId);
        assertThat(insufficientCheckedSku.getLockStock()).isEqualTo(2);

        boolean firstReleased = stockReservationService.releaseStock(
                new StockReleaseRequest(reservationNo)
        );
        PmsSkuStock firstReleasedSku =
                pmsSkuStockMapper.selectByPrimaryKey(skuId);

        assertThat(firstReleased).isTrue();
        assertThat(firstReleasedSku.getLockStock()).isZero();
        assertThat(findReservations(reservationNo).get(0).getStatus())
                .isEqualTo(StockReservationStatus.RELEASED.name());

        boolean retriedReleased = stockReservationService.releaseStock(
                new StockReleaseRequest(reservationNo)
        );
        PmsSkuStock retriedReleasedSku =
                pmsSkuStockMapper.selectByPrimaryKey(skuId);

        assertThat(retriedReleased).isTrue();
        assertThat(retriedReleasedSku.getLockStock()).isZero();
    }

    /**
     * 查询指定预占编号对应的全部预占记录。
     *
     * @param reservationNo 库存预占编号
     * @return 匹配的库存预占记录列表
     */
    private List<PmsStockReservation> findReservations(String reservationNo) {
        PmsStockReservationExample example = new PmsStockReservationExample();
        example.createCriteria().andReservationNoEqualTo(reservationNo);

        return pmsStockReservationMapper.selectByExample(example);
    }
}
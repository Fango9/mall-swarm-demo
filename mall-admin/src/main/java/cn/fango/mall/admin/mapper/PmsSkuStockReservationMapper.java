package cn.fango.mall.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SKU 库存预占的原子数据访问对象。
 */
@Mapper
public interface PmsSkuStockReservationMapper {

    /**
     * 在可用库存充足时原子增加锁定库存。
     *
     * @param skuId SKU 主键
     * @param quantity 本次预占数量
     * @return 更新行数；1 表示预占成功，0 表示 SKU 不存在或可用库存不足
     */
    int lockStock(
            @Param("skuId") Long skuId,
            @Param("quantity") Integer quantity
    );

    /**
     * 在锁定库存充足时原子减少锁定库存。
     *
     * @param skuId SKU 主键
     * @param quantity 本次释放数量
     * @return 更新行数；1 表示释放成功，0 表示 SKU 不存在或锁定库存异常
     */
    int releaseStock(
            @Param("skuId") Long skuId,
            @Param("quantity") Integer quantity
    );
}
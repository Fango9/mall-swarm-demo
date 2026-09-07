package cn.fango.mall.admin.mapper;

import cn.fango.mall.mbg.model.PmsStockReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 批量写入已经完成 MySQL 锁库存的预占用明细。
     *
     * <p>调用方必须与 {@link #lockStock(Long, Integer)} 处于同一事务；任一行的
     * 唯一键冲突都必须让整个事务回滚，随后由重复消费走幂等校验。</p>
     *
     * @param reservations 同一批次的新预占用明细
     * @return 实际插入行数
     */
    int batchInsertReservations(
            @Param("reservations") List<PmsStockReservation> reservations
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

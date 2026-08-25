package cn.fango.mall.mbg.mapper;

import cn.fango.mall.mbg.model.PmsStockReservation;
import cn.fango.mall.mbg.model.PmsStockReservationExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PmsStockReservationMapper {
    long countByExample(PmsStockReservationExample example);

    int deleteByExample(PmsStockReservationExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PmsStockReservation row);

    int insertSelective(PmsStockReservation row);

    List<PmsStockReservation> selectByExample(PmsStockReservationExample example);

    PmsStockReservation selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PmsStockReservation row, @Param("example") PmsStockReservationExample example);

    int updateByExample(@Param("row") PmsStockReservation row, @Param("example") PmsStockReservationExample example);

    int updateByPrimaryKeySelective(PmsStockReservation row);

    int updateByPrimaryKey(PmsStockReservation row);
}
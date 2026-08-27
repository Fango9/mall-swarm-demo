package cn.fango.mall.mbg.mapper;

import cn.fango.mall.mbg.model.PmsOutboxEvent;
import cn.fango.mall.mbg.model.PmsOutboxEventExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PmsOutboxEventMapper {
    long countByExample(PmsOutboxEventExample example);

    int deleteByExample(PmsOutboxEventExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PmsOutboxEvent row);

    int insertSelective(PmsOutboxEvent row);

    List<PmsOutboxEvent> selectByExampleWithBLOBs(PmsOutboxEventExample example);

    List<PmsOutboxEvent> selectByExample(PmsOutboxEventExample example);

    PmsOutboxEvent selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PmsOutboxEvent row, @Param("example") PmsOutboxEventExample example);

    int updateByExampleWithBLOBs(@Param("row") PmsOutboxEvent row, @Param("example") PmsOutboxEventExample example);

    int updateByExample(@Param("row") PmsOutboxEvent row, @Param("example") PmsOutboxEventExample example);

    int updateByPrimaryKeySelective(PmsOutboxEvent row);

    int updateByPrimaryKeyWithBLOBs(PmsOutboxEvent row);

    int updateByPrimaryKey(PmsOutboxEvent row);
}
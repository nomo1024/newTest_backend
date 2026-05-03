package com.lyc.usercenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyc.usercenter.model.domain.SourceData;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 传感器数据数据库操作
 */
@Mapper
public interface SensorMapper extends BaseMapper<SourceData> {

    /**
     * 动态指定表名插入数据（注意：表名通过 ${table} 注入，需保证来源可信）
     */
    @Insert("INSERT INTO ${table} (col1, collect_time) VALUES (#{data.col1}, #{data.collect_time})")
    @Options(useGeneratedKeys = true, keyProperty = "data.id")
    int insertToTable(@Param("table") String table, @Param("data") SourceData data);

    /**
     * 查询指定表最近的 N 条数据（按 id 降序）
     */
    @Select("SELECT id, col1, collect_time FROM ${table} ORDER BY id desc LIMIT #{limit}")
    List<SourceData> selectRecentFromTable(@Param("table") String table, @Param("limit") int limit);

}





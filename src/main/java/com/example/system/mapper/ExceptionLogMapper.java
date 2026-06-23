package com.example.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.system.entity.ExceptionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异常日志Mapper
 * 
 * @author System
 */
@Mapper
public interface ExceptionLogMapper extends BaseMapper<ExceptionLog> {
}
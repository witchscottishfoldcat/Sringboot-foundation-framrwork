package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.entity.ExceptionLog;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 异常日志服务接口
 * 
 * @author System
 */
public interface ExceptionLogService extends IService<ExceptionLog> {

    /**
     * 记录异常日志
     *
     * @param exception 异常对象
     * @param request  HTTP请求
     * @param traceId   追踪ID
     */
    void recordException(Exception exception, HttpServletRequest request, String traceId);

    /**
     * 记录异常日志（无请求信息）
     *
     * @param exception 异常对象
     * @param traceId   追踪ID
     */
    void recordException(Exception exception, String traceId);
}
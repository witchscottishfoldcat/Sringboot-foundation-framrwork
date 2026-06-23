package com.example.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 异常日志实体
 * 
 * @author System
 */
@Data
@Accessors(chain = true)
@TableName("sys_exception_log")
public class ExceptionLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 追踪ID
     */
    @TableField("trace_id")
    private String traceId;

    /**
     * 异常类型
     */
    @TableField("exception_type")
    private String exceptionType;

    /**
     * 错误码
     */
    @TableField("error_code")
    private Integer errorCode;

    /**
     * 错误消息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 请求URI
     */
    @TableField("request_uri")
    private String requestUri;

    /**
     * 请求方法
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求参数
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 用户ID（如果已登录）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户名（如果已登录）
     */
    @TableField("username")
    private String username;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * User-Agent
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 异常堆栈
     */
    @TableField("stack_trace")
    private String stackTrace;

    /**
     * 是否已处理
     */
    @TableField("handled")
    private Boolean handled;

    /**
     * 处理人
     */
    @TableField("handler")
    private String handler;

    /**
     * 处理备注
     */
    @TableField("handle_note")
    private String handleNote;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
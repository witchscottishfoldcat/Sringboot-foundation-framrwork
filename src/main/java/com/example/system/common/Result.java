package com.example.system.common;

import com.example.system.exception.BaseException;
import com.example.system.util.TimeUtil;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一响应结果封装
 * 
 * @author System
 */
@Data
public class Result<T> {
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 请求追踪ID（可选，用于日志追踪）
     */
    private String traceId;

    public Result() {
        this.timestamp = TimeUtil.nowUtc();
    }

    public Result(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    public Result(Integer code, String message, T data, String traceId) {
        this(code, message, data);
        this.traceId = traceId;
    }

    // ========== 成功响应 ==========

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage());
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message);
    }

    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    // ========== 错误响应 ==========

    /**
     * 错误响应（默认错误）
     */
    public static <T> Result<T> error() {
        return new Result<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    /**
     * 错误响应（自定义消息）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    /**
     * 错误响应（自定义错误码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    /**
     * 错误响应（使用错误码枚举）
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 错误响应（使用错误码枚举和自定义消息）
     */
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message);
    }

    /**
     * 错误响应（来自异常）
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> error(BaseException exception) {
        return new Result<>(exception.getErrorCodeValue(), exception.getMessage(), (T) exception.getData());
    }

    /**
     * 错误响应（来自异常，带追踪ID）
     */
    @SuppressWarnings("unchecked")
    public static <T> Result<T> error(BaseException exception, String traceId) {
        return new Result<>(exception.getErrorCodeValue(), exception.getMessage(), (T) exception.getData(), traceId);
    }

    // ========== 常用业务错误响应 ==========

    /**
     * 参数错误
     */
    public static <T> Result<T> paramError(String message) {
        return error(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 未授权
     */
    public static <T> Result<T> unauthorized(String message) {
        return error(ErrorCode.UNAUTHORIZED, message);
    }

    /**
     * 禁止访问
     */
    public static <T> Result<T> forbidden(String message) {
        return error(ErrorCode.FORBIDDEN, message);
    }

    /**
     * 资源不存在
     */
    public static <T> Result<T> notFound(String message) {
        return error(ErrorCode.NOT_FOUND, message);
    }

    // ========== 判断方法 ==========

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return Integer.valueOf(ErrorCode.SUCCESS.getCode()).equals(this.code);
    }

    /**
     * 是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
    
    /**
     * 设置追踪ID
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
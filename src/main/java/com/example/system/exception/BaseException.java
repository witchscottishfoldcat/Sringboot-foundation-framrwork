package com.example.system.exception;

import com.example.system.common.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 异常基类
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Object data;
    
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = null;
    }
    
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    public BaseException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }
    
    public BaseException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }
    
    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    public BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = null;
    }
    
    public BaseException(ErrorCode errorCode, String message, Throwable cause, Object data) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = data;
    }
    
    /**
     * 获取错误码
     * @return 错误码
     */
    public int getErrorCodeValue() {
        return errorCode.getCode();
    }
    
    /**
     * 获取错误消息
     * @return 错误消息
     */
    @Override
    public String getMessage() {
        return super.getMessage();
    }
    
    /**
     * 获取错误码
     * @return 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * 获取数据
     * @return 数据
     */
    public Object getData() {
        return data;
    }
}
package com.example.system.exception;

import com.example.system.common.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统异常
 * 用于处理系统级异常情况
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemException extends BaseException {
    
    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public SystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public SystemException(ErrorCode errorCode, Object data) {
        super(errorCode, data);
    }
    
    public SystemException(ErrorCode errorCode, String message, Object data) {
        super(errorCode, message, data);
    }
    
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
    
    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public SystemException(ErrorCode errorCode, String message, Throwable cause, Object data) {
        super(errorCode, message, cause, data);
    }
    
    // 便捷静态方法
    
    /**
     * 系统错误
     * @return SystemException
     */
    public static SystemException systemError() {
        return new SystemException(ErrorCode.SYSTEM_ERROR);
    }
    
    /**
     * 系统错误
     * @param message 自定义消息
     * @return SystemException
     */
    public static SystemException systemError(String message) {
        return new SystemException(ErrorCode.SYSTEM_ERROR, message);
    }
    
    /**
     * 系统错误
     * @param cause 异常原因
     * @return SystemException
     */
    public static SystemException systemError(Throwable cause) {
        return new SystemException(ErrorCode.SYSTEM_ERROR, cause);
    }
    
    /**
     * 系统错误
     * @param message 自定义消息
     * @param cause 异常原因
     * @return SystemException
     */
    public static SystemException systemError(String message, Throwable cause) {
        return new SystemException(ErrorCode.SYSTEM_ERROR, message, cause);
    }
    
    /**
     * 数据库错误
     * @return SystemException
     */
    public static SystemException databaseError() {
        return new SystemException(ErrorCode.DATABASE_ERROR);
    }
    
    /**
     * 数据库错误
     * @param message 自定义消息
     * @return SystemException
     */
    public static SystemException databaseError(String message) {
        return new SystemException(ErrorCode.DATABASE_ERROR, message);
    }
    
    /**
     * 数据库错误
     * @param cause 异常原因
     * @return SystemException
     */
    public static SystemException databaseError(Throwable cause) {
        return new SystemException(ErrorCode.DATABASE_ERROR, cause);
    }
    
    /**
     * 网络错误
     * @return SystemException
     */
    public static SystemException networkError() {
        return new SystemException(ErrorCode.NETWORK_ERROR);
    }
    
    /**
     * 文件错误
     * @return SystemException
     */
    public static SystemException fileError() {
        return new SystemException(ErrorCode.FILE_ERROR);
    }
    
    /**
     * 配置错误
     * @return SystemException
     */
    public static SystemException configError() {
        return new SystemException(ErrorCode.CONFIG_ERROR);
    }
    
    /**
     * 令牌生成错误
     * @return SystemException
     */
    public static SystemException tokenGenerateError() {
        return new SystemException(ErrorCode.TOKEN_GENERATE_ERROR);
    }
    
    /**
     * 令牌解析错误
     * @return SystemException
     */
    public static SystemException tokenParseError() {
        return new SystemException(ErrorCode.TOKEN_PARSE_ERROR);
    }
}
package com.example.system.exception;

import com.example.system.common.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 认证异常
 * 用于处理认证和授权相关异常
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthException extends BaseException {
    
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public AuthException(ErrorCode errorCode, Object data) {
        super(errorCode, data);
    }
    
    public AuthException(ErrorCode errorCode, String message, Object data) {
        super(errorCode, message, data);
    }
    
    public AuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
    
    public AuthException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public AuthException(ErrorCode errorCode, String message, Throwable cause, Object data) {
        super(errorCode, message, cause, data);
    }
    
    // 便捷静态方法
    
    /**
     * 未授权访问
     * @return AuthException
     */
    public static AuthException unauthorized() {
        return new AuthException(ErrorCode.UNAUTHORIZED);
    }
    
    /**
     * 未授权访问
     * @param message 自定义消息
     * @return AuthException
     */
    public static AuthException unauthorized(String message) {
        return new AuthException(ErrorCode.UNAUTHORIZED, message);
    }
    
    /**
     * 禁止访问
     * @return AuthException
     */
    public static AuthException forbidden() {
        return new AuthException(ErrorCode.FORBIDDEN);
    }
    
    /**
     * 禁止访问
     * @param message 自定义消息
     * @return AuthException
     */
    public static AuthException forbidden(String message) {
        return new AuthException(ErrorCode.FORBIDDEN, message);
    }
    
    /**
     * 用户已禁用
     * @return AuthException
     */
    public static AuthException userDisabled() {
        return new AuthException(ErrorCode.USER_DISABLED);
    }
    
    /**
     * 登录已过期
     * @return AuthException
     */
    public static AuthException loginExpired() {
        return new AuthException(ErrorCode.USER_LOGIN_EXPIRED);
    }

    /**
     * 登录已过期
     * @param message 自定义消息
     * @return AuthException
     */
    public static AuthException loginExpired(String message) {
        return new AuthException(ErrorCode.USER_LOGIN_EXPIRED, message);
    }

    /**
     * 用户令牌无效
     * @return AuthException
     */
    public static AuthException tokenInvalid() {
        return new AuthException(ErrorCode.USER_TOKEN_INVALID);
    }

    /**
     * 用户令牌无效
     * @param message 自定义消息
     * @return AuthException
     */
    public static AuthException tokenInvalid(String message) {
        return new AuthException(ErrorCode.USER_TOKEN_INVALID, message);
    }
    
    /**
     * 用户无权限
     * @return AuthException
     */
    public static AuthException noPermission() {
        return new AuthException(ErrorCode.USER_NO_PERMISSION);
    }
    
    /**
     * 用户无权限
     * @param message 自定义消息
     * @return AuthException
     */
    public static AuthException noPermission(String message) {
        return new AuthException(ErrorCode.USER_NO_PERMISSION, message);
    }
    
    /**
     * 用户不存在
     * @return AuthException
     */
    public static AuthException userNotFound() {
        return new AuthException(ErrorCode.USER_NOT_FOUND);
    }
    
    /**
     * 密码错误
     * @return AuthException
     */
    public static AuthException passwordError() {
        return new AuthException(ErrorCode.USER_PASSWORD_ERROR);
    }

    /**
     * 密码错误
     * @param message 自定义消息
     * @return AuthException
     */
    public static AuthException passwordError(String message) {
        return new AuthException(ErrorCode.USER_PASSWORD_ERROR, message);
    }
}
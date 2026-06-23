package com.example.system.exception;

import com.example.system.common.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常
 * 用于处理业务逻辑中的异常情况
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends BaseException {
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public BusinessException(ErrorCode errorCode, Object data) {
        super(errorCode, data);
    }
    
    public BusinessException(ErrorCode errorCode, String message, Object data) {
        super(errorCode, message, data);
    }
    
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
    
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public BusinessException(ErrorCode errorCode, String message, Throwable cause, Object data) {
        super(errorCode, message, cause, data);
    }
    
    // 便捷静态方法
    
    /**
     * 用户不存在
     * @return BusinessException
     */
    public static BusinessException userNotFound() {
        return new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    
    /**
     * 用户不存在
     * @param message 自定义消息
     * @return BusinessException
     */
    public static BusinessException userNotFound(String message) {
        return new BusinessException(ErrorCode.USER_NOT_FOUND, message);
    }
    
    /**
     * 密码错误
     * @return BusinessException
     */
    public static BusinessException passwordError() {
        return new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
    }
    
    /**
     * 密码错误
     * @param message 自定义消息
     * @return BusinessException
     */
    public static BusinessException passwordError(String message) {
        return new BusinessException(ErrorCode.USER_PASSWORD_ERROR, message);
    }
    
    /**
     * 用户名已存在
     * @return BusinessException
     */
    public static BusinessException userAlreadyExists() {
        return new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
    }
    
    /**
     * 用户名已存在
     * @param message 自定义消息
     * @return BusinessException
     */
    public static BusinessException userAlreadyExists(String message) {
        return new BusinessException(ErrorCode.USER_ALREADY_EXISTS, message);
    }
    
    /**
     * 角色不存在
     * @return BusinessException
     */
    public static BusinessException roleNotFound() {
        return new BusinessException(ErrorCode.ROLE_NOT_FOUND);
    }
    
    /**
     * 权限不存在
     * @return BusinessException
     */
    public static BusinessException permissionNotFound() {
        return new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
    }
    
    /**
     * 数据不存在
     * @return BusinessException
     */
    public static BusinessException dataNotFound() {
        return new BusinessException(ErrorCode.DATA_NOT_FOUND);
    }
    
    /**
     * 数据不存在
     * @param message 自定义消息
     * @return BusinessException
     */
    public static BusinessException dataNotFound(String message) {
        return new BusinessException(ErrorCode.DATA_NOT_FOUND, message);
    }
    
    /**
     * 数据重复
     * @return BusinessException
     */
    public static BusinessException dataDuplicate() {
        return new BusinessException(ErrorCode.DATA_DUPLICATE);
    }
    
    /**
     * 参数无效
     * @return BusinessException
     */
    public static BusinessException paramInvalid() {
        return new BusinessException(ErrorCode.PARAM_INVALID);
    }
    
    /**
     * 参数无效
     * @param message 自定义消息
     * @return BusinessException
     */
    public static BusinessException paramInvalid(String message) {
        return new BusinessException(ErrorCode.PARAM_INVALID, message);
    }
    
    /**
     * 参数缺失
     * @return BusinessException
     */
    public static BusinessException paramMissing() {
        return new BusinessException(ErrorCode.PARAM_MISSING);
    }
    
    /**
     * 用户无权限
     * @return BusinessException
     */
    public static BusinessException noPermission() {
        return new BusinessException(ErrorCode.USER_NO_PERMISSION);
    }
    
    /**
     * 数据保存失败
     * @return BusinessException
     */
    public static BusinessException dataSaveFailed() {
        return new BusinessException(ErrorCode.DATA_SAVE_FAILED);
    }
    
    /**
     * 数据更新失败
     * @return BusinessException
     */
    public static BusinessException dataUpdateFailed() {
        return new BusinessException(ErrorCode.DATA_UPDATE_FAILED);
    }
    
    /**
     * 数据删除失败
     * @return BusinessException
     */
    public static BusinessException dataDeleteFailed() {
        return new BusinessException(ErrorCode.DATA_DELETE_FAILED);
    }
}
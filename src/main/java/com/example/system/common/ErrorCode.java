package com.example.system.common;

/**
 * 统一错误码枚举
 * 
 * @author System
 */
public enum ErrorCode {
    // 通用成功
    SUCCESS(200, "操作成功"),
    
    // 通用客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    // 通用服务端错误 5xx
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    
    // 业务错误码 6xxx
    // 用户相关 6001-6099
    USER_NOT_FOUND(6001, "用户不存在"),
    USER_PASSWORD_ERROR(6002, "密码错误"),
    USER_ALREADY_EXISTS(6003, "用户名已存在"),
    USER_DISABLED(6004, "用户已被禁用"),
    USER_LOGIN_EXPIRED(6005, "登录已过期"),
    USER_TOKEN_INVALID(6006, "用户令牌无效"),
    USER_NO_PERMISSION(6007, "用户无权限"),
    USER_TOKEN_EXPIRED(6008, "令牌已过期"),
    USER_TOKEN_REVOKED(6009, "令牌已被吊销"),
    REFRESH_TOKEN_INVALID(6010, "刷新令牌无效"),
    
    // 角色相关 6100-6199
    ROLE_NOT_FOUND(6100, "角色不存在"),
    ROLE_ALREADY_EXISTS(6101, "角色已存在"),
    ROLE_IN_USE(6102, "角色正在使用中，无法删除"),
    
    // 权限相关 6200-6299
    PERMISSION_NOT_FOUND(6200, "权限不存在"),
    PERMISSION_ALREADY_EXISTS(6201, "权限已存在"),
    
    // 数据操作相关 6300-6399
    DATA_SAVE_FAILED(6300, "数据保存失败"),
    DATA_UPDATE_FAILED(6301, "数据更新失败"),
    DATA_DELETE_FAILED(6302, "数据删除失败"),
    DATA_NOT_FOUND(6303, "数据不存在"),
    DATA_DUPLICATE(6304, "数据重复"),
    
    // 参数验证相关 6400-6499
    PARAM_INVALID(6400, "参数无效"),
    PARAM_MISSING(6401, "缺少必要参数"),
    PARAM_FORMAT_ERROR(6402, "参数格式错误"),
    
    // 系统错误码 7xxx
    SYSTEM_ERROR(7000, "系统错误"),
    DATABASE_ERROR(7001, "数据库操作错误"),
    NETWORK_ERROR(7002, "网络连接错误"),
    FILE_ERROR(7003, "文件操作错误"),
    CONFIG_ERROR(7004, "配置错误"),
    TOKEN_GENERATE_ERROR(7005, "令牌生成失败"),
    TOKEN_PARSE_ERROR(7006, "令牌解析失败"),
    
    // 第三方服务错误 8xxx
    THIRD_PARTY_SERVICE_ERROR(8000, "第三方服务错误"),
    EMAIL_SEND_ERROR(8001, "邮件发送失败"),
    SMS_SEND_ERROR(8002, "短信发送失败");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
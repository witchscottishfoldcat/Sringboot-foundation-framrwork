package com.example.system.handler;

import com.example.system.common.ErrorCode;
import com.example.system.common.Result;
import com.example.system.exception.AuthException;
import com.example.system.exception.BaseException;
import com.example.system.exception.BusinessException;
import com.example.system.exception.SystemException;
import com.example.system.service.ExceptionLogService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 全局异常处理器
 * 
 * @author System
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private ExceptionLogService exceptionLogService;

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        // 记录异常日志
        exceptionLogService.recordException(e, request, traceId);
        
        log.warn("业务异常: URI={}, 错误码={}, 错误消息={}", 
                request.getRequestURI(), e.getErrorCodeValue(), e.getMessage());
        
        Result<Object> result = Result.error(e, traceId);
        
        // 根据错误码决定HTTP状态码
        HttpStatus httpStatus = determineHttpStatusByErrorCode(e.getErrorCodeValue());
        
        return ResponseEntity.status(httpStatus).body(result);
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Result<Object>> handleAuthException(AuthException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        // 记录异常日志
        exceptionLogService.recordException(e, request, traceId);
        
        log.warn("认证异常: URI={}, 错误码={}, 错误消息={}", 
                request.getRequestURI(), e.getErrorCodeValue(), e.getMessage());
        
        Result<Object> result = Result.error(e, traceId);
        
        // 认证异常通常返回401或403
        HttpStatus httpStatus = determineHttpStatusByErrorCode(e.getErrorCodeValue());
        
        return ResponseEntity.status(httpStatus).body(result);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<Result<Object>> handleSystemException(SystemException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        // 记录异常日志
        exceptionLogService.recordException(e, request, traceId);
        
        log.error("系统异常: URI={}, 错误码={}, 错误消息={}", 
                request.getRequestURI(), e.getErrorCodeValue(), e.getMessage(), e);
        
        Result<Object> result = Result.error(e, traceId);
        
        // 系统异常通常返回500
        HttpStatus httpStatus = determineHttpStatusByErrorCode(e.getErrorCodeValue());
        
        return ResponseEntity.status(httpStatus).body(result);
    }

    /**
     * 处理参数验证异常 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder errorMessage = new StringBuilder("参数验证失败: ");
        
        for (int i = 0; i < fieldErrors.size(); i++) {
            FieldError error = fieldErrors.get(i);
            errorMessage.append(error.getField()).append(" ").append(error.getDefaultMessage());
            if (i < fieldErrors.size() - 1) {
                errorMessage.append("; ");
            }
        }
        
        log.warn("参数验证异常: URI={}, 错误消息={}", request.getRequestURI(), errorMessage.toString());
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.PARAM_INVALID, errorMessage.toString());
        result.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理参数绑定异常 (@Validated)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Object>> handleBindException(BindException e, HttpServletRequest request) {
        
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder errorMessage = new StringBuilder("参数绑定失败: ");
        
        for (int i = 0; i < fieldErrors.size(); i++) {
            FieldError error = fieldErrors.get(i);
            errorMessage.append(error.getField()).append(" ").append(error.getDefaultMessage());
            if (i < fieldErrors.size() - 1) {
                errorMessage.append("; ");
            }
        }
        
        log.warn("参数绑定异常: URI={}, 错误消息={}", request.getRequestURI(), errorMessage.toString());
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.PARAM_INVALID, errorMessage.toString());
        result.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理约束违反异常 (@Validated的集合参数)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        StringBuilder errorMessage = new StringBuilder("参数约束违反: ");
        
        int i = 0;
        for (ConstraintViolation<?> violation : violations) {
            errorMessage.append(violation.getPropertyPath()).append(" ").append(violation.getMessage());
            if (i < violations.size() - 1) {
                errorMessage.append("; ");
            }
            i++;
        }
        
        log.warn("参数约束违反: URI={}, 错误消息={}", request.getRequestURI(), errorMessage.toString());
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.PARAM_INVALID, errorMessage.toString());
        result.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        
        String errorMessage = String.format("缺少必要参数: %s", e.getParameterName());
        
        log.warn("缺少请求参数: URI={}, 错误消息={}", request.getRequestURI(), errorMessage);
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.PARAM_MISSING, errorMessage);
        result.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        
        String errorMessage = String.format("参数类型不匹配: %s, 期望类型: %s", 
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        
        log.warn("参数类型不匹配: URI={}, 错误消息={}", request.getRequestURI(), errorMessage);
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.PARAM_FORMAT_ERROR, errorMessage);
        result.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理HTTP消息不可读异常（JSON格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        
        log.warn("HTTP消息不可读: URI={}, 错误消息={}", request.getRequestURI(), e.getMessage());
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.PARAM_FORMAT_ERROR, "请求参数格式错误，请检查JSON格式");
        result.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理HTTP请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        
        String errorMessage = String.format("不支持的请求方法: %s", e.getMethod());
        
        log.warn("不支持的请求方法: URI={}, 错误消息={}", request.getRequestURI(), errorMessage);
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.METHOD_NOT_ALLOWED, errorMessage);
        result.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(result);
    }

    /**
     * 处理路径找不到异常（404）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        
        String errorMessage = String.format("请求路径不存在: %s", e.getRequestURL());
        
        log.warn("请求路径不存在: URI={}, 错误消息={}", request.getRequestURI(), errorMessage);
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.NOT_FOUND, errorMessage);
        result.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * 处理访问拒绝异常（403）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Object>> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        
        log.warn("访问被拒绝: URI={}, 错误消息={}", request.getRequestURI(), e.getMessage());
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.FORBIDDEN, "访问被拒绝，权限不足");
        result.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }

    /**
     * 处理 Spring Security 认证异常（401）
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Object>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {

        log.warn("认证失败: URI={}, 错误消息={}", request.getRequestURI(), e.getMessage());

        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.UNAUTHORIZED, "未认证或令牌无效，请重新登录");
        result.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    /**
     * 处理 JWT 过期异常（401）
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Result<Object>> handleExpiredJwtException(
            ExpiredJwtException e, HttpServletRequest request) {

        log.warn("令牌已过期: URI={}", request.getRequestURI());

        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.USER_TOKEN_EXPIRED, "令牌已过期，请重新登录");
        result.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    /**
     * 处理 JWT 解析异常（401）
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Result<Object>> handleJwtException(
            JwtException e, HttpServletRequest request) {

        log.warn("令牌无效: URI={}, 错误消息={}", request.getRequestURI(), e.getMessage());

        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.USER_TOKEN_INVALID, "令牌无效，请重新登录");
        result.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    @ExceptionHandler({SQLException.class, DataAccessException.class})
    public ResponseEntity<Result<Object>> handleDataAccessException(Exception e, HttpServletRequest request) {
        
        log.error("数据库访问异常: URI={}, 错误消息={}", request.getRequestURI(), e.getMessage(), e);
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.DATABASE_ERROR, "数据库操作失败");
        result.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理文件上传大小超出限制异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        
        log.warn("文件上传大小超出限制: URI={}, 错误消息={}", request.getRequestURI(), e.getMessage());
        
        String traceId = generateTraceId();
        Result<Object> result = Result.error(ErrorCode.FILE_ERROR, "上传文件大小超出限制");
        result.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception e, HttpServletRequest request) {
        
        // 首先检查是否是自定义异常
        if (e instanceof BaseException) {
            if (e instanceof BusinessException) {
                return handleBusinessException((BusinessException) e, request);
            } else if (e instanceof AuthException) {
                return handleAuthException((AuthException) e, request);
            } else if (e instanceof SystemException) {
                return handleSystemException((SystemException) e, request);
            }
        }
        
        // 处理未知异常
        String traceId = generateTraceId();
        
        // 记录异常日志
        exceptionLogService.recordException(e, request, traceId);
        
        log.error("未知异常: URI={}, 异常类型={}, 错误消息={}", 
                request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
        
        Result<Object> result = Result.error(ErrorCode.INTERNAL_SERVER_ERROR, "系统内部错误");
        result.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 根据错误码确定HTTP状态码
     */
    private HttpStatus determineHttpStatusByErrorCode(int errorCode) {
        if (errorCode >= 200 && errorCode < 300) {
            return HttpStatus.OK;
        } else if (errorCode >= 400 && errorCode < 500) {
            if (errorCode == ErrorCode.UNAUTHORIZED.getCode()) {
                return HttpStatus.UNAUTHORIZED;
            } else if (errorCode == ErrorCode.FORBIDDEN.getCode()) {
                return HttpStatus.FORBIDDEN;
            } else if (errorCode == ErrorCode.NOT_FOUND.getCode()) {
                return HttpStatus.NOT_FOUND;
            } else {
                return HttpStatus.BAD_REQUEST;
            }
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
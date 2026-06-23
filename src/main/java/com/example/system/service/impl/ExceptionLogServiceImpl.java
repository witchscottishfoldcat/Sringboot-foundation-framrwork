package com.example.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.entity.ExceptionLog;
import com.example.system.exception.BaseException;
import com.example.system.mapper.ExceptionLogMapper;
import com.example.system.service.ExceptionLogService;
import com.example.system.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异常日志服务实现
 *
 * @author System
 */
@Slf4j
@Service
public class ExceptionLogServiceImpl extends ServiceImpl<ExceptionLogMapper, ExceptionLog>
        implements ExceptionLogService {

    @Autowired
    private JwtUtil jwtUtil;

    /** 异步写库线程池：有界队列 + 拒绝策略（丢弃并告警），防止异常风暴压垮 DB。 */
    private ThreadPoolExecutor logWriteExecutor;

    @PostConstruct
    void initExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        logWriteExecutor = new ThreadPoolExecutor(
                Math.max(1, cores / 4),
                Math.max(2, cores / 2),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                r -> {
                    Thread t = new Thread(r, "exception-log-writer");
                    t.setDaemon(true);
                    return t;
                },
                (r, executor) -> log.warn("异常日志写入队列已满，丢弃本次记录（队列容量 500）。")
        );
    }

    @PreDestroy
    void shutdownExecutor() {
        if (logWriteExecutor == null) {
            return;
        }
        logWriteExecutor.shutdown();
        try {
            // 最多等待 10 秒排空剩余日志
            if (!logWriteExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("异常日志线程池在 10s 内未排空，强制关闭。");
                logWriteExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logWriteExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void recordException(Exception exception, HttpServletRequest request, String traceId) {
        try {
            ExceptionLog exceptionLog = buildExceptionLog(exception, request, traceId);
            
            // 异步保存，避免影响性能
            this.saveAsync(exceptionLog);
            
            // 同时记录到日志文件
            logToFile(exceptionLog);
            
        } catch (Exception e) {
            log.error("记录异常日志失败: ", e);
        }
    }

    @Override
    public void recordException(Exception exception, String traceId) {
        try {
            ExceptionLog exceptionLog = buildExceptionLog(exception, null, traceId);
            
            // 异步保存，避免影响性能
            this.saveAsync(exceptionLog);
            
            // 同时记录到日志文件
            logToFile(exceptionLog);
            
        } catch (Exception e) {
            log.error("记录异常日志失败: ", e);
        }
    }

    /**
     * 构建异常日志对象
     */
    private ExceptionLog buildExceptionLog(Exception exception, HttpServletRequest request, String traceId) {
        ExceptionLog exceptionLog = new ExceptionLog();
        
        // 基本信息
        exceptionLog.setTraceId(traceId);
        exceptionLog.setExceptionType(exception.getClass().getSimpleName());
        
        // 如果是自定义异常，提取错误码
        if (exception instanceof BaseException) {
            BaseException baseException = (BaseException) exception;
            exceptionLog.setErrorCode(baseException.getErrorCodeValue());
        }
        
        exceptionLog.setErrorMessage(exception.getMessage());
        
        // 如果有请求信息，提取请求相关数据
        if (request != null) {
            extractRequestInfo(request, exceptionLog);
        }
        
        // 提取异常堆栈
        exceptionLog.setStackTrace(getStackTrace(exception));
        
        // 默认未处理
        exceptionLog.setHandled(false);
        
        return exceptionLog;
    }

    /**
     * 提取请求信息
     */
    private void extractRequestInfo(HttpServletRequest request, ExceptionLog exceptionLog) {
        exceptionLog.setRequestUri(request.getRequestURI());
        exceptionLog.setRequestMethod(request.getMethod());

        // 获取请求参数（敏感字段脱敏）
        StringBuilder params = new StringBuilder();
        request.getParameterMap().forEach((key, values) -> {
            if (params.length() > 0) {
                params.append("&");
            }
            params.append(key).append("=");
            if (values != null && values.length > 0) {
                params.append(isSensitive(key) ? "***" : String.join(",", values));
            }
        });
        exceptionLog.setRequestParams(params.toString());
        
        // 获取IP地址
        exceptionLog.setIpAddress(getIpAddress(request));
        
        // 获取User-Agent
        exceptionLog.setUserAgent(request.getHeader("User-Agent"));
        
        // 尝试从 SecurityContext 获取当前用户（过滤器已解析过 token）
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.example.system.security.UserPrincipal principal) {
                exceptionLog.setUserId(principal.getUserId());
                exceptionLog.setUsername(principal.getUsername());
                return;
            }
        } catch (Exception e) {
            log.debug("读取 SecurityContext 失败: " + e.getMessage());
        }

        // 回退：直接解析 Authorization 头中的 token
        try {
            String token = request.getHeader("Authorization");
            if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    String username = jwtUtil.getUsername(token);
                    if (username != null && !username.isEmpty()) {
                        exceptionLog.setUsername(username);
                        Long userId = jwtUtil.getUserId(token);
                        exceptionLog.setUserId(userId);
                    }
                } catch (Exception e) {
                    // Token解析失败，忽略
                    log.debug("JWT令牌解析失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // 忽略解析JWT异常
            log.debug("解析JWT令牌失败: " + e.getMessage());
        }
    }

    /**
     * 判断参数名是否为敏感字段（用于日志脱敏）。
     */
    private boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("pwd")
                || lower.contains("secret") || lower.contains("token")
                || lower.contains("credential") || lower.contains("authorization");
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果是多个IP，取第一个
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Throwable throwable) {
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return "Failed to get stack trace: " + e.getMessage();
        }
    }

    /**
     * 异步保存异常日志（有界线程池 + 优雅关闭）。
     * 队列满时按拒绝策略丢弃并告警，避免异常风暴压垮数据库。
     */
    private void saveAsync(ExceptionLog exceptionLog) {
        logWriteExecutor.execute(() -> {
            try {
                this.save(exceptionLog);
            } catch (Exception e) {
                log.error("异步保存异常日志失败: ", e);
            }
        });
    }

    /**
     * 记录到日志文件
     */
    private void logToFile(ExceptionLog exceptionLog) {
        // 格式化日志信息
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n=====================================\n");
        logMessage.append("异常追踪ID: ").append(exceptionLog.getTraceId()).append("\n");
        logMessage.append("异常类型: ").append(exceptionLog.getExceptionType()).append("\n");
        
        if (exceptionLog.getErrorCode() != null) {
            logMessage.append("错误码: ").append(exceptionLog.getErrorCode()).append("\n");
        }
        
        logMessage.append("错误消息: ").append(exceptionLog.getErrorMessage()).append("\n");
        
        if (StringUtils.hasText(exceptionLog.getRequestUri())) {
            logMessage.append("请求URI: ").append(exceptionLog.getRequestUri()).append("\n");
            logMessage.append("请求方法: ").append(exceptionLog.getRequestMethod()).append("\n");
            
            if (StringUtils.hasText(exceptionLog.getRequestParams())) {
                logMessage.append("请求参数: ").append(exceptionLog.getRequestParams()).append("\n");
            }
            
            if (StringUtils.hasText(exceptionLog.getIpAddress())) {
                logMessage.append("IP地址: ").append(exceptionLog.getIpAddress()).append("\n");
            }
        }
        
        if (StringUtils.hasText(exceptionLog.getUsername())) {
            logMessage.append("用户: ").append(exceptionLog.getUsername()).append("\n");
        }
        
        if (StringUtils.hasText(exceptionLog.getStackTrace())) {
            logMessage.append("异常堆栈:\n").append(exceptionLog.getStackTrace()).append("\n");
        }
        
        logMessage.append("=====================================\n");
        
        // 根据异常级别选择不同的日志级别
        if (exceptionLog.getErrorCode() != null && 
            (exceptionLog.getErrorCode() >= 7000 || exceptionLog.getErrorCode() < 200)) {
            // 系统异常使用error级别
            log.error(logMessage.toString());
        } else {
            // 业务异常使用warn级别
            log.warn(logMessage.toString());
        }
    }
}
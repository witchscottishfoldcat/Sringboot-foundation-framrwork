package com.example.system.security;

import com.example.system.exception.AuthException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录防爆破限流器（基于 Caffeine，无外部依赖）。
 * <p>
 * 双维度限流：
 * <ul>
 *   <li><b>账户维度</b>：连续失败 N 次后锁定该账户 M 分钟；成功/锁定到期自动复位。</li>
 *   <li><b>IP 维度</b>：单位时间窗口内同一 IP 最多发起 K 次登录，超出即拒绝（防止扫描式爆破）。</li>
 * </ul>
 * 仅适用于单实例部署；多实例应替换为 Redis 实现（接口已抽象，可直接换）。
 *
 * @author system
 */
@Slf4j
@Component
public class LoginRateLimiter {

    /** 单个账户的失败计数与锁定到期时间（epoch millis）。0=未锁定。 */
    private Cache<String, AttemptState> accountAttempts;

    /** IP 维度的短窗口计数。 */
    private Cache<String, AtomicInteger> ipAttempts;

    @Value("${app.security.login.max-failures:5}")
    private int maxFailures;

    /** 锁定时长（分钟）。 */
    @Value("${app.security.login.lock-minutes:15}")
    private int lockMinutes;

    /** 单 IP 在窗口内的最大尝试次数。 */
    @Value("${app.security.login.ip-window-max:20}")
    private int ipWindowMax;

    /** IP 窗口大小（分钟）。 */
    @Value("${app.security.login.ip-window-minutes:1}")
    private int ipWindowMinutes;

    @PostConstruct
    void init() {
        accountAttempts = Caffeine.newBuilder()
                .expireAfterWrite(Math.max(lockMinutes, 60), TimeUnit.MINUTES)
                .maximumSize(100_000)
                .build();
        ipAttempts = Caffeine.newBuilder()
                .expireAfterWrite(ipWindowMinutes, TimeUnit.MINUTES)
                .maximumSize(100_000)
                .build();
    }

    /**
     * 登录前校验：账户未锁定且 IP 未超窗口。违例直接抛 AuthException。
     */
    public void checkBeforeLogin(String username, HttpServletRequest request) {
        // IP 窗口限流
        String ip = clientIp(request);
        AtomicInteger ipCount = ipAttempts.get(ip, k -> new AtomicInteger(0));
        if (ipCount.incrementAndGet() > ipWindowMax) {
            log.warn("登录限流：IP={} 在 {} 分钟内尝试 {} 次，超过上限 {}", ip, ipWindowMinutes, ipCount.get(), ipWindowMax);
            throw AuthException.unauthorized("登录过于频繁，请稍后再试");
        }
        // 账户锁定校验
        if (username != null && !username.isBlank()) {
            AttemptState st = accountAttempts.getIfPresent(username);
            if (st != null && st.isStillLocked()) {
                long remain = (st.lockedUntil - System.currentTimeMillis()) / 1000;
                throw AuthException.unauthorized("账户已被临时锁定，请在 " + Math.max(remain, 1) + " 秒后重试");
            }
        }
    }

    /**
     * 登录失败时调用：累计失败次数，达到阈值则锁定。
     */
    public void recordFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        AttemptState st = accountAttempts.get(username, k -> new AttemptState());
        synchronized (st) {
            st.failures++;
            if (st.failures >= maxFailures) {
                st.lockedUntil = System.currentTimeMillis() + lockMinutes * 60_000L;
                log.warn("账户 [{}] 连续登录失败 {} 次，已锁定 {} 分钟。", username, st.failures, lockMinutes);
            }
        }
    }

    /**
     * 登录成功时调用：清空该账户的失败计数。
     */
    public void recordSuccess(String username) {
        if (username != null) {
            accountAttempts.invalidate(username);
        }
    }

    /**
     * 管理员手动解锁某账户。
     */
    public void unlock(String username) {
        if (username != null) {
            accountAttempts.invalidate(username);
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "unknown" : ip;
    }

    /** 账户失败计数 + 锁定到期时间。 */
    private static class AttemptState {
        int failures = 0;
        long lockedUntil = 0L;

        boolean isStillLocked() {
            return lockedUntil > System.currentTimeMillis();
        }
    }
}

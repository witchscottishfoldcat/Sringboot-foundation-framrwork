package com.example.system.security;

import com.example.system.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link LoginRateLimiter} 单元测试：账户锁定 + 解锁。
 * （IP 限流部分依赖窗口计数，这里只验证账户维度。）
 *
 * @author system
 */
class LoginRateLimiterTest {

    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginRateLimiter();
        ReflectionTestUtils.setField(limiter, "maxFailures", 3);
        ReflectionTestUtils.setField(limiter, "lockMinutes", 15);
        ReflectionTestUtils.setField(limiter, "ipWindowMax", 10_000); // 放开 IP 限制避免干扰账户测试
        ReflectionTestUtils.setField(limiter, "ipWindowMinutes", 1);
        limiter.init();
    }

    private HttpServletRequest request(String ip) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(ip);
        when(req.getRemoteAddr()).thenReturn(ip);
        return req;
    }

    @Test
    void underThreshold_allowsLogin() {
        limiter.checkBeforeLogin("alice", request("1.1.1.1"));
        // 不抛异常即通过
    }

    @Test
    void lockAfterMaxFailures_blocksSubsequentLogin() {
        limiter.checkBeforeLogin("bob", request("2.2.2.2"));
        limiter.recordFailure("bob");
        limiter.recordFailure("bob");
        limiter.recordFailure("bob"); // 第 3 次 → 触发锁定

        AuthException ex = assertThrows(AuthException.class,
                () -> limiter.checkBeforeLogin("bob", request("2.2.2.2")));
        assertTrue(ex.getMessage().contains("锁定"));
    }

    @Test
    void successClearsFailures() {
        limiter.recordFailure("carol");
        limiter.recordFailure("carol");
        limiter.recordSuccess("carol"); // 清零
        // 再次失败 1 次不应锁定
        limiter.recordFailure("carol");
        assertDoesNotThrow(() -> limiter.checkBeforeLogin("carol", request("3.3.3.3")));
    }

    @Test
    void unlockClearsLock() {
        limiter.recordFailure("dave");
        limiter.recordFailure("dave");
        limiter.recordFailure("dave");
        assertThrows(AuthException.class,
                () -> limiter.checkBeforeLogin("dave", request("4.4.4.4")));

        limiter.unlock("dave");
        assertDoesNotThrow(() -> limiter.checkBeforeLogin("dave", request("4.4.4.4")));
    }

    @Test
    void ipLimit_rejectsAfterWindowExceeded() {
        // 重新构造一个小 IP 窗口的限流器
        LoginRateLimiter ipLimiter = new LoginRateLimiter();
        ReflectionTestUtils.setField(ipLimiter, "maxFailures", 100);
        ReflectionTestUtils.setField(ipLimiter, "lockMinutes", 15);
        ReflectionTestUtils.setField(ipLimiter, "ipWindowMax", 3);
        ReflectionTestUtils.setField(ipLimiter, "ipWindowMinutes", 60);
        ipLimiter.init();

        ipLimiter.checkBeforeLogin("u1", request("5.5.5.5"));
        ipLimiter.checkBeforeLogin("u2", request("5.5.5.5"));
        ipLimiter.checkBeforeLogin("u3", request("5.5.5.5"));
        assertThrows(AuthException.class,
                () -> ipLimiter.checkBeforeLogin("u4", request("5.5.5.5")));
    }
}

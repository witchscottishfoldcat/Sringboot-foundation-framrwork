package com.example.system.util;

import com.example.system.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link JwtUtil} 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * @author system
 */
class JwtUtilTest {

    private static final String STRONG_SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; // 64 字节

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(STRONG_SECRET);
        props.setIssuer("test-issuer");
        props.setAccessTokenExpiration(60_000L);   // 1 分钟
        props.setRefreshTokenExpiration(120_000L); // 2 分钟
        jwtUtil = new JwtUtil(props);
        jwtUtil.init();
    }

    @Test
    void generateAccessToken_shouldContainSubjectUserIdAndType() {
        JwtUtil.TokenInfo info = jwtUtil.generateAccessToken(7L, "alice");

        assertNotNull(info.token());
        assertNotNull(info.jti());
        assertEquals(60_000L, info.expirationMillis());

        Claims claims = jwtUtil.parseClaims(info.token());
        assertEquals("alice", claims.getSubject());
        assertEquals(7L, claims.get("userId", Number.class).longValue());
        assertEquals(JwtUtil.TYPE_ACCESS, claims.get("type", String.class));
        assertEquals("test-issuer", claims.getIssuer());
        assertNotNull(claims.getId());
    }

    @Test
    void generateRefreshToken_shouldHaveRefreshType() {
        JwtUtil.TokenInfo info = jwtUtil.generateRefreshToken(9L, "bob");
        Claims claims = jwtUtil.parseClaims(info.token());
        assertEquals(JwtUtil.TYPE_REFRESH, claims.get("type", String.class));
        assertEquals("bob", claims.getSubject());
    }

    @Test
    void jti_shouldBeUniquePerToken() {
        JwtUtil.TokenInfo a = jwtUtil.generateAccessToken(1L, "u");
        JwtUtil.TokenInfo b = jwtUtil.generateAccessToken(1L, "u");
        assertNotEquals(a.jti(), b.jti(), "每次签发的 jti 应不同");
    }

    @Test
    void getUsername_shouldReturnSubject() {
        JwtUtil.TokenInfo info = jwtUtil.generateAccessToken(1L, "carol");
        assertEquals("carol", jwtUtil.getUsername(info.token()));
    }

    @Test
    void getUserId_shouldReturnUserIdClaim() {
        JwtUtil.TokenInfo info = jwtUtil.generateAccessToken(42L, "dave");
        assertEquals(42L, jwtUtil.getUserId(info.token()));
    }

    @Test
    void getType_shouldDistinguishAccessAndRefresh() {
        JwtUtil.TokenInfo access = jwtUtil.generateAccessToken(1L, "u");
        JwtUtil.TokenInfo refresh = jwtUtil.generateRefreshToken(1L, "u");
        assertEquals(JwtUtil.TYPE_ACCESS, jwtUtil.getType(access.token()));
        assertEquals(JwtUtil.TYPE_REFRESH, jwtUtil.getType(refresh.token()));
    }

    @Test
    void getJti_shouldMatchInfoJti() {
        JwtUtil.TokenInfo info = jwtUtil.generateAccessToken(1L, "u");
        assertEquals(info.jti(), jwtUtil.getJti(info.token()));
    }

    @Test
    void parseClaims_shouldRejectTamperedToken() {
        JwtUtil.TokenInfo info = jwtUtil.generateAccessToken(1L, "u");
        String tampered = info.token().substring(0, info.token().length() - 4) + "AAAA";
        assertThrows(JwtException.class, () -> jwtUtil.parseClaims(tampered));
    }

    @Test
    void parseClaims_shouldRejectForeignSecretToken() {
        // 用另一个密钥签发的 token，不应被接受
        JwtProperties other = new JwtProperties();
        other.setSecret("a".repeat(64) + "different-secret-padding-bytes-xxxxxxxxxxxxxxx");
        other.setIssuer("test-issuer");
        JwtUtil otherUtil = new JwtUtil(other);
        otherUtil.init();
        JwtUtil.TokenInfo foreign = otherUtil.generateAccessToken(1L, "u");

        assertThrows(JwtException.class, () -> jwtUtil.parseClaims(foreign.token()));
    }

    @Test
    void parseClaims_shouldThrowOnExpiredToken() throws InterruptedException {
        JwtProperties props = new JwtProperties();
        props.setSecret(STRONG_SECRET);
        props.setIssuer("test-issuer");
        props.setAccessTokenExpiration(1L); // 1ms
        JwtUtil shortLived = new JwtUtil(props);
        shortLived.init();
        JwtUtil.TokenInfo info = shortLived.generateAccessToken(1L, "u");
        Thread.sleep(20L);
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.parseClaims(info.token()));
    }

    @Test
    void parseClaimsIgnoreExpiry_shouldReturnClaimsEvenIfExpired() throws InterruptedException {
        JwtProperties props = new JwtProperties();
        props.setSecret(STRONG_SECRET);
        props.setIssuer("test-issuer");
        props.setAccessTokenExpiration(1L);
        JwtUtil shortLived = new JwtUtil(props);
        shortLived.init();
        JwtUtil.TokenInfo info = shortLived.generateAccessToken(5L, "expiring");
        Thread.sleep(20L);
        Claims claims = jwtUtil.parseClaimsIgnoreExpiry(info.token());
        assertEquals("expiring", claims.getSubject());
        assertEquals(5L, claims.get("userId", Number.class).longValue());
    }

    @Test
    void secretLength_requirementIsEnforcedAt64Bytes() {
        JwtProperties ok = new JwtProperties();
        ok.setSecret(new String(new byte[64], StandardCharsets.UTF_8).replace('\0', 'x'));
        JwtUtil util = new JwtUtil(ok);
        util.init();
        // 64 字节密钥可正常签发
        JwtUtil.TokenInfo info = util.generateAccessToken(1L, "u");
        assertNotNull(info.token());
    }

    @Test
    void secretByteLength_isMeasuredInUtf8Bytes() {
        // 中文字符 UTF-8 占 3 字节，确保字节数而非字符数参与判断
        JwtProperties props = new JwtProperties();
        // 22 个中文字符 = 66 字节 >= 64
        props.setSecret("密".repeat(22));
        props.setIssuer("test-issuer");
        JwtUtil util = new JwtUtil(props);
        util.init();
        JwtUtil.TokenInfo info = util.generateAccessToken(1L, "u");
        assertNotNull(info.token(), "按 UTF-8 字节数计，22 个中文字符应满足 64 字节");
    }
}

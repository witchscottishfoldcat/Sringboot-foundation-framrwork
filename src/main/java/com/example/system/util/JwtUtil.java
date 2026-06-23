package com.example.system.util;

import com.example.system.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类（jjwt 0.12.x）。
 * <p>
 * 双令牌：
 * <ul>
 *   <li><b>access</b>：短期，type=access，用于业务请求鉴权。</li>
 *   <li><b>refresh</b>：长期，type=refresh，仅用于换取新的令牌对（轮换）。</li>
 * </ul>
 * 不再把权限列表写入 token；权限由 Security 链路实时从数据库加载。
 *
 * @author system
 */
@Slf4j
@Component
public class JwtUtil {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "type";

    private final JwtProperties properties;
    private SecretKey signingKey;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = properties.getSecret() == null
                ? new byte[0]
                : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < JwtProperties.MIN_SECRET_BYTES) {
            // 弱/空密钥：生成运行时随机密钥让 bean 能装配，真正的"拒绝启动"门由
            // SecretValidationInitializer（EnvironmentPostProcessor，早于 bean 创建）负责。
            // 这里仅 WARN，避免 @PostConstruct 抛异常导致上下文无法初始化。
            log.warn("JWT 密钥长度不足 {} 字节（当前 {} 字节），已生成运行时随机密钥临时兜底。"
                            + " 生产环境必须配置 JWT_SECRET，否则启动校验会拒绝。",
                    JwtProperties.MIN_SECRET_BYTES, keyBytes.length);
            this.signingKey = Jwts.SIG.HS512.key().build();
        } else {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * 生成 access token。
     */
    public TokenInfo generateAccessToken(Long userId, String username) {
        return generate(userId, username, TYPE_ACCESS, properties.getAccessTokenExpiration());
    }

    /**
     * 生成 refresh token。
     */
    public TokenInfo generateRefreshToken(Long userId, String username) {
        return generate(userId, username, TYPE_REFRESH, properties.getRefreshTokenExpiration());
    }

    private TokenInfo generate(Long userId, String username, String type, long expirationMillis) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);
        String token = Jwts.builder()
                .id(jti)
                .issuer(properties.getIssuer())
                .subject(username)
                .claim("userId", userId)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
        return new TokenInfo(token, jti, exp.toInstant(), expirationMillis);
    }

    /**
     * 解析并校验 token 的签名与过期时间，返回 Claims。
     *
     * @throws ExpiredJwtException token 已过期
     * @throws JwtException         token 无效
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析 token，忽略过期异常（用于从过期 token 中提取 jti 做吊销）。
     */
    public Claims parseClaimsIgnoreExpiry(String token) {
        try {
            return parseClaims(token);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object v = parseClaims(token).get("userId");
        if (v instanceof Number n) {
            return n.longValue();
        }
        return v == null ? null : Long.valueOf(v.toString());
    }

    public String getType(String token) {
        return parseClaims(token).get(CLAIM_TYPE, String.class);
    }

    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    public String getJtiIgnoreExpiry(String token) {
        return parseClaimsIgnoreExpiry(token).getId();
    }

    public long getAccessTokenExpiration() {
        return properties.getAccessTokenExpiration();
    }

    public long getRefreshTokenExpiration() {
        return properties.getRefreshTokenExpiration();
    }

    /**
     * 令牌信息（token 字符串 + jti + 过期瞬时 + 有效期）。
     */
    public record TokenInfo(String token, String jti, java.time.Instant expiresAt, long expirationMillis) {
    }
}

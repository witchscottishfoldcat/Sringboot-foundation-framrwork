package com.example.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性。
 * <p>
 * 支持双令牌：access token（短期，用于业务请求）+ refresh token（长期，用于轮换）。
 *
 * @author system
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 签名密钥（HS512 要求 >= 64 字节）。生产环境必须通过环境变量 JWT_SECRET 注入，
     * 启动期会做强度校验，弱/默认密钥在非 dev profile 下拒绝启动。
     */
    private String secret;

    /**
     * 签发者（iss）声明。
     */
    private String issuer = "system";

    /**
     * Access token 有效期（毫秒），默认 2 小时。
     */
    private long accessTokenExpiration = 7_200_000L;

    /**
     * Refresh token 有效期（毫秒），默认 7 天。
     */
    private long refreshTokenExpiration = 604_800_000L;

    /**
     * 是否强制校验密钥强度。dev profile 下可关闭以便使用开发用密钥。
     */
    private boolean enforceSecret = true;

    /**
     * Access token 期望的最小密钥字节长度（HS512）。
     */
    public static final int MIN_SECRET_BYTES = 64;
}

package com.example.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * JWT 密钥启动期强校验。
 * <p>
 * 策略：
 * <ul>
 *   <li>当激活 profile 含 {@code dev} 时跳过强制校验（允许开发用占位密钥）。</li>
 *   <li>否则（生产/预发）：密钥缺失、长度不足 64 字节、命中弱密钥黑名单、
 *       或形似占位符（含 "dev-only"/"please-override"/"change"/"xxxxxxxx" 等模式）→ 抛异常拒绝启动。</li>
 * </ul>
 *
 * @author system
 */
@Slf4j
@Component
public class SecretValidationInitializer {

    /** 弱密钥黑名单（命中即拒绝）。 */
    private static final List<String> WEAK_SECRET_BLACKLIST = List.of(
            "mySecretKey",
            "secret",
            "changeme",
            "change-me",
            "password",
            "123456",
            "test",
            "key"
    );

    /** 占位符模式（命中即拒绝，防止"看起来够长"的默认值蒙混过关）。 */
    private static final List<String> PLACEHOLDER_PATTERNS = List.of(
            "dev-only",
            "dev_only",
            "please-override",
            "please_override",
            "override-in-production",
            "change-me",
            "changeme",
            "todo",
            "xxxxxxxx",
            "placeholder",
            "example",
            "your-secret",
            "your_secret"
    );

    private final JwtProperties jwtProperties;
    private final Environment environment;

    @Value("${jwt.enforce-secret:true}")
    private boolean enforceSecret;

    public SecretValidationInitializer(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (!enforceSecret) {
            log.warn("jwt.enforce-secret=false，已跳过 JWT 密钥强校验（仅建议开发环境使用）。");
            return;
        }
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev"));
        String secret = jwtProperties.getSecret();

        if (secret == null || secret.isBlank()) {
            if (isDev) {
                log.warn("开发环境未配置 JWT 密钥，将使用运行时占位（仅限 dev）。");
                return;
            }
            throw fail("JWT 密钥未配置（请通过环境变量 JWT_SECRET 注入不少于 64 字节的强密钥）。");
        }
        int len = secret.getBytes(StandardCharsets.UTF_8).length;
        if (len < JwtProperties.MIN_SECRET_BYTES) {
            if (isDev) {
                log.warn("开发环境放行弱密钥：当前 {} 字节，要求 >= {} 字节。", len, JwtProperties.MIN_SECRET_BYTES);
                return;
            }
            throw fail(String.format("JWT 密钥长度不足：当前 %d 字节，HS512 要求 >= %d 字节。",
                    len, JwtProperties.MIN_SECRET_BYTES));
        }
        String lower = secret.toLowerCase(Locale.ROOT);
        if (WEAK_SECRET_BLACKLIST.contains(lower)) {
            throw fail("JWT 密钥命中弱密钥黑名单，禁止启动。");
        }
        for (String pattern : PLACEHOLDER_PATTERNS) {
            if (lower.contains(pattern)) {
                throw fail("JWT 密钥形似占位符/默认值（命中模式 '" + pattern + "'），禁止启动。"
                        + " 请配置一个真正的随机强密钥。");
            }
        }
        // 熵检查：全相同字符的密钥（如 "aaaa...a"）拒绝
        if (secret.chars().distinct().count() < 4) {
            throw fail("JWT 密钥字符多样性过低（少于 4 种字符），禁止启动。");
        }
        log.info("JWT 密钥强校验通过（长度 {} 字节）。", len);
    }

    private IllegalStateException fail(String reason) {
        log.error("JWT 密钥校验失败：{}", reason);
        return new IllegalStateException("JWT 密钥校验失败：" + reason
                + " 请配置一个长度不少于 64 字节的强密钥（建议环境变量 JWT_SECRET）。");
    }
}

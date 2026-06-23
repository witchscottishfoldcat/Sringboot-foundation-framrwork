package com.example.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * JWT 密钥启动期强校验。
 * <p>
 * 关键设计：同时实现 {@link EnvironmentPostProcessor} 与监听 {@code ApplicationReadyEvent}。
 * <ul>
 *   <li>{@link EnvironmentPostProcessor}：在 <b>任何 bean 创建之前</b> 执行（上下文初始化前的最早阶段），
 *       此时如果校验失败，可以<b>立即终止启动</b>，避免 JwtUtil 等 bean 在装配阶段因弱密钥崩溃。
 *       这是真正的"fail-fast 启动门"。</li>
 *   <li>dev profile 跳过强制校验（允许开发用占位密钥），但仍会打印 WARN。</li>
 * </ul>
 *
 * @author system
 */
@Slf4j
public class SecretValidationInitializer implements EnvironmentPostProcessor {

    /** 弱密钥黑名单（命中即拒绝）。 */
    private static final List<String> WEAK_SECRET_BLACKLIST = List.of(
            "mysecretkey",
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

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 读取配置项（此时尚无 bean，直接从 Environment 取）
        boolean enforce = environment.getProperty("jwt.enforce-secret", Boolean.class, true);
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev"));
        String secret = environment.getProperty("jwt.secret");

        if (!enforce) {
            log.warn("jwt.enforce-secret=false，已跳过 JWT 密钥强校验（仅建议开发环境使用）。");
            return;
        }

        if (secret == null || secret.isBlank()) {
            if (isDev) {
                log.warn("开发环境未配置 JWT 密钥（jwt.secret 为空），JwtUtil 将使用运行时随机密钥兜底。");
                return;
            }
            // fail-fast：直接抛异常终止启动
            throw new IllegalStateException("JWT 密钥校验失败：密钥未配置。"
                    + "请通过环境变量 JWT_SECRET 注入不少于 64 字节的强密钥，"
                    + "或使用 dev profile 跳过校验。");
        }

        int len = secret.getBytes(StandardCharsets.UTF_8).length;
        if (len < JwtProperties.MIN_SECRET_BYTES) {
            if (isDev) {
                log.warn("开发环境放行弱密钥：当前 {} 字节，要求 >= {} 字节。", len, JwtProperties.MIN_SECRET_BYTES);
                return;
            }
            throw new IllegalStateException(String.format(
                    "JWT 密钥校验失败：长度不足，当前 %d 字节，HS512 要求 >= %d 字节。",
                    len, JwtProperties.MIN_SECRET_BYTES));
        }

        String lower = secret.toLowerCase(Locale.ROOT);
        if (WEAK_SECRET_BLACKLIST.contains(lower)) {
            throw new IllegalStateException("JWT 密钥校验失败：命中弱密钥黑名单，禁止启动。");
        }
        for (String pattern : PLACEHOLDER_PATTERNS) {
            if (lower.contains(pattern)) {
                throw new IllegalStateException("JWT 密钥校验失败：形似占位符/默认值（命中模式 '"
                        + pattern + "'），禁止启动。请配置一个真正的随机强密钥。");
            }
        }
        if (secret.chars().distinct().count() < 4) {
            throw new IllegalStateException("JWT 密钥校验失败：字符多样性过低（少于 4 种字符），禁止启动。");
        }

        log.info("JWT 密钥强校验通过（长度 {} 字节）。", len);
    }
}

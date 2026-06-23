package com.example.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS 跨域配置。
 * <p>
 * 生产环境应配置明确的 origin 白名单，而非通配 {@code *}。
 *
 * @author system
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * 允许的来源白名单。默认仅本地开发常用地址；生产必须显式配置。
     * {@code *} 仅在 allow-credentials=false 时有意义，不推荐生产使用。
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:8080",
            "http://localhost:5173",
            "http://127.0.0.1:5173"
    ));

    /**
     * 是否允许携带凭据（cookie）。默认 true。
     */
    private boolean allowCredentials = true;

    /**
     * 预检缓存秒数。
     */
    private long maxAge = 3600L;
}

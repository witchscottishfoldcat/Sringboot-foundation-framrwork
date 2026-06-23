package com.example.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * 时区策略配置。
 * <p>
 * 双时区约定：
 * <ul>
 *   <li><b>internal</b>：系统内部统一使用，固定为 UTC（实体、数据库、令牌签发时间一律 UTC）。</li>
 *   <li><b>external</b>：对外（HTTP JSON 序列化、日志展示）使用的时区，默认 Asia/Shanghai，可配置。</li>
 * </ul>
 * 序列化方向：internal(UTC) → external(对外)；反序列化方向：external(对外) → internal(UTC)。
 *
 * @author system
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.timezone")
public class TimeProperties {

    /**
     * 内部时区，固定 UTC，不可配置（保证一致性）。
     */
    private String internal = "UTC";

    /**
     * 对外时区，默认上海时间，可通过 app.timezone.external 覆盖。
     */
    private String external = "Asia/Shanghai";

    /**
     * 是否在 HTTP 响应中以对外时区输出时间（false 则原样输出 UTC）。
     */
    private boolean externalSerializationEnabled = true;

    public ZoneId internalZoneId() {
        return ZoneId.of(internal);
    }

    public ZoneId externalZoneId() {
        return ZoneId.of(external);
    }
}

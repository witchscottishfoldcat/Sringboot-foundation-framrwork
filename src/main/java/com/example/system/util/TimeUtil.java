package com.example.system.util;

import com.example.system.config.TimeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 时区工具类。
 * <p>
 * 核心原则：内部存储与业务计算一律 UTC；仅在边界（HTTP 序列化 / 展示）转换为对外时区。
 *
 * <ul>
 *   <li>{@link #nowUtc()}：所有 create_time/update_time、令牌签发时间统一调用此方法。</li>
 *   <li>{@link #toExternal(LocalDateTime)}：把 UTC 的 LocalDateTime 转成对外时区，用于展示。</li>
 *   <li>{@link #toUtc(LocalDateTime)}：把前端传入的对外时区时间转回 UTC。</li>
 * </ul>
 *
 * @author system
 */
@Component
public class TimeUtil {

    private static final AtomicReference<ZoneId> INTERNAL_ZONE = new AtomicReference<>(ZoneOffset.UTC);
    private static final AtomicReference<ZoneId> EXTERNAL_ZONE = new AtomicReference<>(ZoneId.of("Asia/Shanghai"));

    @Autowired
    public void init(TimeProperties properties) {
        // (kept public for test-friendliness)

        INTERNAL_ZONE.set(properties.internalZoneId());
        EXTERNAL_ZONE.set(properties.externalZoneId());
    }

    /**
     * 内部（UTC）当前时间。
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(INTERNAL_ZONE.get());
    }

    /**
     * 内部（UTC）当前瞬时。
     */
    public static Instant nowInstant() {
        return Instant.now();
    }

    /**
     * 内部时区 ZoneId（固定 UTC）。
     */
    public static ZoneId internalZone() {
        return INTERNAL_ZONE.get();
    }

    /**
     * 对外时区 ZoneId（默认 Asia/Shanghai，可配）。
     */
    public static ZoneId externalZone() {
        return EXTERNAL_ZONE.get();
    }

    /**
     * 把一个"无时区"的本地时间，按对外时区解析后，转成内部 UTC 本地时间。
     * 典型场景：前端传入的对外时间 → 入库前转 UTC。
     */
    public static LocalDateTime toUtc(LocalDateTime externalLocalTime) {
        if (externalLocalTime == null) {
            return null;
        }
        return externalLocalTime.atZone(EXTERNAL_ZONE.get())
                .withZoneSameInstant(INTERNAL_ZONE.get())
                .toLocalDateTime();
    }

    /**
     * 把一个对外时区的 ZonedDateTime 转成内部 UTC 本地时间。
     */
    public static LocalDateTime toUtc(ZonedDateTime externalZoned) {
        if (externalZoned == null) {
            return null;
        }
        return externalZoned.withZoneSameInstant(INTERNAL_ZONE.get()).toLocalDateTime();
    }

    /**
     * 把内部 UTC 本地时间转成对外时区的本地时间，用于展示。
     */
    public static LocalDateTime toExternal(LocalDateTime utcLocalTime) {
        if (utcLocalTime == null) {
            return null;
        }
        return utcLocalTime.atZone(INTERNAL_ZONE.get())
                .withZoneSameInstant(EXTERNAL_ZONE.get())
                .toLocalDateTime();
    }

    /**
     * 把内部 UTC 本地时间转成对外时区的 ZonedDateTime（带时区偏移信息）。
     */
    public static ZonedDateTime toExternalZoned(LocalDateTime utcLocalTime) {
        if (utcLocalTime == null) {
            return null;
        }
        return utcLocalTime.atZone(INTERNAL_ZONE.get())
                .withZoneSameInstant(EXTERNAL_ZONE.get());
    }

    /**
     * 计算过期瞬时（基于当前 UTC）。
     */
    public static Instant expiresAtUtc(long millis) {
        return nowInstant().plusMillis(millis);
    }
}

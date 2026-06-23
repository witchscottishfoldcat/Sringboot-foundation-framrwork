package com.example.system.util;

import com.example.system.config.TimeProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TimeUtil} 单元测试。
 * 验证内部 UTC、对外上海时间的双向转换。
 *
 * @author system
 */
class TimeUtilTest {

    @BeforeAll
    static void init() {
        // 模拟 @Autowired init
        TimeProperties props = new TimeProperties();
        props.setExternal("Asia/Shanghai");
        new TimeUtil().init(props);
    }

    @Test
    void nowUtc_shouldBeInUtcZone() {
        LocalDateTime now = TimeUtil.nowUtc();
        // UTC 与now()的瞬时只差几纳秒，这里验证它可被当作UTC解释
        assertNotNull(now);
        // 上海比 UTC 快 8 小时；nowUtc 转 external 后应晚 8 小时
        ZonedDateTime utcZoned = now.atZone(ZoneOffset.UTC);
        ZonedDateTime shanghaiZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
        assertEquals(shanghaiZoned.toLocalDateTime(), TimeUtil.toExternal(now));
    }

    @Test
    void toExternal_utcNoon_shouldBeShanghaiEightPm() {
        // UTC 12:00 -> 上海 20:00
        LocalDateTime utcNoon = LocalDateTime.of(2026, 6, 23, 12, 0, 0);
        LocalDateTime external = TimeUtil.toExternal(utcNoon);
        assertEquals(LocalDateTime.of(2026, 6, 23, 20, 0, 0), external);
    }

    @Test
    void toUtc_shanghaiEightPm_shouldBeUtcNoon() {
        // 上海 20:00 -> UTC 12:00
        LocalDateTime shanghaiEightPm = LocalDateTime.of(2026, 6, 23, 20, 0, 0);
        LocalDateTime utc = TimeUtil.toUtc(shanghaiEightPm);
        assertEquals(LocalDateTime.of(2026, 6, 23, 12, 0, 0), utc);
    }

    @Test
    void toUtcAndExternal_shouldBeInverse() {
        LocalDateTime original = LocalDateTime.of(2026, 1, 15, 7, 30, 45);
        LocalDateTime roundTrip = TimeUtil.toUtc(TimeUtil.toExternal(original));
        assertEquals(original, roundTrip);
    }

    @Test
    void toUtc_zonedDateTimeFromNewYork_shouldConvertToUtc() {
        // 纽约夏令时 UTC-4，2026-06-23 10:00 纽约 = 14:00 UTC
        ZonedDateTime newYork = ZonedDateTime.of(2026, 6, 23, 10, 0, 0, 0, ZoneId.of("America/New_York"));
        LocalDateTime utc = TimeUtil.toUtc(newYork);
        assertEquals(LocalDateTime.of(2026, 6, 23, 14, 0, 0), utc);
    }

    @Test
    void toUtc_nullInput_shouldReturnNull() {
        assertNull(TimeUtil.toUtc((LocalDateTime) null));
    }

    @Test
    void toExternal_nullInput_shouldReturnNull() {
        assertNull(TimeUtil.toExternal(null));
    }

    @Test
    void zones_shouldMatchConfiguredValues() {
        // ZoneId.of("UTC") 与 ZoneOffset.UTC 不是同一对象，但规则相同（偏移 0）
        assertEquals(ZoneOffset.UTC, TimeUtil.internalZone().getRules().getOffset(Instant.EPOCH));
        assertEquals("UTC", TimeUtil.internalZone().getId());
        assertEquals(ZoneId.of("Asia/Shanghai"), TimeUtil.externalZone());
    }
}

package com.example.system.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Jackson 时间序列化配置。
 * <p>
 * 约定：实体与数据库内部一律 UTC 的 LocalDateTime。
 * <ul>
 *   <li>序列化（响应）：UTC LocalDateTime → 对外时区字符串输出。</li>
 *   <li>反序列化（请求）：对外时区字符串 → UTC LocalDateTime 入库。</li>
 * </ul>
 *
 * @author system
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_OFFSET_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final TimeProperties timeProperties;

    @Autowired
    public JacksonConfig(TimeProperties timeProperties) {
        this.timeProperties = timeProperties;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        ZoneId internal = timeProperties.internalZoneId();
        ZoneId external = timeProperties.externalZoneId();
        boolean externalOut = timeProperties.isExternalSerializationEnabled();

        return builder -> {
            builder.modules(new JavaTimeModule(), customTimeModule(internal, external, externalOut));
            // 关闭默认的 WRITE_DATES_AS_TIMESTAMPS，统一以字符串输出
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }

    /**
     * 自定义时间模块：
     * - LocalDateTime（内部 UTC）序列化为对外时区字符串；反序列化时把对外时区输入转回 UTC。
     */
    private SimpleModule customTimeModule(ZoneId internal, ZoneId external, boolean externalOut) {
        SimpleModule module = new SimpleModule("DualTimeZoneModule");

        // LocalDateTime 序列化：UTC → 对外时区
        module.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen,
                                  SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                if (externalOut) {
                    ZonedDateTime zoned = value.atZone(internal).withZoneSameInstant(external);
                    gen.writeString(zoned.format(ISO_OFFSET_FORMATTER));
                } else {
                    gen.writeString(value.atZone(internal).format(ISO_OFFSET_FORMATTER));
                }
            }
        });

        // LocalDateTime 反序列化：对外时区输入 → UTC
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (p.currentToken() == JsonToken.VALUE_NULL) {
                    return null;
                }
                String text = p.getValueAsString();
                if (text == null || text.isBlank()) {
                    return null;
                }
                // 1) 带偏移量 / ISO（推荐）：直接解析到瞬时再转 UTC
                try {
                    TemporalAccessor parsed = ISO_OFFSET_FORMATTER.parseBest(text,
                            ZonedDateTime::from, OffsetDateTime::from, LocalDateTime::from);
                    if (parsed instanceof ZonedDateTime zdt) {
                        return zdt.withZoneSameInstant(internal).toLocalDateTime();
                    }
                    if (parsed instanceof OffsetDateTime odt) {
                        return odt.atZoneSameInstant(internal).toLocalDateTime();
                    }
                    // 纯 LocalDateTime：按对外时区解释
                    LocalDateTime ldt = (LocalDateTime) parsed;
                    return ldt.atZone(external).withZoneSameInstant(internal).toLocalDateTime();
                } catch (DateTimeException ex) {
                    // 2) 兼容 "yyyy-MM-dd HH:mm:ss" 无偏移格式：按对外时区解释后转 UTC
                    try {
                        LocalDateTime ldt = LocalDateTime.parse(text, LOCAL_DATE_TIME_FORMATTER);
                        return ldt.atZone(external).withZoneSameInstant(internal).toLocalDateTime();
                    } catch (DateTimeException ex2) {
                        throw ctxt.weirdStringException(text, LocalDateTime.class,
                                "无法解析时间字符串，期望 ISO_OFFSET_DATE_TIME 或 yyyy-MM-dd HH:mm:ss");
                    }
                }
            }
        });

        return module;
    }
}

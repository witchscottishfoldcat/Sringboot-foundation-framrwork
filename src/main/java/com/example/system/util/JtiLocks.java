package com.example.system.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 jti（令牌唯一标识）的细粒度串行锁。
 * <p>
 * 用途：消除 refresh token 并发轮换的 TOCTOU 竞态——同一 jti 的两次并发刷新会被串行化，
 * 第二次进入时能看到第一次写入的吊销状态并拒绝。
 * <p>
 * 单实例方案；多实例应替换为 Redis 分布式锁（如 Redisson），接口可平滑替换。
 *
 * @author system
 */
@Component
public class JtiLocks {

    /** key = jti，value = 永远是同一个锁对象（用于 synchronized）。 */
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * 获取（或创建）某 jti 对应的锁对象。调用方对其 synchronized。
     */
    public Object lockFor(String jti) {
        return locks.computeIfAbsent(jti, k -> new Object());
    }

    /**
     * 释放（可选）：锁使用完毕后清理，避免 Map 无限增长。
     * 仅在没有其它线程持锁时调用。
     */
    public void release(String jti) {
        locks.remove(jti);
    }
}

package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.entity.TokenBlacklist;
import com.example.system.mapper.TokenBlacklistMapper;
import com.example.system.service.TokenBlacklistService;
import com.example.system.util.TimeUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 令牌黑名单服务实现。
 * <p>
 * 性能优化：使用 Caffeine 本地缓存 {@code isRevoked} 结果。
 * <ul>
 *   <li>命中已吊销（true）：缓存到该令牌自然过期时刻（之后必然无效，可丢弃）。</li>
 *   <li>未命中（false）：短 TTL（30s）缓存，避免被吊销后还能长期使用；吊销时主动失效。</li>
 * </ul>
 *
 * @author system
 */
@Slf4j
@Service
public class TokenBlacklistServiceImpl extends ServiceImpl<TokenBlacklistMapper, TokenBlacklist>
        implements TokenBlacklistService {

    /** 已吊销令牌的缓存（jti -> Boolean.TRUE）；写库后主动写入。 */
    private Cache<String, Boolean> revokedCache;

    /** 未命中查询的负缓存（jti -> Boolean.FALSE），短 TTL。 */
    private Cache<String, Boolean> notRevokedCache;

    @PostConstruct
    void initCaches() {
        revokedCache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS) // 兜底；通常写库时已按 expireAt 设定
                .maximumSize(100_000)
                .build();
        notRevokedCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS) // 负缓存短 TTL，保证吊销尽快生效
                .maximumSize(100_000)
                .build();
    }

    @Override
    public void revoke(String jti, Long userId, String username, String type,
                       LocalDateTime expireAt, String reason) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        // 幂等：已存在则跳过
        if (isRevoked(jti)) {
            return;
        }
        TokenBlacklist record = new TokenBlacklist()
                .setJti(jti)
                .setUserId(userId)
                .setUsername(username)
                .setTokenType(type)
                .setExpireAt(expireAt)
                .setReason(reason);
        try {
            save(record);
        } catch (Exception e) {
            // 并发幂等场景下可能唯一索引冲突，忽略
            log.debug("吊销 jti={} 时发生异常（可能已存在）：{}", jti, e.getMessage());
        }
        // 主动更新缓存：写入正缓存、清除负缓存，保证吊销立即生效
        revokedCache.put(jti, Boolean.TRUE);
        notRevolvedCacheInvalidate(jti);
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Boolean positive = revokedCache.getIfPresent(jti);
        if (Boolean.TRUE.equals(positive)) {
            return true;
        }
        Boolean negative = notRevokedCache.getIfPresent(jti);
        if (Boolean.FALSE.equals(negative)) {
            return false;
        }
        // 缓存未命中，查库
        QueryWrapper<TokenBlacklist> qw = new QueryWrapper<>();
        qw.eq("jti", jti).last("LIMIT 1");
        boolean exists = baseMapper.selectCount(qw) > 0;
        if (exists) {
            revokedCache.put(jti, Boolean.TRUE);
        } else {
            notRevokedCache.put(jti, Boolean.FALSE);
        }
        return exists;
    }

    private void notRevolvedCacheInvalidate(String jti) {
        notRevokedCache.invalidate(jti);
    }

    @Override
    public int cleanExpired() {
        LocalDateTime now = TimeUtil.nowUtc();
        QueryWrapper<TokenBlacklist> qw = new QueryWrapper<>();
        qw.lt("expire_at", now);
        int deleted = baseMapper.delete(qw);
        if (deleted > 0) {
            log.info("清理过期黑名单记录 {} 条。", deleted);
        }
        return deleted;
    }
}

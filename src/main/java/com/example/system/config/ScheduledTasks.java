package com.example.system.config;

import com.example.system.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：清理已自然过期的令牌黑名单记录，防止表无限膨胀。
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 每天凌晨 3:30（UTC）清理过期黑名单记录。
     */
    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    public void cleanExpiredTokenBlacklist() {
        try {
            int n = tokenBlacklistService.cleanExpired();
            log.info("定时清理 token_blacklist 完成，删除 {} 条。", n);
        } catch (Exception e) {
            log.error("定时清理 token_blacklist 失败：{}", e.getMessage(), e);
        }
    }
}

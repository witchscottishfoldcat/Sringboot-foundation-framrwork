package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.entity.TokenBlacklist;

import java.time.LocalDateTime;

/**
 * 令牌黑名单服务。
 *
 * @author system
 */
public interface TokenBlacklistService extends IService<TokenBlacklist> {

    /**
     * 吊销指定 jti。
     *
     * @param jti      令牌唯一标识
     * @param userId   用户ID
     * @param username 用户名
     * @param type     access / refresh
     * @param expireAt 令牌原始过期时间（UTC）
     * @param reason   吊销原因
     */
    void revoke(String jti, Long userId, String username, String type, LocalDateTime expireAt, String reason);

    /**
     * 判断 jti 是否已被吊销。
     */
    boolean isRevoked(String jti);

    /**
     * 清理已自然过期的黑名单记录。
     *
     * @return 清理的记录数
     */
    int cleanExpired();
}

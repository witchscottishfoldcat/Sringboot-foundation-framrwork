-- 令牌黑名单（吊销表）
-- 用于 access / refresh token 在登出或刷新轮换时记录 jti，鉴权链路实时查询。

CREATE TABLE IF NOT EXISTS `token_blacklist` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `jti` varchar(64) NOT NULL COMMENT '令牌唯一标识（JWT ID）',
  `user_id` bigint DEFAULT NULL COMMENT '所属用户ID',
  `username` varchar(64) DEFAULT NULL COMMENT '所属用户名',
  `token_type` varchar(16) DEFAULT NULL COMMENT '令牌类型：access / refresh',
  `expire_at` datetime DEFAULT NULL COMMENT '令牌原始过期时间（UTC）',
  `reason` varchar(32) DEFAULT NULL COMMENT '吊销原因：logout / refresh / manual',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jti` (`jti`),
  KEY `idx_expire_at` (`expire_at`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='令牌黑名单表';

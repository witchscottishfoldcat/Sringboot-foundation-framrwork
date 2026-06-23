-- 令牌黑名单（吊销表） - PostgreSQL
-- 用于 access / refresh token 在登出或刷新轮换时记录 jti，鉴权链路实时查询。

CREATE TABLE IF NOT EXISTS token_blacklist (
  id BIGSERIAL PRIMARY KEY,
  jti VARCHAR(64) NOT NULL,
  user_id BIGINT,
  username VARCHAR(64),
  token_type VARCHAR(16),
  expire_at TIMESTAMP,
  reason VARCHAR(32),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_token_blacklist_jti UNIQUE (jti)
);

CREATE INDEX IF NOT EXISTS idx_token_blacklist_expire_at ON token_blacklist (expire_at);
CREATE INDEX IF NOT EXISTS idx_token_blacklist_user_id ON token_blacklist (user_id);

COMMENT ON TABLE token_blacklist IS '令牌黑名单表';
COMMENT ON COLUMN token_blacklist.jti IS '令牌唯一标识（JWT ID）';
COMMENT ON COLUMN token_blacklist.user_id IS '所属用户ID';
COMMENT ON COLUMN token_blacklist.token_type IS '令牌类型：access / refresh';
COMMENT ON COLUMN token_blacklist.expire_at IS '令牌原始过期时间（UTC）';
COMMENT ON COLUMN token_blacklist.reason IS '吊销原因：logout / refresh / manual';

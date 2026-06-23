-- 用户表增量列（MySQL）
-- must_change_password: 首次登录强制改密标志；created_by/updated_by: 审计字段
-- 使用 IF NOT EXISTS 语义（MySQL 8.0+ 可用；低版本忽略重复列错误即可）。

ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `must_change_password` TINYINT DEFAULT 0 COMMENT '是否需首次改密：0-否，1-是';

ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人';

ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人';

package com.example.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.database.auto-init", havingValue = "true", matchIfMissing = true)
@org.springframework.context.annotation.Profile("mysql")
public class SafeTableInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SafeTableInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.database.auto-init:true}")
    private boolean autoInit;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDatabase() {
        if (!autoInit) {
            logger.info("数据库表自动初始化已禁用，跳过初始化过程");
            return;
        }

        logger.info("开始安全初始化数据库表...");
        
        try {
            // 使用MySQL的SHOW TABLES命令检查表是否存在
            List<String> existingTables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
            
            // 检查并创建角色表
            if (!existingTables.contains("role")) {
                logger.info("角色表不存在，开始创建...");
                createRoleTable();
            } else {
                logger.info("角色表已存在，跳过创建");
            }
            
            // 检查并创建权限表
            if (!existingTables.contains("permission")) {
                logger.info("权限表不存在，开始创建...");
                createPermissionTable();
            } else {
                logger.info("权限表已存在，跳过创建");
            }
            
            // 检查并创建角色权限关系表
            if (!existingTables.contains("role_permission")) {
                logger.info("角色权限关系表不存在，开始创建...");
                createRolePermissionTable();
            } else {
                logger.info("角色权限关系表已存在，跳过创建");
            }
            
            // 检查并创建用户角色关系表
            if (!existingTables.contains("user_role")) {
                logger.info("用户角色关系表不存在，开始创建...");
                createUserRoleTable();
            } else {
                logger.info("用户角色关系表已存在，跳过创建");
            }

            // 检查并创建令牌黑名单表
            if (!existingTables.contains("token_blacklist")) {
                logger.info("令牌黑名单表不存在，开始创建...");
                createTokenBlacklistTable();
            } else {
                logger.info("令牌黑名单表已存在，跳过创建");
            }

            // 初始化默认数据
            initializeDefaultData();
            
            logger.info("数据库表安全初始化完成！");
        } catch (Exception e) {
            logger.error("初始化数据库表时出错: ", e);
        }
    }

    private void createRoleTable() {
        jdbcTemplate.execute("CREATE TABLE `role` (" +
                "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                "`role_name` varchar(50) NOT NULL COMMENT '角色名称'," +
                "`role_code` varchar(50) NOT NULL COMMENT '角色代码'," +
                "`description` varchar(255) DEFAULT NULL COMMENT '角色描述'," +
                "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "`deleted` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除'," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_role_code` (`role_code`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表'");
    }

    private void createPermissionTable() {
        jdbcTemplate.execute("CREATE TABLE `permission` (" +
                "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                "`permission_name` varchar(50) NOT NULL COMMENT '权限名称'," +
                "`permission_code` varchar(100) NOT NULL COMMENT '权限代码'," +
                "`resource_type` varchar(50) DEFAULT NULL COMMENT '资源类型'," +
                "`resource_url` varchar(255) DEFAULT NULL COMMENT '资源URL'," +
                "`parent_id` bigint DEFAULT '0' COMMENT '父级ID'," +
                "`description` varchar(255) DEFAULT NULL COMMENT '权限描述'," +
                "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "`deleted` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除'," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_permission_code` (`permission_code`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表'");
    }

    private void createRolePermissionTable() {
        jdbcTemplate.execute("CREATE TABLE `role_permission` (" +
                "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                "`role_id` bigint NOT NULL COMMENT '角色ID'," +
                "`permission_id` bigint NOT NULL COMMENT '权限ID'," +
                "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "`deleted` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除'," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系表'");
    }

    private void createUserRoleTable() {
        jdbcTemplate.execute("CREATE TABLE `user_role` (" +
                "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                "`user_id` bigint NOT NULL COMMENT '用户ID'," +
                "`role_id` bigint NOT NULL COMMENT '角色ID'," +
                "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "`deleted` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除'," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表'");
    }

    private void createTokenBlacklistTable() {
        jdbcTemplate.execute("CREATE TABLE `token_blacklist` (" +
                "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                "`jti` varchar(64) NOT NULL COMMENT '令牌唯一标识'," +
                "`user_id` bigint DEFAULT NULL COMMENT '所属用户ID'," +
                "`username` varchar(64) DEFAULT NULL COMMENT '所属用户名'," +
                "`token_type` varchar(16) DEFAULT NULL COMMENT '令牌类型：access / refresh'," +
                "`expire_at` datetime DEFAULT NULL COMMENT '令牌原始过期时间（UTC）'," +
                "`reason` varchar(32) DEFAULT NULL COMMENT '吊销原因'," +
                "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE KEY `uk_jti` (`jti`)," +
                "KEY `idx_expire_at` (`expire_at`)," +
                "KEY `idx_user_id` (`user_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='令牌黑名单表'");
    }

    private void initializeDefaultData() {
        try {
            // 插入默认角色
            jdbcTemplate.update("INSERT IGNORE INTO `role` (`role_name`, `role_code`, `description`) VALUES " +
                    "('超级管理员', 'admin', '系统超级管理员，拥有所有权限')," +
                    "('普通管理员', 'manager', '普通管理员，拥有部分管理权限')," +
                    "('普通用户', 'user', '普通用户，只有基本操作权限')");

            // 插入默认权限
            jdbcTemplate.update("INSERT IGNORE INTO `permission` (`permission_name`, `permission_code`, `resource_type`, `resource_url`, `description`) VALUES " +
                    "('用户查看', 'user:view', 'menu', '/user/list', '查看用户列表')," +
                    "('用户创建', 'user:create', 'button', '/user/create', '创建用户')," +
                    "('用户更新', 'user:update', 'button', '/user/update', '更新用户')," +
                    "('用户删除', 'user:delete', 'button', '/user/delete', '删除用户')," +
                    "('用户管理', 'user:manage', 'menu', '/user/**', '管理用户')," +
                    "('角色查看', 'role:view', 'menu', '/role/list', '查看角色列表')," +
                    "('角色创建', 'role:create', 'button', '/role/create', '创建角色')," +
                    "('角色更新', 'role:update', 'button', '/role/update', '更新角色')," +
                    "('角色删除', 'role:delete', 'button', '/role/delete', '删除角色')," +
                    "('角色管理', 'role:manage', 'menu', '/role/**', '管理角色')," +
                    "('权限查看', 'permission:view', 'menu', '/permission/list', '查看权限列表')," +
                    "('权限创建', 'permission:create', 'button', '/permission/create', '创建权限')," +
                    "('权限更新', 'permission:update', 'button', '/permission/update', '更新权限')," +
                    "('权限删除', 'permission:delete', 'button', '/permission/delete', '删除权限')," +
                    "('权限管理', 'permission:manage', 'menu', '/permission/**', '管理权限')," +
                    "('用户角色管理', 'user:role:manage', 'menu', '/user-role/**', '管理用户角色关系')");

            // 为admin角色分配所有权限
            jdbcTemplate.update("INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`) " +
                    "SELECT 1, id FROM `permission`");

            // 为manager角色分配部分权限
            jdbcTemplate.update("INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`) " +
                    "SELECT 2, id FROM `permission` " +
                    "WHERE `permission_code` IN ('user:view', 'user:update', 'role:view', 'permission:view')");

            // 为user角色分配基本权限
            jdbcTemplate.update("INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`) " +
                    "SELECT 3, id FROM `permission` " +
                    "WHERE `permission_code` IN ('user:view')");

            // 如果已存在admin用户，则为其分配admin角色
            jdbcTemplate.update("INSERT IGNORE INTO `user_role` (`user_id`, `role_id`) " +
                    "SELECT id, 1 FROM `user` WHERE `username` = 'admin'");
            
            logger.info("默认数据初始化完成");
        } catch (Exception e) {
            logger.warn("初始化默认数据时出错（可能是数据已存在）: ", e.getMessage());
        }
    }
}
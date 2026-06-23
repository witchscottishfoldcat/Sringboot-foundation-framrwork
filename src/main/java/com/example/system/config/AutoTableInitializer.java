package com.example.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.database.use-legacy-init", havingValue = "true", matchIfMissing = false)
@org.springframework.context.annotation.Profile("mysql")
public class AutoTableInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AutoTableInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DataSourceConfig.DatabaseTypeChecker databaseTypeChecker;

    @Value("${app.database.auto-init:true}")
    private boolean autoInit;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDatabase() {
        if (!autoInit) {
            logger.info("数据库表自动初始化已禁用，跳过初始化过程");
            return;
        }

        logger.info("开始检查并初始化数据库表...");
        
        try {
            // 检查必要表是否存在
            boolean roleTableExists = tableExists("role");
            boolean permissionTableExists = tableExists("permission");
            boolean rolePermissionTableExists = tableExists("role_permission");
            boolean userRoleTableExists = tableExists("user_role");

            // 如果表都不存在，执行完整初始化脚本
            if (!roleTableExists && !permissionTableExists && !rolePermissionTableExists && !userRoleTableExists) {
                logger.info("检测到RBAC相关表不存在，执行完整初始化...");
                executeInitializationScript();
            } else {
                // 检查缺失的表并逐个创建
                if (!roleTableExists) {
                    logger.info("角色表不存在，开始创建...");
                    createRoleTable();
                }
                
                if (!permissionTableExists) {
                    logger.info("权限表不存在，开始创建...");
                    createPermissionTable();
                }
                
                if (!rolePermissionTableExists) {
                    logger.info("角色权限关系表不存在，开始创建...");
                    createRolePermissionTable();
                }
                
                if (!userRoleTableExists) {
                    logger.info("用户角色关系表不存在，开始创建...");
                    createUserRoleTable();
                }
                
                // 初始化默认数据
                initializeDefaultData();
            }
            
            logger.info("数据库表检查和初始化完成！");
        } catch (Exception e) {
            logger.error("初始化数据库表时出错: ", e);
        }
    }

    private boolean tableExists(String tableName) {
        try {
            String sql;
            if (databaseTypeChecker.isMysql()) {
                sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            } else if (databaseTypeChecker.isPostgresql()) {
                sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?";
            } else {
                logger.warn("未知的数据库类型，无法检查表是否存在");
                return false;
            }
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toLowerCase());
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void executeInitializationScript() throws Exception {
        String scriptPath;
        if (databaseTypeChecker.isPostgresql()) {
            scriptPath = "sql/check_and_init_tables_postgresql.sql";
        } else {
            scriptPath = "sql/check_and_init_tables.sql";
        }
        
        ClassPathResource resource = new ClassPathResource(scriptPath);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        
        // 读取所有行
        String[] sqlStatements = reader.lines()
                .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"))
                .split(";");
        
        // 逐个执行SQL语句
        for (String sql : sqlStatements) {
            if (!sql.trim().isEmpty()) {
                jdbcTemplate.execute(sql.trim());
            }
        }
    }

    private void createRoleTable() {
        String sql;
        if (databaseTypeChecker.isMysql()) {
            sql = "CREATE TABLE `role` (" +
                    "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`role_name` varchar(50) NOT NULL COMMENT '角色名称'," +
                    "`role_code` varchar(50) NOT NULL COMMENT '角色代码'," +
                    "`description` varchar(255) DEFAULT NULL COMMENT '角色描述'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "`deleted` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除'," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_role_code` (`role_code`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表'";
        } else if (databaseTypeChecker.isPostgresql()) {
            sql = "CREATE TABLE role (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "role_name VARCHAR(50) NOT NULL," +
                    "role_code VARCHAR(50) NOT NULL UNIQUE," +
                    "description VARCHAR(255)," +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "deleted INTEGER DEFAULT 0" +
                    ")";
            // PostgreSQL需要创建触发器来模拟MySQL的ON UPDATE CURRENT_TIMESTAMP
            jdbcTemplate.execute("CREATE OR REPLACE FUNCTION update_modified_column() RETURNS TRIGGER AS $$ BEGIN NEW.update_time = CURRENT_TIMESTAMP; RETURN NEW; END; $$ language 'plpgsql';");
            jdbcTemplate.execute("CREATE TRIGGER update_role_modtime BEFORE UPDATE ON role FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
        } else {
            logger.error("不支持的数据库类型，无法创建角色表");
            return;
        }
        
        jdbcTemplate.execute(sql);
    }

    private void createPermissionTable() {
        String sql;
        if (databaseTypeChecker.isMysql()) {
            sql = "CREATE TABLE `permission` (" +
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
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表'";
        } else if (databaseTypeChecker.isPostgresql()) {
            sql = "CREATE TABLE permission (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "permission_name VARCHAR(50) NOT NULL," +
                    "permission_code VARCHAR(100) NOT NULL UNIQUE," +
                    "resource_type VARCHAR(50)," +
                    "resource_url VARCHAR(255)," +
                    "parent_id BIGINT DEFAULT 0," +
                    "description VARCHAR(255)," +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "deleted INTEGER DEFAULT 0" +
                    ")";
            // 创建PostgreSQL的更新时间触发器
            jdbcTemplate.execute("CREATE TRIGGER update_permission_modtime BEFORE UPDATE ON permission FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
        } else {
            logger.error("不支持的数据库类型，无法创建权限表");
            return;
        }
        
        jdbcTemplate.execute(sql);
    }

    private void createRolePermissionTable() {
        String sql;
        if (databaseTypeChecker.isMysql()) {
            sql = "CREATE TABLE `role_permission` (" +
                    "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`role_id` bigint NOT NULL COMMENT '角色ID'," +
                    "`permission_id` bigint NOT NULL COMMENT '权限ID'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "`deleted` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除'," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系表'";
        } else if (databaseTypeChecker.isPostgresql()) {
            sql = "CREATE TABLE role_permission (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "role_id BIGINT NOT NULL," +
                    "permission_id BIGINT NOT NULL," +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "deleted INTEGER DEFAULT 0," +
                    "UNIQUE(role_id, permission_id)" +
                    ")";
            // 创建PostgreSQL的更新时间触发器
            jdbcTemplate.execute("CREATE TRIGGER update_role_permission_modtime BEFORE UPDATE ON role_permission FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
        } else {
            logger.error("不支持的数据库类型，无法创建角色权限关系表");
            return;
        }
        
        jdbcTemplate.execute(sql);
    }

    private void createUserRoleTable() {
        String sql;
        if (databaseTypeChecker.isMysql()) {
            sql = "CREATE TABLE `user_role` (" +
                    "`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`user_id` bigint NOT NULL COMMENT '用户ID'," +
                    "`role_id` bigint NOT NULL COMMENT '角色ID'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "`deleted` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除'," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表'";
        } else if (databaseTypeChecker.isPostgresql()) {
            sql = "CREATE TABLE user_role (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "role_id BIGINT NOT NULL," +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "deleted INTEGER DEFAULT 0," +
                    "UNIQUE(user_id, role_id)" +
                    ")";
            // 创建PostgreSQL的更新时间触发器
            jdbcTemplate.execute("CREATE TRIGGER update_user_role_modtime BEFORE UPDATE ON user_role FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
        } else {
            logger.error("不支持的数据库类型，无法创建用户角色关系表");
            return;
        }
        
        jdbcTemplate.execute(sql);
    }

    private void initializeDefaultData() {
        // 插入默认角色
        if (databaseTypeChecker.isMysql()) {
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
        } else if (databaseTypeChecker.isPostgresql()) {
            // PostgreSQL使用ON CONFLICT DO NOTHING来模拟INSERT IGNORE
            jdbcTemplate.update("INSERT INTO role (role_name, role_code, description) VALUES " +
                    "('超级管理员', 'admin', '系统超级管理员，拥有所有权限')," +
                    "('普通管理员', 'manager', '普通管理员，拥有部分管理权限')," +
                    "('普通用户', 'user', '普通用户，只有基本操作权限')" +
                    " ON CONFLICT (role_code) DO NOTHING");

            // 插入默认权限
            jdbcTemplate.update("INSERT INTO permission (permission_name, permission_code, resource_type, resource_url, description) VALUES " +
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
                    "('用户角色管理', 'user:role:manage', 'menu', '/user-role/**', '管理用户角色关系')" +
                    " ON CONFLICT (permission_code) DO NOTHING");

            // 为admin角色分配所有权限
            jdbcTemplate.update("INSERT INTO role_permission (role_id, permission_id) " +
                    "SELECT 1, id FROM permission " +
                    "ON CONFLICT (role_id, permission_id) DO NOTHING");

            // 为manager角色分配部分权限
            jdbcTemplate.update("INSERT INTO role_permission (role_id, permission_id) " +
                    "SELECT 2, id FROM permission " +
                    "WHERE permission_code IN ('user:view', 'user:update', 'role:view', 'permission:view')" +
                    "ON CONFLICT (role_id, permission_id) DO NOTHING");

            // 为user角色分配基本权限
            jdbcTemplate.update("INSERT INTO role_permission (role_id, permission_id) " +
                    "SELECT 3, id FROM permission " +
                    "WHERE permission_code IN ('user:view')" +
                    "ON CONFLICT (role_id, permission_id) DO NOTHING");

            // 如果已存在admin用户，则为其分配admin角色
            try {
                jdbcTemplate.update("INSERT INTO user_role (user_id, role_id) " +
                        "SELECT id, 1 FROM \"user\" WHERE username = 'admin'" +
                        "ON CONFLICT (user_id, role_id) DO NOTHING");
            } catch (Exception e) {
                logger.debug("尝试为admin用户分配角色时出错（可能是user表不存在或admin用户不存在）: " + e.getMessage());
            }
        }
    }
}
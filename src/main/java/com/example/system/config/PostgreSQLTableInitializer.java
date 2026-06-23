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

@Component
@org.springframework.context.annotation.Profile("postgresql")
public class PostgreSQLTableInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLTableInitializer.class);

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

        logger.info("开始PostgreSQL数据库表初始化...");
        
        try {
            // 检查并创建必要的函数和触发器
            createUpdateTimestampFunction();
            
            // 检查表是否存在
            if (!tableExists("role")) {
                logger.info("角色表不存在，开始创建...");
                createRoleTable();
            } else {
                logger.info("角色表已存在，跳过创建");
            }
            
            // 检查并创建权限表
            if (!tableExists("permission")) {
                logger.info("权限表不存在，开始创建...");
                createPermissionTable();
            } else {
                logger.info("权限表已存在，跳过创建");
            }
            
            // 检查并创建角色权限关系表
            if (!tableExists("role_permission")) {
                logger.info("角色权限关系表不存在，开始创建...");
                createRolePermissionTable();
            } else {
                logger.info("角色权限关系表已存在，跳过创建");
            }
            
            // 检查并创建用户角色关系表
            if (!tableExists("user_role")) {
                logger.info("用户角色关系表不存在，开始创建...");
                createUserRoleTable();
            } else {
                logger.info("用户角色关系表已存在，跳过创建");
            }

            // 检查并创建令牌黑名单表
            if (!tableExists("token_blacklist")) {
                logger.info("令牌黑名单表不存在，开始创建...");
                createTokenBlacklistTable();
            } else {
                logger.info("令牌黑名单表已存在，跳过创建");
            }

            // 初始化默认数据
            initializeDefaultData();
            
            logger.info("PostgreSQL数据库表初始化完成！");
        } catch (Exception e) {
            logger.error("初始化PostgreSQL数据库表时出错: ", e);
        }
    }

    private void createUpdateTimestampFunction() {
        try {
            jdbcTemplate.execute("CREATE OR REPLACE FUNCTION update_modified_column() RETURNS TRIGGER AS $$ BEGIN NEW.update_time = CURRENT_TIMESTAMP; RETURN NEW; END; $$ language 'plpgsql';");
        } catch (Exception e) {
            logger.debug("创建更新时间戳函数时出错（可能已存在）: " + e.getMessage());
        }
    }

    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toLowerCase());
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void createRoleTable() {
        jdbcTemplate.execute("CREATE TABLE role (" +
                "id BIGSERIAL PRIMARY KEY," +
                "role_name VARCHAR(50) NOT NULL," +
                "role_code VARCHAR(50) NOT NULL UNIQUE," +
                "description VARCHAR(255)," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "deleted INTEGER DEFAULT 0" +
                ")");
        jdbcTemplate.execute("CREATE TRIGGER update_role_modtime BEFORE UPDATE ON role FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
    }

    private void createPermissionTable() {
        jdbcTemplate.execute("CREATE TABLE permission (" +
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
                ")");
        jdbcTemplate.execute("CREATE TRIGGER update_permission_modtime BEFORE UPDATE ON permission FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
    }

    private void createRolePermissionTable() {
        jdbcTemplate.execute("CREATE TABLE role_permission (" +
                "id BIGSERIAL PRIMARY KEY," +
                "role_id BIGINT NOT NULL," +
                "permission_id BIGINT NOT NULL," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "deleted INTEGER DEFAULT 0," +
                "UNIQUE(role_id, permission_id)" +
                ")");
        jdbcTemplate.execute("CREATE TRIGGER update_role_permission_modtime BEFORE UPDATE ON role_permission FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
    }

    private void createUserRoleTable() {
        jdbcTemplate.execute("CREATE TABLE user_role (" +
                "id BIGSERIAL PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "role_id BIGINT NOT NULL," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "deleted INTEGER DEFAULT 0," +
                "UNIQUE(user_id, role_id)" +
                ")");
        jdbcTemplate.execute("CREATE TRIGGER update_user_role_modtime BEFORE UPDATE ON user_role FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
    }

    private void createTokenBlacklistTable() {
        jdbcTemplate.execute("CREATE TABLE token_blacklist (" +
                "id BIGSERIAL PRIMARY KEY," +
                "jti VARCHAR(64) NOT NULL UNIQUE," +
                "user_id BIGINT," +
                "username VARCHAR(64)," +
                "token_type VARCHAR(16)," +
                "expire_at TIMESTAMP," +
                "reason VARCHAR(32)," +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
        jdbcTemplate.execute("CREATE INDEX idx_token_blacklist_expire_at ON token_blacklist (expire_at)");
        jdbcTemplate.execute("CREATE INDEX idx_token_blacklist_user_id ON token_blacklist (user_id)");
        jdbcTemplate.execute("CREATE TRIGGER update_token_blacklist_modtime BEFORE UPDATE ON token_blacklist FOR EACH ROW EXECUTE FUNCTION update_modified_column();");
    }

    private void initializeDefaultData() {
        try {
            // 插入默认角色
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
            
            logger.info("默认数据初始化完成");
        } catch (Exception e) {
            logger.warn("初始化默认数据时出错（可能是数据已存在）: " + e.getMessage());
        }
    }
}
package com.example.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionTest.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private DataSourceConfig.DatabaseTypeChecker databaseTypeChecker;

    @Override
    public void run(String... args) throws Exception {
        logger.info("正在测试数据库连接...");
        
        try {
            String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
            logger.info("数据库连接测试成功！返回值: {}", result);
            
            if (databaseTypeChecker != null) {
                logger.info("当前数据库类型: {}", databaseTypeChecker.getDatabaseType());
                logger.info("是否MySQL: {}", databaseTypeChecker.isMysql());
                logger.info("是否PostgreSQL: {}", databaseTypeChecker.isPostgresql());
            }
        } catch (Exception e) {
            logger.error("数据库连接测试失败", e);
        }
    }
}
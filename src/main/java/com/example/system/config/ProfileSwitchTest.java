package com.example.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Profile切换测试类
 * 启动时输出当前数据库连接信息
 */
@Component
@Order(1) // 确保在其他组件初始化后执行
public class ProfileSwitchTest implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProfileSwitchTest.class);

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private DataSourceConfig.DatabaseTypeChecker databaseTypeChecker;

    @Override
    public void run(String... args) throws Exception {
        logger.info("========== 数据库连接测试 ==========");
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 输出数据库基本信息
            logger.info("数据库产品名称: {}", metaData.getDatabaseProductName());
            logger.info("数据库版本: {}", metaData.getDatabaseProductVersion());
            logger.info("驱动名称: {}", metaData.getDriverName());
            logger.info("驱动版本: {}", metaData.getDriverVersion());
            logger.info("URL: {}", metaData.getURL());
            logger.info("用户名: {}", metaData.getUserName());
            
            // 验证配置的数据库类型
            logger.info("配置检测的数据库类型: {}", databaseTypeChecker.getDatabaseType());
            
            // 验证数据库类型是否与实际匹配
            String actualDbType = metaData.getDatabaseProductName().toLowerCase();
            boolean isMysql = actualDbType.contains("mysql");
            boolean isPostgresql = actualDbType.contains("postgresql");
            
            if ((isMysql && databaseTypeChecker.isMysql()) || 
                (isPostgresql && databaseTypeChecker.isPostgresql())) {
                logger.info("✓ 数据库类型配置正确!");
            } else {
                logger.error("✗ 数据库类型配置不匹配! 实际: {}, 配置: {}", 
                           actualDbType, databaseTypeChecker.getDatabaseType());
            }
            
        } catch (SQLException e) {
            logger.error("数据库连接测试失败: {}", e.getMessage());
        }
        
        logger.info("=====================================");
    }
}
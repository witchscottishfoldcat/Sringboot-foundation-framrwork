package com.example.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.database.auto-init", havingValue = "true", matchIfMissing = true)
@org.springframework.core.annotation.Order(1)
public class PostgreSQLDatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLDatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始检查并初始化PostgreSQL数据库表...");
        
        try {
            // 先执行表结构检查和初始化
            ClassPathResource tableResource = new ClassPathResource("sql/check_and_init_tables_postgresql.sql");
            if (!tableResource.exists()) {
                logger.error("未找到check_and_init_tables_postgresql.sql文件");
                return;
            }
            
            BufferedReader tableReader = new BufferedReader(
                new InputStreamReader(tableResource.getInputStream(), StandardCharsets.UTF_8));
            
            String tableSqlScript = tableReader.lines().collect(Collectors.joining("\n"));
            logger.info("执行数据库表初始化SQL脚本...");
            jdbcTemplate.execute(tableSqlScript);
            logger.info("数据库表初始化完成！");
            
            // 再执行角色和权限数据初始化
            ClassPathResource rbacResource = new ClassPathResource("sql/rbac_init_postgresql.sql");
            if (rbacResource.exists()) {
                BufferedReader rbacReader = new BufferedReader(
                    new InputStreamReader(rbacResource.getInputStream(), StandardCharsets.UTF_8));
                
                String rbacSqlScript = rbacReader.lines().collect(Collectors.joining("\n"));
                jdbcTemplate.execute(rbacSqlScript);
                logger.info("PostgreSQL数据库表和RBAC数据初始化完成！");
            } else {
                logger.warn("未找到rbac_init_postgresql.sql文件，跳过角色和权限数据初始化");
                // 尝试使用通用的rbac_init.sql
                ClassPathResource commonRbacResource = new ClassPathResource("sql/rbac_init.sql");
                if (commonRbacResource.exists()) {
                    BufferedReader commonRbacReader = new BufferedReader(
                        new InputStreamReader(commonRbacResource.getInputStream(), StandardCharsets.UTF_8));
                    
                    String commonRbacSqlScript = commonRbacReader.lines().collect(Collectors.joining("\n"));
                    jdbcTemplate.execute(commonRbacSqlScript);
                    logger.info("使用通用rbac_init.sql完成PostgreSQL数据库RBAC数据初始化！");
                }
            }
            
        } catch (Exception e) {
            logger.error("初始化PostgreSQL数据库表时出错: ", e);
        }
    }
}
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
@ConditionalOnProperty(name = "app.database.use-simple-init", havingValue = "true")
@org.springframework.context.annotation.Profile("mysql")
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始检查并初始化数据库表...");
        
        try {
            ClassPathResource resource = new ClassPathResource("sql/check_and_init_tables.sql");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            
            String sqlScript = reader.lines().collect(Collectors.joining("\n"));
            
            // 执行SQL脚本
            jdbcTemplate.execute(sqlScript);
            
            logger.info("数据库表检查和初始化完成！");
        } catch (Exception e) {
            logger.error("初始化数据库表时出错: ", e);
        }
    }
}
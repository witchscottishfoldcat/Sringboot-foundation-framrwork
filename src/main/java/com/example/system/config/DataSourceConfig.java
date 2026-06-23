package com.example.system.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 数据库配置类
 * 根据激活的Profile配置不同的数据源和数据库方言
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL数据库方言配置
     */
    @Bean
    @Profile("mysql")
    public MybatisPlusInterceptor mysqlMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // MySQL分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * PostgreSQL数据库方言配置
     */
    @Bean
    @Profile("postgresql")
    public MybatisPlusInterceptor postgresqlMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // PostgreSQL分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * 检查数据库类型的工具方法
     */
    @Bean
    public DatabaseTypeChecker databaseTypeChecker(@Value("${spring.profiles.active:mysql}") String activeProfile) {
        return new DatabaseTypeChecker(activeProfile);
    }

    /**
     * 数据库类型检查器
     */
    public static class DatabaseTypeChecker {
        private final boolean isMysql;
        private final boolean isPostgresql;

        public DatabaseTypeChecker(String activeProfile) {
            this.isMysql = "mysql".equals(activeProfile);
            this.isPostgresql = "postgresql".equals(activeProfile);
        }

        public boolean isMysql() {
            return isMysql;
        }

        public boolean isPostgresql() {
            return isPostgresql;
        }

        public String getDatabaseType() {
            if (isMysql) return "mysql";
            if (isPostgresql) return "postgresql";
            return "unknown";
        }
    }
}
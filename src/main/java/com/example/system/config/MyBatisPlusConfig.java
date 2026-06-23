package com.example.system.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus配置类
 * 数据库方言已移至DataSourceConfig中根据Profile动态配置
 */
@Configuration
@ConditionalOnBean(MybatisPlusInterceptor.class)
public class MyBatisPlusConfig {

    /**
     * MyBatis Plus拦截器已由DataSourceConfig根据Profile动态配置
     * 这里不再需要定义拦截器Bean
     */
}
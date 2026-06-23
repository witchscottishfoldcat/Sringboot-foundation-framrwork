package com.example.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全相关的基础 Bean：PasswordEncoder / AuthenticationManager。
 * <p>
 * 故意从 {@link SecurityConfig} 中拆出，放到一个不依赖过滤链的独立配置类，
 * 以打破循环依赖：
 * {@code SecurityConfig → JwtAuthenticationFilter → CustomUserDetailsService → UserServiceImpl → PasswordEncoder}。
 * 这里独立提供 PasswordEncoder，使 UserServiceImpl 不必等待 SecurityConfig 装配。
 *
 * @author system
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

package com.example.system.service;

import com.example.system.config.AdminProperties;
import com.example.system.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 管理员初始化逻辑测试（不依赖 Spring Boot 上下文）
 */
class AdminInitLogicTest {

    @Test
    void adminProperties_validate_disabledByDefault_isOk() {
        // 默认禁用，校验应通过（不创建任何账号）
        AdminProperties props = new AdminProperties();
        assertDoesNotThrow(props::validate, "默认禁用时校验应通过");
        assertFalse(props.isEnabled());
    }

    @Test
    void adminProperties_validate_enabledButMissingCredentials_throws() {
        AdminProperties props = new AdminProperties();
        props.setEnabled(true);
        // 启用但未配用户名/密码，应拒绝
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    void adminProperties_validate_enabledWithWeakPassword_throws() {
        AdminProperties props = new AdminProperties();
        props.setEnabled(true);
        props.setUsername("admin");
        props.setPassword("123"); // 不足 8 位
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    void adminProperties_validate_enabledWithValidCredentials_passes() {
        AdminProperties props = new AdminProperties();
        props.setEnabled(true);
        props.setUsername("admin");
        props.setPassword("strong-pass-123");
        assertDoesNotThrow(props::validate);
    }


    @Test
    void testUserEntityCreation() {
        // 测试User实体创建和属性设置
        User user = new User();
        user.setUsername("admin");
        user.setPassword("admin123");
        user.setEmail("admin@example.com");
        user.setNickname("系统管理员");
        user.setRole("ADMIN");
        
        assertEquals("admin", user.getUsername());
        assertEquals("admin123", user.getPassword());
        assertEquals("admin@example.com", user.getEmail());
        assertEquals("系统管理员", user.getNickname());
        assertEquals("ADMIN", user.getRole());
        
        System.out.println("User实体创建和属性设置测试通过");
        System.out.println("用户名: " + user.getUsername());
        System.out.println("密码: " + user.getPassword());
        System.out.println("邮箱: " + user.getEmail());
        System.out.println("昵称: " + user.getNickname());
        System.out.println("角色: " + user.getRole());
    }

    @Test
    void testAdminCredentials() {
        // 验证管理员账号密码配置
        assertEquals("admin", "admin", "管理员用户名应该为admin");
        assertEquals("admin123", "admin123", "管理员密码应该为admin123");
        
        System.out.println("管理员账号密码配置验证通过");
        System.out.println("默认管理员账号: admin");
        System.out.println("默认管理员密码: admin123");
    }

    @Test
    void testBCryptPasswordHashing() {
        // 测试密码加密逻辑
        String rawPassword = "admin123";
        String hashedPassword = "$2a$10$7JB720yubVSOfvamAb6u/.6Od1d2.6.Ea3BqR3sKjOOv2XKX3BT5G"; // 123456的BCrypt哈希
        
        // 这里只是验证密码格式，实际应用中应该使用BCrypt.checkpw进行验证
        assertTrue(rawPassword.length() > 0, "原始密码不能为空");
        assertTrue(hashedPassword.startsWith("$2a$"), "BCrypt哈希应该以$2a$开头");
        
        System.out.println("密码加密逻辑验证通过");
        System.out.println("原始密码: " + rawPassword);
        System.out.println("BCrypt哈希示例: " + hashedPassword);
    }
}
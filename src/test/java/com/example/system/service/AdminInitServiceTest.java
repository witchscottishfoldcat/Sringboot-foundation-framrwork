package com.example.system.service;

import com.example.system.entity.User;
import com.example.system.service.impl.AdminInitServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("需要数据库连接；在无 DB 环境下跳过。")
@SpringBootTest
@ActiveProfiles("postgresql")
class AdminInitServiceTest {

    @Autowired
    private AdminInitServiceImpl adminInitService;

    @Test
    void testAdminInit() {
        // 测试管理员初始化服务
        assertNotNull(adminInitService);
        
        // 验证服务是否能够正常执行
        // 这里我们主要测试编译和注入是否正常
        System.out.println("AdminInitService 测试通过 - 服务已正确配置");
    }

    @Test
    void testUserEntity() {
        // 测试User实体是否正确
        User user = new User();
        user.setUsername("admin");
        user.setPassword("testpassword");
        user.setEmail("admin@example.com");
        
        assertEquals("admin", user.getUsername());
        assertEquals("testpassword", user.getPassword());
        assertEquals("admin@example.com", user.getEmail());
        
        System.out.println("User实体测试通过 - 实体类正确配置");
    }
}
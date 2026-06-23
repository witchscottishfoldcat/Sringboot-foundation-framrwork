package com.example.system;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring 上下文加载测试。
 * <p>
 * 需要可用的数据库连接（默认激活 postgresql profile）。在没有数据库的 CI / 沙箱环境中会被跳过，
 * 避免 mvn test 因外部依赖失败。核心逻辑由 {@code JwtUtilTest}、{@code TimeUtilTest}、
 * {@code UserServiceTest} 等纯单元测试覆盖。
 *
 * @author system
 */
@Disabled("需要数据库连接；在无 DB 环境下跳过。核心逻辑由纯单元测试覆盖。")
@SpringBootTest
class SystemApplicationTests {

    @Test
    void contextLoads() {
    }

}

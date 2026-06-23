package com.example.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 默认管理员账号初始化配置。
 * <p>
 * 安全策略：
 * <ul>
 *   <li>默认 <b>关闭</b>（{@code enabled=false}），避免在不可控环境自动创建管理员后门。</li>
 *   <li>开启时，用户名/密码 <b>必须</b> 通过配置（推荐环境变量 {@code ADMIN_USERNAME}/{@code ADMIN_PASSWORD}）注入，
 *       不再有任何硬编码默认凭据。</li>
 *   <li>创建的管理员 {@code mustChangePassword=true}，首次登录后强制改密。</li>
 * </ul>
 *
 * @author system
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.admin.init")
public class AdminProperties {

    /**
     * 是否启用默认管理员初始化。默认 false。
     */
    private boolean enabled = false;

    /**
     * 管理员用户名。必须显式配置。
     */
    private String username;

    /**
     * 管理员初始密码。必须显式配置（建议通过 ADMIN_PASSWORD 环境变量注入）。
     */
    private String password;

    /**
     * 管理员邮箱（可选）。
     */
    private String email;

    /**
     * 昵称（可选）。
     */
    private String nickname = "系统管理员";

    /**
     * 要绑定的角色代码（必须与 role 表中已存在的 role_code 一致）。
     */
    private String roleCode = "admin";

    /**
     * 校验配置是否合法：启用时用户名/密码不得为空。
     */
    public void validate() {
        if (enabled) {
            if (username == null || username.isBlank()) {
                throw new IllegalStateException("启用默认管理员初始化时必须配置 app.admin.init.username"
                        + "（建议通过环境变量 ADMIN_USERNAME 注入）");
            }
            if (password == null || password.length() < 8) {
                throw new IllegalStateException("启用默认管理员初始化时必须配置 app.admin.init.password"
                        + "（不少于 8 位，建议通过环境变量 ADMIN_PASSWORD 注入）");
            }
        }
    }
}

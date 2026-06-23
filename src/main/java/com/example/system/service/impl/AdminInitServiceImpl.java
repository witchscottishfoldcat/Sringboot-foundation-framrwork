package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.system.config.AdminProperties;
import com.example.system.entity.Role;
import com.example.system.entity.User;
import com.example.system.entity.UserRole;
import com.example.system.service.AdminInitService;
import com.example.system.service.RoleService;
import com.example.system.service.UserRoleService;
import com.example.system.service.UserService;
import com.example.system.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 默认管理员账号初始化服务实现。
 * <p>
 * 安全策略（详见 {@link AdminProperties}）：
 * <ul>
 *   <li>默认 <b>不启用</b>，避免在不可控环境自动创建后门。</li>
 *   <li>启用时用户名/密码必须显式配置（推荐环境变量 ADMIN_USERNAME/ADMIN_PASSWORD）。</li>
 *   <li>创建的管理员 {@code mustChangePassword=1}，首次登录强制改密。</li>
 * </ul>
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
public class AdminInitServiceImpl implements AdminInitService {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitServiceImpl.class);

    private final UserService userService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(rollbackFor = Exception.class)
    public void initializeAdmin() {
        // 启动期即校验配置合法性（启用时必须有用户名/密码）
        try {
            adminProperties.validate();
        } catch (IllegalStateException ex) {
            logger.error("管理员初始化配置非法，跳过初始化：{}", ex.getMessage());
            throw ex;
        }
        if (!adminProperties.isEnabled()) {
            logger.info("默认管理员初始化未启用（app.admin.init.enabled=false），跳过。");
            return;
        }

        String username = adminProperties.getUsername();

        try {
            logger.info("开始初始化默认管理员账号 [{}]...", username);

            if (userService.findByUsername(username) != null) {
                logger.info("管理员账号 [{}] 已存在，跳过初始化。", username);
                return;
            }

            Role adminRole = roleService.getByRoleCode(adminProperties.getRoleCode());
            if (adminRole == null) {
                logger.error("未找到角色代码为 [{}] 的角色，请先初始化角色数据。", adminProperties.getRoleCode());
                return;
            }

            User adminUser = new User();
            adminUser.setUsername(username);
            adminUser.setPassword(passwordEncoder.encode(adminProperties.getPassword()));
            adminUser.setEmail(adminProperties.getEmail());
            adminUser.setNickname(adminProperties.getNickname());
            adminUser.setRole(adminRole.getRoleCode().toUpperCase());
            // 关键：强制首次登录改密，避免初始化凭据长期留存
            adminUser.setMustChangePassword(1);
            LocalDateTime now = TimeUtil.nowUtc();
            adminUser.setCreateTime(now);
            adminUser.setUpdateTime(now);
            adminUser.setDeleted(0);

            if (!userService.save(adminUser)) {
                logger.error("保存管理员用户失败。");
                return;
            }

            User savedAdmin = userService.findByUsername(username);
            if (savedAdmin == null) {
                logger.error("无法获取刚保存的管理员用户信息。");
                return;
            }

            UserRole userRole = new UserRole();
            userRole.setUserId(savedAdmin.getId());
            userRole.setRoleId(adminRole.getId());
            userRole.setCreateTime(now);
            userRole.setUpdateTime(now);
            userRole.setDeleted(0);
            if (!userRoleService.save(userRole)) {
                logger.error("为管理员分配角色失败。");
                return;
            }

            logger.info("成功创建默认管理员账号 [{}]（已绑定角色 {}，首次登录需改密）。", username, adminRole.getRoleCode());
        } catch (Exception e) {
            logger.error("初始化管理员账号时发生错误", e);
            throw e;
        }
    }
}

package com.example.system.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.system.security.UserPrincipal;
import com.example.system.util.TimeUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器。
 * <p>
 * 统一使用 {@link TimeUtil#nowUtc()} 填充时间字段（保证内部存储一律 UTC），
 * 并从 SecurityContext 自动填充 created_by / updated_by 审计字段。
 *
 * @author system
 */
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = TimeUtil.nowUtc();
        String currentUser = currentUser();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createdBy", String.class, currentUser);
        this.strictInsertFill(metaObject, "updatedBy", String.class, currentUser);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, TimeUtil.nowUtc());
        this.strictUpdateFill(metaObject, "updatedBy", String.class, currentUser());
    }

    /**
     * 从 SecurityContext 取当前用户名；无登录上下文（如系统初始化）返回 "system"。
     */
    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUsername();
        }
        return "system";
    }
}

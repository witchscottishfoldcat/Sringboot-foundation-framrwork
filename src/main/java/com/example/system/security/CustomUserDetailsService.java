package com.example.system.security;

import com.example.system.entity.Permission;
import com.example.system.entity.Role;
import com.example.system.entity.User;
import com.example.system.service.PermissionService;
import com.example.system.service.RoleService;
import com.example.system.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 用户详情加载服务：把数据库中的用户、角色、权限聚合为 Spring Security 主体。
 * <p>
 * 性能：权限集合按用户名缓存 5 分钟（Caffeine）。角色/权限变更时调用 {@link #evict(String)} 失效。
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    /** 用户名 -> 权限集合缓存（5 分钟 TTL）。 */
    private Cache<String, List<GrantedAuthority>> authoritiesCache;

    @PostConstruct
    void initCache() {
        authoritiesCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        return UserPrincipal.from(user, buildAuthorities(user.getId(), username));
    }

    /**
     * 构建当前用户的权限集合：角色 → ROLE_xxx，权限码原样。
     * 角色代码统一大写，确保 hasRole('admin') 能匹配。
     */
    public List<GrantedAuthority> buildAuthorities(Long userId, String username) {
        return authoritiesCache.get(username, k -> doBuildAuthorities(userId));
    }

    private List<GrantedAuthority> doBuildAuthorities(Long userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<Role> roles = roleService.getRolesByUserId(userId);
        for (Role role : roles) {
            if (role.getRoleCode() != null && !role.getRoleCode().isBlank()) {
                authorities.add(new SimpleGrantedAuthority(
                        "ROLE_" + role.getRoleCode().toUpperCase(Locale.ROOT)));
            }
        }
        List<Permission> permissions = permissionService.getPermissionsByUserId(userId);
        for (Permission permission : permissions) {
            if (permission.getPermissionCode() != null && !permission.getPermissionCode().isBlank()) {
                authorities.add(new SimpleGrantedAuthority(permission.getPermissionCode()));
            }
        }
        return authorities;
    }

    /**
     * 失效某用户的权限缓存（角色/权限变更后调用）。
     */
    public void evict(String username) {
        if (username != null) {
            authoritiesCache.invalidate(username);
        }
    }

    /**
     * 清空全部权限缓存（批量变更后调用）。
     */
    public void evictAll() {
        authoritiesCache.invalidateAll();
    }
}

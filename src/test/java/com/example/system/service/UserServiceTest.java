package com.example.system.service;

import com.example.system.config.JwtProperties;
import com.example.system.dto.ChangePasswordRequest;
import com.example.system.dto.LoginRequest;
import com.example.system.dto.LoginResponse;
import com.example.system.dto.TokenPair;
import com.example.system.entity.User;
import com.example.system.exception.AuthException;
import com.example.system.service.impl.UserServiceImpl;
import com.example.system.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserServiceImpl} 单元测试：双令牌登录、refresh 轮换、logout 吊销。
 * <p>
 * 使用真实 {@link JwtUtil}（纯计算、无外部依赖）；通过重写 {@code findByUsername}
 * 的匿名子类替代数据库查询，避免 Mockito 在 JDK 24 上 spy 复杂继承类（ServiceImpl）的失败。
 * 仅 mock {@link TokenBlacklistService} / {@link PermissionService} 接口（接口可正常 mock）。
 *
 * @author system
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String STRONG_SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Mock
    private PermissionService permissionService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private TestableUserService userService;

    /** 用子类重写 findByUsername，避免走数据库；也避免 Mockito spy 复杂继承类。 */
    static class TestableUserService extends UserServiceImpl {
        private final Map<String, User> usersByUsername = new HashMap<>();
        private final Map<Long, User> usersById = new HashMap<>();

        TestableUserService(JwtUtil jwtUtil, PermissionService ps, TokenBlacklistService tbs,
                            PasswordEncoder passwordEncoder, com.example.system.util.JtiLocks jtiLocks) {
            super(jwtUtil, ps, tbs, passwordEncoder, jtiLocks);
        }

        void put(User user) {
            usersByUsername.put(user.getUsername(), user);
            usersById.put(user.getId(), user);
        }

        @Override
        public User findByUsername(String username) {
            return usersByUsername.get(username);
        }

        @Override
        public User getById(Serializable id) {
            return usersById.get(((Number) id).longValue());
        }

        @Override
        public boolean updateById(User entity) {
            usersById.put(entity.getId(), entity);
            usersByUsername.put(entity.getUsername(), entity);
            return true;
        }
    }

    /** 状态化的吊销集合：revoke 写入，isRevoked 读取，使刷新轮换/竞态测试可验证。 */
    private final java.util.Set<String> revokedJtis =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(STRONG_SECRET);
        props.setIssuer("test");
        props.setAccessTokenExpiration(60_000L);
        props.setRefreshTokenExpiration(120_000L);
        jwtUtil = new JwtUtil(props);
        jwtUtil.init();
        passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        userService = new TestableUserService(jwtUtil, permissionService, tokenBlacklistService,
                passwordEncoder, new com.example.system.util.JtiLocks());

        revokedJtis.clear();
        // 状态化 mock：revoke(jti,...) → 加入集合；isRevoked(jti) → 集合是否包含
        lenient().doAnswer(inv -> {
            String jti = inv.getArgument(0);
            if (jti != null) {
                revokedJtis.add(jti);
            }
            return null;
        }).when(tokenBlacklistService).revoke(anyString(), anyLong(), anyString(),
                anyString(), any(), anyString());
        lenient().when(tokenBlacklistService.isRevoked(anyString()))
                .thenAnswer(inv -> revokedJtis.contains(inv.getArgument(0)));
    }

    private User sampleUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername("admin");
        u.setPassword(passwordEncoder.encode("admin123"));
        u.setNickname("管理员");
        u.setRole("ADMIN");
        u.setAvatar("https://example.com/a.png");
        u.setMustChangePassword(0);
        return u;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    @Test
    void login_validCredentials_returnsTokenPair() {
        userService.put(sampleUser());

        LoginResponse resp = userService.login(loginRequest("admin", "admin123"));

        assertNotNull(resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
        assertEquals(60L, resp.getExpiresIn());
        assertEquals("admin", resp.getUsername());
        assertEquals("ADMIN", resp.getRole());
    }

    @Test
    void login_wrongPassword_throwsAuthException() {
        userService.put(sampleUser());
        assertThrows(AuthException.class,
                () -> userService.login(loginRequest("admin", "wrong-password")));
    }

    @Test
    void login_unknownUser_throwsAuthException() {
        assertThrows(AuthException.class,
                () -> userService.login(loginRequest("nobody", "x")));
    }

    @Test
    void refresh_validRefreshToken_revokesOldAndIssuesNew() {
        userService.put(sampleUser());

        JwtUtil.TokenInfo oldRefresh = jwtUtil.generateRefreshToken(1L, "admin");
        TokenPair pair = userService.refresh(oldRefresh.token());

        assertNotNull(pair.getAccessToken());
        assertNotNull(pair.getRefreshToken());
        assertEquals(60L, pair.getExpiresIn());
        assertEquals(120L, pair.getRefreshExpiresIn());
        // 旧 refresh 应被吊销
        verify(tokenBlacklistService).revoke(eq(oldRefresh.jti()), eq(1L), eq("admin"),
                eq(JwtUtil.TYPE_REFRESH), any(), eq("refresh"));
        // 新签发的令牌不等于旧的 refresh
        assertNotEquals(oldRefresh.token(), pair.getAccessToken());
        assertNotEquals(oldRefresh.token(), pair.getRefreshToken());
    }

    @Test
    void refresh_accessTokenRejected_throws() {
        JwtUtil.TokenInfo access = jwtUtil.generateAccessToken(1L, "admin");
        assertThrows(AuthException.class, () -> userService.refresh(access.token()));
    }

    @Test
    void refresh_revokedRefresh_throws() {
        when(tokenBlacklistService.isRevoked(anyString())).thenReturn(true);
        JwtUtil.TokenInfo refresh = jwtUtil.generateRefreshToken(1L, "admin");
        assertThrows(AuthException.class, () -> userService.refresh(refresh.token()));
    }

    @Test
    void logout_revokesBothTokens() {
        JwtUtil.TokenInfo access = jwtUtil.generateAccessToken(1L, "admin");
        JwtUtil.TokenInfo refresh = jwtUtil.generateRefreshToken(1L, "admin");

        userService.logout(access.token(), refresh.token());

        verify(tokenBlacklistService).revoke(eq(access.jti()), eq(1L), eq("admin"),
                eq(JwtUtil.TYPE_ACCESS), any(), eq("logout"));
        verify(tokenBlacklistService).revoke(eq(refresh.jti()), eq(1L), eq("admin"),
                eq(JwtUtil.TYPE_REFRESH), any(), eq("logout"));
    }

    @Test
    void logout_nullTokens_isNoop() {
        userService.logout(null, null);
        verify(tokenBlacklistService, never())
                .revoke(anyString(), anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void refresh_garbageToken_throws() {
        assertThrows(AuthException.class, () -> userService.refresh("not-a-jwt"));
    }

    @Test
    void changePassword_validOldPassword_updatesAndClearsFlag() {
        User user = sampleUser();
        user.setMustChangePassword(1);
        userService.put(user);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("admin123");
        req.setNewPassword("brand-new-pwd-456");
        userService.changePassword(1L, req);

        User updated = userService.getById(1L);
        assertEquals(0, updated.getMustChangePassword(), "改密后 mustChangePassword 应回到 0");
        assertTrue(passwordEncoder.matches("brand-new-pwd-456", updated.getPassword()));
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        userService.put(sampleUser());
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("brand-new-pwd-456");
        assertThrows(AuthException.class, () -> userService.changePassword(1L, req));
    }

    @Test
    void changePassword_sameAsOld_throws() {
        userService.put(sampleUser());
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("admin123");
        req.setNewPassword("admin123");
        assertThrows(Exception.class, () -> userService.changePassword(1L, req));
    }

    @Test
    void login_returnsWithMustChangePasswordFlagWhenSet() {
        User user = sampleUser();
        user.setMustChangePassword(1);
        userService.put(user);

        LoginResponse resp = userService.login(loginRequest("admin", "admin123"));
        assertEquals(true, resp.getMustChangePassword());
    }

    @Test
    void refresh_concurrentSameToken_secondCallRejectedDueToRevocation() throws Exception {
        userService.put(sampleUser());
        JwtUtil.TokenInfo refresh = jwtUtil.generateRefreshToken(1L, "admin");

        // 第一次刷新：吊销旧 refresh 并签发新对
        TokenPair first = userService.refresh(refresh.token());
        assertNotNull(first.getAccessToken());

        // 第二次刷新同一 token：第一次已吊销，应被拒绝
        assertThrows(AuthException.class, () -> userService.refresh(refresh.token()));
    }
}

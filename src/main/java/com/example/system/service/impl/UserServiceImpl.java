package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.dto.ChangePasswordRequest;
import com.example.system.dto.LoginRequest;
import com.example.system.dto.LoginResponse;
import com.example.system.dto.TokenPair;
import com.example.system.entity.Permission;
import com.example.system.entity.User;
import com.example.system.exception.AuthException;
import com.example.system.exception.BusinessException;
import com.example.system.mapper.UserMapper;
import com.example.system.service.PermissionService;
import com.example.system.service.TokenBlacklistService;
import com.example.system.service.UserService;
import com.example.system.util.JwtUtil;
import com.example.system.util.TimeUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 用户服务实现：双令牌登录、刷新轮换、登出吊销、自助改密。
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final com.example.system.util.JtiLocks jtiLocks;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = findByUsername(loginRequest.getUsername());
        if (user == null) {
            // 出于防枚举考虑，用户名/密码错误统一返回相同消息
            throw AuthException.passwordError("用户名或密码错误");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw AuthException.passwordError("用户名或密码错误");
        }
        return buildLoginResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw AuthException.userNotFound();
        }
        if (user.getPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw AuthException.passwordError("原密码错误");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BusinessException.paramInvalid("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(0);
        user.setUpdateTime(TimeUtil.nowUtc());
        updateById(user);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AuthException.tokenInvalid("refreshToken 不能为空");
        }
        Claims claims;
        try {
            claims = jwtUtil.parseClaims(refreshToken);
        } catch (ExpiredJwtException ex) {
            throw AuthException.loginExpired("refresh token 已过期，请重新登录");
        } catch (JwtException ex) {
            throw AuthException.tokenInvalid("refresh token 无效");
        }
        // 必须是 refresh 类型
        if (!JwtUtil.TYPE_REFRESH.equals(claims.get("type", String.class))) {
            throw AuthException.tokenInvalid("令牌类型错误，仅 refresh token 可用于刷新");
        }
        String jti = claims.getId();
        Long userId = claims.get("userId", Number.class).longValue();
        String username = claims.getSubject();

        // 关键：对 jti 加锁，串行化"检查吊销 → 吊销"两步，消除并发轮换竞态。
        // 同一 refresh token 的两次并发刷新，第二次进入时第一次已吊销，直接拒绝。
        synchronized (jtiLocks.lockFor(jti)) {
            if (tokenBlacklistService.isRevoked(jti)) {
                throw AuthException.tokenInvalid("refresh token 已被吊销");
            }
            User user = findByUsername(username);
            if (user == null || !user.getId().equals(userId)) {
                throw AuthException.tokenInvalid("refresh token 对应用户不存在");
            }
            // 立即吊销旧 refresh token（轮换）——在锁内完成，保证可见
            LocalDateTime expireAt = claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
            tokenBlacklistService.revoke(jti, userId, username, JwtUtil.TYPE_REFRESH, expireAt, "refresh");
            return generatePair(user);
        }
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        revokeIfPresent(accessToken, JwtUtil.TYPE_ACCESS, "logout");
        revokeIfPresent(refreshToken, JwtUtil.TYPE_REFRESH, "logout");
    }

    private void revokeIfPresent(String token, String expectedType, String reason) {
        if (token == null || token.isBlank()) {
            return;
        }
        // 登出即便 token 已过期也应吊销其 jti，防止边界复用；忽略过期异常
        Claims claims;
        try {
            claims = jwtUtil.parseClaimsIgnoreExpiry(token);
        } catch (JwtException ex) {
            // 无效 token 无法读取 jti，跳过
            return;
        }
        String jti = claims.getId();
        Long userId = claims.get("userId", Number.class) == null
                ? null : claims.get("userId", Number.class).longValue();
        String username = claims.getSubject();
        LocalDateTime expireAt = claims.getExpiration() == null ? null
                : claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        tokenBlacklistService.revoke(jti, userId, username, expectedType, expireAt, reason);
    }

    private LoginResponse buildLoginResponse(User user) {
        JwtUtil.TokenInfo access = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        JwtUtil.TokenInfo refresh = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        boolean mustChange = user.getMustChangePassword() != null && user.getMustChangePassword() == 1;
        return LoginResponse.builder()
                .accessToken(access.token())
                .refreshToken(refresh.token())
                .expiresIn(access.expirationMillis() / 1000)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .mustChangePassword(mustChange)
                .build();
    }

    private TokenPair generatePair(User user) {
        JwtUtil.TokenInfo access = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        JwtUtil.TokenInfo refresh = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        return TokenPair.builder()
                .accessToken(access.token())
                .refreshToken(refresh.token())
                .expiresIn(access.expirationMillis() / 1000)
                .refreshExpiresIn(refresh.expirationMillis() / 1000)
                .tokenType("Bearer")
                .build();
    }

    @Override
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return getOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(User user) {
        if (findByUsername(user.getUsername()) != null) {
            throw BusinessException.userAlreadyExists();
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        LocalDateTime now = TimeUtil.nowUtc();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        // 注册创建的用户默认不需要强制改密
        if (user.getMustChangePassword() == null) {
            user.setMustChangePassword(0);
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        return save(user);
    }

    @Override
    public List<Permission> getPermissions(Long userId) {
        return permissionService.getPermissionsByUserId(userId);
    }
}

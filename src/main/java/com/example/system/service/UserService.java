package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.dto.ChangePasswordRequest;
import com.example.system.dto.LoginRequest;
import com.example.system.dto.LoginResponse;
import com.example.system.dto.TokenPair;
import com.example.system.entity.Permission;
import com.example.system.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户登录，返回双令牌。
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 使用 refresh token 轮换获取新的令牌对（旧 refresh token 立即吊销）。
     */
    TokenPair refresh(String refreshToken);

    /**
     * 登出：把 access / refresh token 的 jti 写入黑名单。
     */
    void logout(String accessToken, String refreshToken);

    /**
     * 修改当前用户密码（用于首次登录强制改密 / 自助改密）。
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 根据用户名查找用户。
     */
    User findByUsername(String username);

    /**
     * 用户注册。
     */
    boolean register(User user);

    /**
     * 获取用户权限代码列表（供前端拉取与 Security 实时校验）。
     */
    List<Permission> getPermissions(Long userId);
}

package com.example.system.controller;

import com.example.system.common.Result;
import com.example.system.dto.ChangePasswordRequest;
import com.example.system.dto.LoginRequest;
import com.example.system.dto.LoginResponse;
import com.example.system.dto.LogoutRequest;
import com.example.system.dto.RefreshTokenRequest;
import com.example.system.dto.TokenPair;
import com.example.system.entity.Permission;
import com.example.system.entity.User;
import com.example.system.exception.BusinessException;
import com.example.system.security.CurrentUser;
import com.example.system.security.UserPrincipal;
import com.example.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证管理：登录、注册、刷新、登出、当前用户信息。
 *
 * @author system
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、刷新、登出和认证相关接口")
public class AuthController {

    private final UserService userService;
    private final com.example.system.security.LoginRateLimiter loginRateLimiter;

    @Operation(summary = "用户登录", description = "校验用户名密码，返回 access + refresh 双令牌；含失败次数锁定与 IP 限流")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "认证失败",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "登录请求",
                    required = true, content = @Content(schema = @Schema(implementation = LoginRequest.class)))
            @RequestBody @Valid LoginRequest loginRequest,
            HttpServletRequest httpRequest) {
        // 登录前限流校验（账户锁定 + IP 窗口）
        loginRateLimiter.checkBeforeLogin(loginRequest.getUsername(), httpRequest);
        try {
            LoginResponse resp = userService.login(loginRequest);
            loginRateLimiter.recordSuccess(loginRequest.getUsername());
            return Result.success(resp);
        } catch (RuntimeException ex) {
            // 仅对认证类失败累计（密码/用户名错误），其它异常不计数
            loginRateLimiter.recordFailure(loginRequest.getUsername());
            throw ex;
        }
    }

    @Operation(summary = "用户注册", description = "创建新用户账户")
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        boolean success = userService.register(user);
        if (!success) {
            throw BusinessException.dataSaveFailed();
        }
        return Result.success("注册成功");
    }

    @Operation(summary = "刷新令牌", description = "使用 refresh token 轮换获取新的令牌对，旧 refresh token 立即失效")
    @PostMapping("/refresh")
    public Result<TokenPair> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return Result.success(userService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "登出", description = "吊销当前 access 与 refresh token", security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest httpRequest, @RequestBody(required = false) LogoutRequest request) {
        String header = httpRequest.getHeader("Authorization");
        String accessToken = null;
        if (header != null && header.startsWith("Bearer ")) {
            accessToken = header.substring(7);
        }
        String refreshToken = request == null ? null : request.getRefreshToken();
        // 优先使用 body 中的 accessToken，其次使用请求头中的
        if (request != null && request.getAccessToken() != null) {
            accessToken = request.getAccessToken();
        }
        userService.logout(accessToken, refreshToken);
        return Result.success("已登出");
    }

    @Operation(summary = "修改密码", description = "当前用户自助修改密码；首次登录（mustChangePassword=true）也用此接口",
            security = @SecurityRequirement(name = "JWT"))
    @PostMapping("/change-password")
    public Result<String> changePassword(@CurrentUser UserPrincipal principal,
                                         @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(principal.getUserId(), request);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "获取当前登录用户的权限列表", description = "前端登录后可拉取一次用于按钮控制",
            security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/permissions")
    public Result<List<Permission>> permissions(@CurrentUser UserPrincipal principal) {
        return Result.success(userService.getPermissions(principal.getUserId()));
    }

    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户详细信息",
            security = @SecurityRequirement(name = "JWT"))
    @GetMapping("/info")
    @PreAuthorize("hasAuthority('user:view') or #userId == authentication.principal.userId")
    public Result<User> getUserInfo(@RequestParam Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        user.setPassword(null);
        return Result.success(user);
    }
}

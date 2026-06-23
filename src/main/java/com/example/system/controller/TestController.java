package com.example.system.controller;

import com.example.system.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RBAC 权限测试接口（演示 @PreAuthorize 用法）。
 *
 * @author system
 */
@RestController
@RequestMapping("/test")
@Tag(name = "测试接口", description = "用于测试RBAC权限的接口")
public class TestController {

    @Operation(summary = "公开接口", description = "任何人都可以访问")
    @GetMapping("/public")
    public Result<String> publicEndpoint() {
        return Result.success("这是一个公开接口，任何人都可以访问");
    }

    @Operation(summary = "需要用户角色", description = "需要 user 角色才能访问")
    @GetMapping("/user")
    @PreAuthorize("hasRole('user')")
    public Result<String> userEndpoint() {
        return Result.success("这是用户角色接口，只有用户角色才能访问");
    }

    @Operation(summary = "需要管理员角色", description = "需要 admin 角色才能访问")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('admin')")
    public Result<String> adminEndpoint() {
        return Result.success("这是管理员角色接口，只有管理员角色才能访问");
    }

    @Operation(summary = "需要用户查看权限", description = "需要 user:view 权限才能访问")
    @GetMapping("/user-view")
    @PreAuthorize("hasAuthority('user:view')")
    public Result<String> userViewEndpoint() {
        return Result.success("这是需要用户查看权限的接口");
    }

    @Operation(summary = "需要用户管理权限", description = "需要 user:manage 权限才能访问")
    @GetMapping("/user-manage")
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<String> userManageEndpoint() {
        return Result.success("这是需要用户管理权限的接口");
    }

    @Operation(summary = "需要多个权限中的任一个", description = "满足任一权限即可访问")
    @GetMapping("/any-permission")
    @PreAuthorize("hasAuthority('user:view') or hasAuthority('role:view')")
    public Result<String> anyPermissionEndpoint() {
        return Result.success("这是需要多个权限中任一个的接口");
    }

    @Operation(summary = "需要所有权限", description = "需要所有指定权限才能访问")
    @GetMapping("/all-permissions")
    @PreAuthorize("hasAuthority('user:view') and hasAuthority('role:view')")
    public Result<String> allPermissionsEndpoint() {
        return Result.success("这是需要所有权限的接口");
    }
}

package com.example.system.controller;

import com.example.system.common.Result;
import com.example.system.entity.Permission;
import com.example.system.entity.Role;
import com.example.system.exception.BusinessException;
import com.example.system.service.PermissionService;
import com.example.system.service.RoleService;
import com.example.system.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-role")
@RequiredArgsConstructor
@Tag(name = "用户角色管理", description = "用户角色关系相关接口")
@PreAuthorize("hasRole('admin')")
public class UserRoleController {

    private final UserRoleService userRoleService;

    private final RoleService roleService;

    private final PermissionService permissionService;

    @Operation(summary = "获取用户的角色")
    @GetMapping("/user/{userId}/roles")
    public Result<List<Role>> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long userId) {
        List<Role> roles = roleService.getRolesByUserId(userId);
        return Result.success(roles);
    }

    @Operation(summary = "获取用户的权限")
    @GetMapping("/user/{userId}/permissions")
    public Result<List<Permission>> getUserPermissions(@Parameter(description = "用户ID") @PathVariable Long userId) {
        List<Permission> permissions = permissionService.getPermissionsByUserId(userId);
        return Result.success(permissions);
    }

    @Operation(summary = "为用户分配角色")
    @PostMapping("/assign")
    public Result<String> assignRole(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "角色ID") @RequestParam Long roleId) {
        boolean success = userRoleService.assignUserRole(userId, roleId);
        if (success) {
            return Result.success("角色分配成功");
        } else {
            throw BusinessException.dataSaveFailed();
        }
    }

    @Operation(summary = "取消用户角色")
    @DeleteMapping("/remove")
    public Result<String> removeRole(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "角色ID") @RequestParam Long roleId) {
        boolean success = userRoleService.removeUserRole(userId, roleId);
        if (success) {
            return Result.success("角色取消成功");
        } else {
            throw BusinessException.dataDeleteFailed();
        }
    }
}
package com.example.system.controller;

import com.example.system.common.Result;
import com.example.system.entity.User;
import com.example.system.exception.BusinessException;
import com.example.system.security.CurrentUser;
import com.example.system.security.UserPrincipal;
import com.example.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理。
 *
 * @author system
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页获取用户列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('user:view')")
    public Result<com.example.system.dto.PageResult<User>> pageUsers(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页大小，1~100") @RequestParam(defaultValue = "10") long size) {
        // 限制单页最大 100，防止超大查询
        long safeSize = Math.max(1, Math.min(size, 100));
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(Math.max(1, current), safeSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = userService.page(page);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(com.example.system.dto.PageResult.from(result));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAuthority('user:view')")
    public Result<User> getUserDetail(@Parameter(description = "用户ID") @PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<User> me(@CurrentUser UserPrincipal principal) {
        User user = userService.getById(principal.getUserId());
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "创建用户")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('user:create') and hasRole('admin')")
    public Result<String> createUser(@RequestBody User user) {
        boolean success = userService.register(user);
        if (!success) {
            throw BusinessException.dataSaveFailed();
        }
        return Result.success("用户创建成功");
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<String> updateUser(@Parameter(description = "用户ID") @PathVariable Long id, @RequestBody User user) {
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            throw BusinessException.userNotFound();
        }
        user.setId(id);
        boolean success = userService.updateById(user);
        if (!success) {
            throw BusinessException.dataUpdateFailed();
        }
        return Result.success("用户更新成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('user:delete') and hasRole('admin')")
    public Result<String> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            throw BusinessException.userNotFound();
        }
        boolean success = userService.removeById(id);
        if (!success) {
            throw BusinessException.dataDeleteFailed();
        }
        return Result.success("用户删除成功");
    }
}

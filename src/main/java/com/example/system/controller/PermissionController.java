package com.example.system.controller;

import com.example.system.common.Result;
import com.example.system.entity.Permission;
import com.example.system.exception.BusinessException;
import com.example.system.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "权限相关接口")
@PreAuthorize("hasRole('admin')")
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "获取所有权限")
    @GetMapping("/list")
    public Result<List<Permission>> getAllPermissions() {
        return Result.success(permissionService.list());
    }

    @Operation(summary = "创建权限")
    @PostMapping("/create")
    public Result<String> createPermission(@RequestBody Permission permission) {
        boolean success = permissionService.save(permission);
        if (success) {
            return Result.success("权限创建成功");
        } else {
            throw BusinessException.dataSaveFailed();
        }
    }

    @Operation(summary = "更新权限")
    @PutMapping("/update/{id}")
    public Result<String> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        Permission existingPermission = permissionService.getById(id);
        if (existingPermission == null) {
            throw BusinessException.permissionNotFound();
        }
        
        permission.setId(id);
        boolean success = permissionService.updateById(permission);
        if (success) {
            return Result.success("权限更新成功");
        } else {
            throw BusinessException.dataUpdateFailed();
        }
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/delete/{id}")
    public Result<String> deletePermission(@PathVariable Long id) {
        Permission existingPermission = permissionService.getById(id);
        if (existingPermission == null) {
            throw BusinessException.permissionNotFound();
        }
        
        boolean success = permissionService.removeById(id);
        if (success) {
            return Result.success("权限删除成功");
        } else {
            throw BusinessException.dataDeleteFailed();
        }
    }
}
package com.example.system.controller;

import com.example.system.common.Result;
import com.example.system.entity.Role;
import com.example.system.exception.BusinessException;
import com.example.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色相关接口")
@PreAuthorize("hasRole('admin')")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "获取所有角色")
    @GetMapping("/list")
    public Result<List<Role>> getAllRoles() {
        return Result.success(roleService.list());
    }

    @Operation(summary = "创建角色")
    @PostMapping("/create")
    public Result<String> createRole(@RequestBody Role role) {
        boolean success = roleService.save(role);
        if (success) {
            return Result.success("角色创建成功");
        } else {
            throw BusinessException.dataSaveFailed();
        }
    }

    @Operation(summary = "更新角色")
    @PutMapping("/update/{id}")
    public Result<String> updateRole(@PathVariable Long id, @RequestBody Role role) {
        Role existingRole = roleService.getById(id);
        if (existingRole == null) {
            throw BusinessException.roleNotFound();
        }
        
        role.setId(id);
        boolean success = roleService.updateById(role);
        if (success) {
            return Result.success("角色更新成功");
        } else {
            throw BusinessException.dataUpdateFailed();
        }
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteRole(@PathVariable Long id) {
        Role existingRole = roleService.getById(id);
        if (existingRole == null) {
            throw BusinessException.roleNotFound();
        }
        
        boolean success = roleService.removeById(id);
        if (success) {
            return Result.success("角色删除成功");
        } else {
            throw BusinessException.dataDeleteFailed();
        }
    }
}
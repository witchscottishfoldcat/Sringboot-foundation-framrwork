package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.entity.Permission;
import com.example.system.entity.Role;
import com.example.system.entity.RolePermission;
import com.example.system.mapper.PermissionMapper;
import com.example.system.mapper.RolePermissionMapper;
import com.example.system.service.PermissionService;
import com.example.system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    
    @Autowired
    private RoleService roleService;

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        // 获取用户的所有角色
        List<Long> roleIds = roleService.getRolesByUserId(userId).stream()
                .map(Role::getId)
                .collect(Collectors.toList());
        
        if (roleIds.isEmpty()) {
            return List.of();
        }
        
        // 获取角色关联的所有权限ID
        QueryWrapper<RolePermission> rpQueryWrapper = new QueryWrapper<>();
        rpQueryWrapper.in("role_id", roleIds);
        List<Long> permissionIds = rolePermissionMapper.selectList(rpQueryWrapper).stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        
        // 获取权限详情
        QueryWrapper<Permission> pQueryWrapper = new QueryWrapper<>();
        pQueryWrapper.in("id", permissionIds);
        return list(pQueryWrapper);
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        // 获取角色关联的所有权限ID
        QueryWrapper<RolePermission> rpQueryWrapper = new QueryWrapper<>();
        rpQueryWrapper.eq("role_id", roleId);
        List<Long> permissionIds = rolePermissionMapper.selectList(rpQueryWrapper).stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        
        // 获取权限详情
        QueryWrapper<Permission> pQueryWrapper = new QueryWrapper<>();
        pQueryWrapper.in("id", permissionIds);
        return list(pQueryWrapper);
    }

    @Override
    public Permission getByPermissionCode(String permissionCode) {
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("permission_code", permissionCode);
        return getOne(queryWrapper);
    }
}
package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.entity.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {
    
    /**
     * 根据用户ID获取权限列表
     */
    List<Permission> getPermissionsByUserId(Long userId);
    
    /**
     * 根据角色ID获取权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);
    
    /**
     * 根据权限代码获取权限
     */
    Permission getByPermissionCode(String permissionCode);
}
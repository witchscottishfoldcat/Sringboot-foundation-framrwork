package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {
    
    /**
     * 根据用户ID获取角色列表
     */
    List<Role> getRolesByUserId(Long userId);
    
    /**
     * 根据角色代码获取角色
     */
    Role getByRoleCode(String roleCode);
}
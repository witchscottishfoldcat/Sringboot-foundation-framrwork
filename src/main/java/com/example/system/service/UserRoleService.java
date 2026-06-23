package com.example.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.system.entity.UserRole;

import java.util.List;

public interface UserRoleService extends IService<UserRole> {
    
    /**
     * 根据用户ID获取角色ID列表
     */
    List<Long> getRoleIdsByUserId(Long userId);
    
    /**
     * 分配用户角色
     */
    boolean assignUserRole(Long userId, Long roleId);
    
    /**
     * 取消用户角色
     */
    boolean removeUserRole(Long userId, Long roleId);
}
package com.example.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.system.entity.UserRole;
import com.example.system.mapper.UserRoleMapper;
import com.example.system.service.UserRoleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return baseMapper.getRoleIdsByUserId(userId);
    }

    @Override
    public boolean assignUserRole(Long userId, Long roleId) {
        // 检查是否已经存在关联
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("role_id", roleId);
        UserRole existRelation = getOne(queryWrapper);
        
        if (existRelation != null) {
            return true; // 已存在关联
        }
        
        // 创建新的关联
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return save(userRole);
    }

    @Override
    public boolean removeUserRole(Long userId, Long roleId) {
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("role_id", roleId);
        return remove(queryWrapper);
    }
}
package com.example.system.service;

/**
 * 管理员初始化服务接口
 */
public interface AdminInitService {
    
    /**
     * 初始化默认管理员账号
     * 如果admin用户不存在，则创建并分配超级管理员角色
     */
    void initializeAdmin();
}
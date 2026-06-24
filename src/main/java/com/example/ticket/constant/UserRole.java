package com.example.ticket.constant;

/**
 * 用户角色常量类
 * 用于统一管理系统中的用户角色，避免在代码中使用魔法值
 * 
 * @author chenyuhan
 * @since 2025-11-26
 */
public class UserRole {
    
    /**
     * 普通用户角色
     */
    public static final String USER = "USER";
    
    /**
     * 管理员角色
     */
    public static final String ADMIN = "ADMIN";
    
    /**
     * 私有构造函数，防止实例化
     */
    private UserRole() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }
}

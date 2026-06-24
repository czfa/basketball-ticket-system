package com.example.ticket.constant;

/**
 * 用户状态常量类
 * 用于统一管理用户账号的各种状态，避免在代码中使用魔法值
 * 
 * @author chenyuhan
 * @since 2025-11-26
 */
public class UserStatus {
    
    /**
     * 激活：用户账号正常可用
     */
    public static final String ACTIVE = "ACTIVE";
    
    /**
     * 封禁：用户账号已被封禁
     */
    public static final String BANNED = "BANNED";
    
    /**
     * 私有构造函数，防止实例化
     */
    private UserStatus() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }
}

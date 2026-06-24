package com.example.ticket.constant;

/**
 * 电子票状态常量类
 * 用于统一管理电子票的各种状态，避免在代码中使用魔法值
 * 
 * @author chenyuhan
 * @since 2025-11-26
 */
public class TicketStatus {
    
    /**
     * 有效：票已出票，可以使用
     */
    public static final String VALID = "VALID";
    
    /**
     * 已使用：票已检票使用
     */
    public static final String USED = "USED";
    
    /**
     * 已退款：票已退款
     */
    public static final String REFUNDED = "REFUNDED";
    
    /**
     * 已过期：票已过期
     */
    public static final String EXPIRED = "EXPIRED";
    
    /**
     * 私有构造函数，防止实例化
     */
    private TicketStatus() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }
}

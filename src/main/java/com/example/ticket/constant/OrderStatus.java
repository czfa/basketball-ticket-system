package com.example.ticket.constant;

/**
 * 订单状态常量类
 * 用于统一管理订单的各种状态，避免在代码中使用魔法值
 * 
 * @author chenyuhan
 * @since 2025-11-26
 */
public class OrderStatus {
    
    /**
     * 待支付：订单已创建，等待用户支付
     */
    public static final String PENDING = "PENDING";
    
    /**
     * 已支付：订单已成功支付，等待出票
     */
    public static final String PAID = "PAID";
    
    /**
     * 已取消：订单已取消（通常是未支付状态下用户主动取消）
     */
    public static final String CANCELLED = "CANCELLED";
    
    /**
     * 已退款：订单已退款
     */
    public static final String REFUNDED = "REFUNDED";
    
    /**
     * 已过期：订单已过期（未在规定时间内支付）
     */
    public static final String EXPIRED = "EXPIRED";
    
    /**
     * 私有构造函数，防止实例化
     */
    private OrderStatus() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }
}

package com.example.ticket.common;

/**
 * 响应状态码枚举类
 * 用于统一管理系统中的各种响应状态码，便于维护和扩展
 * 
 * @author chenyuhan
 * @since 2025-11-26
 */
public enum ResultCode {
    
    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),
    
    /**
     * 失败（通用错误）
     */
    ERROR(500, "操作失败"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),
    
    /**
     * 未授权（未登录）
     */
    UNAUTHORIZED(401, "未授权，请先登录"),
    
    /**
     * 权限不足
     */
    FORBIDDEN(403, "权限不足"),
    
    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),
    
    /**
     * 用户名已存在
     */
    USERNAME_EXISTS(1001, "用户名已存在"),
    
    /**
     * 邮箱已存在
     */
    EMAIL_EXISTS(1002, "邮箱已存在"),
    
    /**
     * 用户名或密码错误
     */
    LOGIN_ERROR(1003, "用户名或密码错误"),
    
    /**
     * 用户不存在
     */
    USER_NOT_FOUND(1004, "用户不存在"),
    
    /**
     * 订单不存在
     */
    ORDER_NOT_FOUND(2001, "订单不存在"),
    
    /**
     * 订单状态错误
     */
    ORDER_STATUS_ERROR(2002, "订单状态错误"),
    
    /**
     * 座位已被预订
     */
    SEAT_ALREADY_BOOKED(3001, "座位已被预订"),
    
    /**
     * 座位不存在
     */
    SEAT_NOT_FOUND(3002, "座位不存在"),
    
    /**
     * 赛事不存在
     */
    MATCH_NOT_FOUND(4001, "赛事不存在"),
    
    /**
     * 座位数量不足
     */
    SEAT_INSUFFICIENT(3003, "座位数量不足");
    
    /**
     * 状态码
     */
    private final Integer code;
    
    /**
     * 状态消息
     */
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}

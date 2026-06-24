package com.example.ticket.constant;

/**
 * 检票状态常量类
 * 用于统一管理电子票的检票状态，避免在代码中使用魔法值
 * 
 * @author chenyuhan
 * @since 2025-11-26
 */
public class CheckStatus {
    
    /**
     * 未检票：票尚未被检票
     */
    public static final String UNCHECKED = "UNCHECKED";
    
    /**
     * 已检票：票已被检票
     */
    public static final String CHECKED = "CHECKED";
    
    /**
     * 私有构造函数，防止实例化
     */
    private CheckStatus() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }
}

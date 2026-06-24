package com.example.ticket.constant;

/**
 * 赛事状态常量类
 * 用于统一管理赛事的各种状态，避免在代码中使用魔法值
 * 
 * @author chenyuhan
 * @since 2025-11-26
 */
public class MatchStatus {
    
    /**
     * 即将开始：赛事尚未开始，可进行购票
     */
    public static final String UPCOMING = "UPCOMING";
    
    /**
     * 进行中：赛事正在进行中
     */
    public static final String ONGOING = "ONGOING";
    
    /**
     * 已结束：赛事已结束
     */
    public static final String FINISHED = "FINISHED";
    
    /**
     * 已取消：赛事已取消
     */
    public static final String CANCELLED = "CANCELLED";
    
    /**
     * 私有构造函数，防止实例化
     */
    private MatchStatus() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }
}

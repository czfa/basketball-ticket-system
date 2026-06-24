package com.example.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 管理员日志表
 * </p>
 *
 * @author chenyuhan
 * @since 2025-11-26
 */
@TableName("admin_log")
public class AdminLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /**
     * 管理员ID
     */
    private Long adminId;

    /**
     * 管理员姓名
     */
    private String adminName;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作目标ID
     */
    private Long targetId;

    /**
     * 操作详情
     */
    private String details;

    /**
     * 操作IP
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;


    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    @Override
    public String toString() {
        return "AdminLog{" +
        "logId=" + logId +
        ", adminId=" + adminId +
        ", adminName=" + adminName +
        ", action=" + action +
        ", module=" + module +
        ", targetId=" + targetId +
        ", details=" + details +
        ", ipAddress=" + ipAddress +
        ", userAgent=" + userAgent +
        ", operationTime=" + operationTime +
        "}";
    }
}

package com.example.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 电子票表
 * </p>
 *
 * @author chenyuhan
 * @since 2025-11-26
 */
@TableName("tickets")
public class Tickets implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 电子票ID
     */
    @TableId(value = "ticket_id", type = IdType.AUTO)
    private Long ticketId;

    /**
     * 票号
     */
    private String ticketNumber;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 座位ID
     */
    private Long seatId;

    /**
     * 二维码数据
     */
    private String qrCode;

    /**
     * 检票状态
     */
    private String checkStatus;

    /**
     * 检票时间
     */
    private LocalDateTime checkTime;

    /**
     * 检票操作员
     */
    private String checkOperator;

    /**
     * 出票时间
     */
    private LocalDateTime issueTime;

    /**
     * 票状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(String checkStatus) {
        this.checkStatus = checkStatus;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    public String getCheckOperator() {
        return checkOperator;
    }

    public void setCheckOperator(String checkOperator) {
        this.checkOperator = checkOperator;
    }

    public LocalDateTime getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(LocalDateTime issueTime) {
        this.issueTime = issueTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "Tickets{" +
        "ticketId=" + ticketId +
        ", ticketNumber=" + ticketNumber +
        ", orderId=" + orderId +
        ", seatId=" + seatId +
        ", qrCode=" + qrCode +
        ", checkStatus=" + checkStatus +
        ", checkTime=" + checkTime +
        ", checkOperator=" + checkOperator +
        ", issueTime=" + issueTime +
        ", status=" + status +
        ", createTime=" + createTime +
        ", updateTime=" + updateTime +
        "}";
    }
}

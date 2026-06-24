package com.example.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 座位表
 * </p>
 *
 * @author chenyuhan
 * @since 2025-11-26
 */
@TableName("seat")
public class Seat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 座位ID
     */
    @TableId(value = "seat_id", type = IdType.AUTO)
    private Long seatId;

    /**
     * 赛事ID
     */
    private Long matchId;

    /**
     * 座位号（如：A区1排10座）
     */
    private String seatNumber;

    /**
     * 区域（如：A区）
     */
    private String seatZone;

    /**
     * 排号
     */
    private Integer seatRow;

    /**
     * 列号
     */
    private Integer seatCol;

    /**
     * 座位类型
     */
    private String seatType;

    /**
     * 实际价格
     */
    private BigDecimal price;

    /**
     * 是否已预订
     */
    private Boolean isBooked;

    /**
     * 是否可用
     */
    private Boolean isAvailable;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    private Long orderId;


    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatZone() {
        return seatZone;
    }

    public void setSeatZone(String seatZone) {
        this.seatZone = seatZone;
    }

    public Integer getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(Integer seatRow) {
        this.seatRow = seatRow;
    }

    public Integer getSeatCol() {
        return seatCol;
    }

    public void setSeatCol(Integer seatCol) {
        this.seatCol = seatCol;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getIsBooked() {
        return isBooked;
    }

    public void setIsBooked(Boolean isBooked) {
        this.isBooked = isBooked;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return "Seat{" +
        "seatId=" + seatId +
        ", matchId=" + matchId +
        ", seatNumber=" + seatNumber +
        ", seatZone=" + seatZone +
        ", seatRow=" + seatRow +
        ", seatCol=" + seatCol +
        ", seatType=" + seatType +
        ", price=" + price +
        ", isBooked=" + isBooked +
        ", isAvailable=" + isAvailable +
        ", createTime=" + createTime +
        ", updateTime=" + updateTime +
                ",orderId =" + orderId +
        "}";
    }
}

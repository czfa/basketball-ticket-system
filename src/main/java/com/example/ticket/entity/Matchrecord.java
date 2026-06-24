package com.example.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 赛事表
 * </p>
 *
 * @author chenyuhan
 * @since 2025-11-26
 */
@TableName("matchrecord")
public class Matchrecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 赛事ID
     */
    @TableId(value = "match_id", type = IdType.AUTO)
    private Long matchId;

    /**
     * 赛事名称
     */
    private String matchName;

    /**
     * 主队
     */
    private String homeTeam;

    /**
     * 客队
     */
    private String awayTeam;

    /**
     * 比赛场馆
     */
    private String venue;

    /**
     * 比赛时间
     */
    private LocalDateTime matchTime;

    /**
     * 赛事描述
     */
    private String description;

    /**
     * 总座位数
     */
    private Integer totalSeats;

    /**
     * 可用座位数
     */
    private Integer availableSeats;

    /**
     * 基础票价
     */
    private BigDecimal basePrice;

    /**
     * 赛事状态
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


    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getMatchName() {
        return matchName;
    }

    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public LocalDateTime getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(LocalDateTime matchTime) {
        this.matchTime = matchTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
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
        return "Matchrecord{" +
        "matchId=" + matchId +
        ", matchName=" + matchName +
        ", homeTeam=" + homeTeam +
        ", awayTeam=" + awayTeam +
        ", venue=" + venue +
        ", matchTime=" + matchTime +
        ", description=" + description +
        ", totalSeats=" + totalSeats +
        ", availableSeats=" + availableSeats +
        ", basePrice=" + basePrice +
        ", status=" + status +
        ", createTime=" + createTime +
        ", updateTime=" + updateTime +
        "}";
    }
}

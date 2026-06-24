package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.ticket.constant.MatchStatus;
import com.example.ticket.entity.Matchrecord;
import com.example.ticket.mapper.MatchrecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MatchService {
    @Autowired
    private MatchrecordMapper matchrecordMapper;

    // 查询所有比赛信息（按赛事ID从小到大排序）
    public List<Matchrecord> getAllMatches() {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("match_id");
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 根据比赛ID查询比赛信息
    public Matchrecord getMatchById(Long matchId) {
        return matchrecordMapper.selectById(matchId);
    }

    // 根据比赛名称查询比赛信息
    public List<Matchrecord> getMatchesByName(String matchName) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("match_name", matchName)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 根据球队名称查询比赛信息
    public List<Matchrecord> getMatchesByTeam(String teamName) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("home_team", teamName)
                .or()
                .like("away_team", teamName)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 根据比赛状态查询比赛信息
    public List<Matchrecord> getMatchesByStatus(String status) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    /**
     * 查询即将开始的比赛
     * 
     * @return 即将开始的比赛列表
     */
    public List<Matchrecord> getUpcomingMatches() {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", MatchStatus.UPCOMING)
                .orderByAsc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    /**
     * 查询进行中的比赛
     * 
     * @return 进行中的比赛列表
     */
    public List<Matchrecord> getOngoingMatches() {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", MatchStatus.ONGOING)
                .orderByAsc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    /**
     * 查询已结束的比赛
     * 
     * @return 已结束的比赛列表
     */
    public List<Matchrecord> getFinishedMatches() {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", MatchStatus.FINISHED)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    /**
     * 查询已取消的比赛
     * 
     * @return 已取消的比赛列表
     */
    public List<Matchrecord> getCancelledMatches() {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", MatchStatus.CANCELLED)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 根据场馆查询比赛信息
    public List<Matchrecord> getMatchesByVenue(String venue) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("venue", venue)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 查询某个时间范围内的比赛
    public List<Matchrecord> getMatchesByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("match_time", startTime, endTime)
                .orderByAsc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 根据主队和客队查询比赛
    public List<Matchrecord> getMatchesByTeams(String homeTeam, String awayTeam) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("home_team", homeTeam)
                .eq("away_team", awayTeam)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 统计比赛数量
    public Long countMatches() {
        return matchrecordMapper.selectCount(null);
    }

    public List<Matchrecord> getMatchesByDateRange(String startDate, String endDate) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("match_time", startDate, endDate)
                .orderByAsc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    public List<Matchrecord> getMatchesByMatchTime(String matchTime) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_time", matchTime)
                .orderByAsc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    /**
     * 获取热门比赛（按可用座位数升序排序）
     * 
     * @param limit 返回数量限制
     * @return 热门比赛列表
     */
    public List<Matchrecord> getPopularMatchesSorted(Integer limit) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", MatchStatus.UPCOMING)
                .orderByAsc("available_seats")
                .last("LIMIT " + limit);
        return matchrecordMapper.selectList(queryWrapper);
    }

    public List<Matchrecord> getLatestMatches(Integer limit) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time")
                .last("LIMIT " + limit);
        return matchrecordMapper.selectList(queryWrapper);
    }

    public List<Matchrecord> getMatchesSortedByPrice(String sortOrder, Integer limit) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        if ("ASC".equalsIgnoreCase(sortOrder)) {
            queryWrapper.orderByAsc("base_price");
        } else {
            queryWrapper.orderByDesc("base_price");
        }
        queryWrapper.last("LIMIT " + limit);
        return matchrecordMapper.selectList(queryWrapper);
    }

    public List<Matchrecord> getMatchesSortedByTime(String sortOrder, Integer limit) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        if ("ASC".equalsIgnoreCase(sortOrder)) {
            queryWrapper.orderByAsc("match_time");
        } else {
            queryWrapper.orderByDesc("match_time");
        }
        queryWrapper.last("LIMIT " + limit);
        return matchrecordMapper.selectList(queryWrapper);
    }

    public List<Matchrecord> searchMatches(String keyword) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("match_name", keyword)
                .or().like("home_team", keyword)
                .or().like("away_team", keyword)
                .or().like("venue", keyword)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    /**
     * 获取推荐比赛（即将开始且有可用座位）
     * 
     * @param limit 返回数量限制
     * @return 推荐比赛列表
     */
    public List<Matchrecord> getRecommendedMatches(Integer limit) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", MatchStatus.UPCOMING)
                .gt("available_seats", 0)
                .orderByAsc("available_seats")
                .last("LIMIT " + limit);
        return matchrecordMapper.selectList(queryWrapper);
    }
    /**
     * 新增比赛（管理员功能）
     * @param matchrecord 比赛信息
     * @return 添加成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addMatch(Matchrecord matchrecord) {
        matchrecord.setCreateTime(LocalDateTime.now());
        matchrecord.setUpdateTime(LocalDateTime.now());
        matchrecord.setStatus(MatchStatus.UPCOMING); // 默认状态为即将开始
        if (matchrecord.getAvailableSeats() == null) {
            matchrecord.setAvailableSeats(matchrecord.getTotalSeats() != null ? matchrecord.getTotalSeats() : 0);
        }
        return matchrecordMapper.insert(matchrecord) > 0;
    }

    /**
     * 更新比赛信息（管理员功能）
     * @param matchrecord 比赛信息
     * @return 更新成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMatch(Matchrecord matchrecord) {
        matchrecord.setUpdateTime(LocalDateTime.now());
        return matchrecordMapper.updateById(matchrecord) > 0;
    }

    /**
     * 删除比赛（管理员功能，逻辑删除）
     * @param matchId 比赛ID
     * @return 删除成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMatch(Long matchId) {
        // 实际业务中应该检查是否有已售出的订单，如果有则不允许删除
        // 这里简化为直接删除（实际应使用逻辑删除）
        return matchrecordMapper.deleteById(matchId) > 0;
    }

    /**
     * 更新比赛状态（管理员功能）
     * @param matchId 比赛ID
     * @param status 新状态（UPCOMING/ONGOING/FINISHED/CANCELLED）
     * @return 更新成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMatchStatus(Long matchId, String status) {
        UpdateWrapper<Matchrecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("match_id", matchId)
                .set("status", status)
                .set("update_time", LocalDateTime.now());
        return matchrecordMapper.update(null, updateWrapper) > 0;
    }

    /**
     * 更新可用座位数（管理员功能，手动调整）
     * @param matchId 比赛ID
     * @param availableSeats 可用座位数
     * @return 更新成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAvailableSeats(Long matchId, Integer availableSeats) {
        UpdateWrapper<Matchrecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("match_id", matchId)
                .set("available_seats", availableSeats)
                .set("update_time", LocalDateTime.now());
        return matchrecordMapper.update(null, updateWrapper) > 0;
    }

    /**
     * 减少可用座位数（售票时调用，由订单Service调用）
     * @param matchId 比赛ID
     * @param seatsToDecrease 减少的座位数
     * @return 更新成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseAvailableSeats(Long matchId, Integer seatsToDecrease) {
        Matchrecord match = matchrecordMapper.selectById(matchId);
        if (match == null || match.getAvailableSeats() < seatsToDecrease) {
            return false;
        }

        UpdateWrapper<Matchrecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("match_id", matchId)
                .set("available_seats", match.getAvailableSeats() - seatsToDecrease)
                .set("update_time", LocalDateTime.now());
        return matchrecordMapper.update(null, updateWrapper) > 0;
    }

    /**
     * 增加可用座位数（退票时调用，由订单Service调用）
     * @param matchId 比赛ID
     * @param seatsToIncrease 增加的座位数
     * @return 更新成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean increaseAvailableSeats(Long matchId, Integer seatsToIncrease) {
        Matchrecord match = matchrecordMapper.selectById(matchId);
        if (match == null) {
            return false;
        }

        int newAvailableSeats = match.getAvailableSeats() + seatsToIncrease;
        if (newAvailableSeats > match.getTotalSeats()) {
            newAvailableSeats = match.getTotalSeats();
        }

        UpdateWrapper<Matchrecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("match_id", matchId)
                .set("available_seats", newAvailableSeats)
                .set("update_time", LocalDateTime.now());
        return matchrecordMapper.update(null, updateWrapper) > 0;
    }

    /**
     * 更新基础票价（管理员功能）
     * @param matchId 比赛ID
     * @param basePrice 基础票价
     * @return 更新成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBasePrice(Long matchId, BigDecimal basePrice) {
        UpdateWrapper<Matchrecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("match_id", matchId)
                .set("base_price", basePrice)
                .set("update_time", LocalDateTime.now());
        return matchrecordMapper.update(null, updateWrapper) > 0;
    }

    // 检查比赛是否存在
    public boolean existsMatch(Long matchId) {
        return matchrecordMapper.selectById(matchId) != null;
    }

    /**
     * 检查比赛是否可预订（状态为即将开始且有可用座位）
     * 
     * @param matchId 比赛ID
     * @return 可预订返回true
     */
    public boolean isMatchBookable(Long matchId) {
        Matchrecord match = matchrecordMapper.selectById(matchId);
        return match != null &&
                MatchStatus.UPCOMING.equals(match.getStatus()) &&
                match.getAvailableSeats() > 0;
    }

    /**
     * 获取热门比赛（可用座位较少的比赛）
     * 
     * @param limit 返回数量限制
     * @return 热门比赛列表
     */
    public List<Matchrecord> getPopularMatches(Integer limit) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", MatchStatus.UPCOMING)
                .orderByAsc("available_seats")
                .last("LIMIT " + limit);
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 根据价格范围查询比赛
    public List<Matchrecord> getMatchesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("base_price", minPrice, maxPrice)
                .orderByAsc("base_price");
        return matchrecordMapper.selectList(queryWrapper);
    }

    // 统计不同状态的比赛数量
    public Long countMatchesByStatus(String status) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        return matchrecordMapper.selectCount(queryWrapper);
    }

    // 获取今天内的比赛
    public List<Matchrecord> getTodayMatches() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return getMatchesByTimeRange(startOfDay, endOfDay);
    }

    // 获取未来7天内的比赛
    public List<Matchrecord> getNextWeekMatches() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusDays(7);
        return getMatchesByTimeRange(now, nextWeek);
    }

    // 根据多个状态查询比赛
    public List<Matchrecord> getMatchesByStatusList(List<String> statusList) {
        QueryWrapper<Matchrecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("status", statusList)
                .orderByDesc("match_time");
        return matchrecordMapper.selectList(queryWrapper);
    }

    /**
     * 根据比赛时间和当前时间判断赛事状态
     * 
     * 判断逻辑：
     * - 如果比赛时间 > 当前时间 + 2小时：即将开始（UPCOMING）
     * - 如果比赛时间 <= 当前时间 <= 比赛时间 + 2小时：进行中（ONGOING）
     * - 如果当前时间 > 比赛时间 + 2小时：已结束（FINISHED）
     * 
     * @param matchTime 比赛时间
     * @return 赛事状态（UPCOMING/ONGOING/FINISHED）
     */
    public String determineMatchStatus(LocalDateTime matchTime) {
        if (matchTime == null) {
            return MatchStatus.UPCOMING;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long hoursBetween = ChronoUnit.HOURS.between(matchTime, now);
        
        // 当前时间早于比赛时间：即将开始
        if (hoursBetween < 0) {
            return MatchStatus.UPCOMING;
        } 
        // 当前时间在比赛开始后2小时内：进行中
        else if (hoursBetween <= 2) {
            return MatchStatus.ONGOING;
        } 
        // 当前时间超过比赛开始后2小时：已结束
        else {
            return MatchStatus.FINISHED;
        }
    }

    /**
     * 更新单个赛事的状态（根据时间自动判断）
     * 
     * @param matchId 赛事ID
     * @return 更新成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMatchStatusByTime(Long matchId) {
        Matchrecord match = matchrecordMapper.selectById(matchId);
        if (match == null) {
            return false;
        }
        
        // 已取消的赛事不自动更新状态
        if (MatchStatus.CANCELLED.equals(match.getStatus())) {
            return true;
        }
        
        String newStatus = determineMatchStatus(match.getMatchTime());
        
        // 只有状态发生变化时才更新
        if (!newStatus.equals(match.getStatus())) {
            match.setStatus(newStatus);
            match.setUpdateTime(LocalDateTime.now());
            return matchrecordMapper.updateById(match) > 0;
        }
        
        return true;
    }

    /**
     * 更新所有赛事的状态（根据时间自动判断）
     * 可以通过定时任务调用，或者在系统启动时调用
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAllMatchStatusByTime() {
        List<Matchrecord> allMatches = matchrecordMapper.selectList(null);
        
        for (Matchrecord match : allMatches) {
            // 已取消的赛事不自动更新状态
            if (MatchStatus.CANCELLED.equals(match.getStatus())) {
                continue;
            }
            
            String newStatus = determineMatchStatus(match.getMatchTime());
            
            // 只有状态发生变化时才更新
            if (!newStatus.equals(match.getStatus())) {
                match.setStatus(newStatus);
                match.setUpdateTime(LocalDateTime.now());
                matchrecordMapper.updateById(match);
            }
        }
    }

    /**
     * 定时任务：每分钟检查并更新赛事状态
     * 使用fixedRate保证每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) // 60秒 = 1分钟
    public void scheduledUpdateMatchStatus() {
        try {
            updateAllMatchStatusByTime();
            System.out.println("定时任务执行：自动更新赛事状态完成 - " + LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("定时任务执行失败：自动更新赛事状态 - " + e.getMessage());
        }
    }

    /**
     * 获取赛事时自动更新状态（确保返回最新状态）
     * 
     * @param matchId 赛事ID
     * @return 更新后的赛事信息
     */
    public Matchrecord getMatchByIdWithStatusUpdate(Long matchId) {
        Matchrecord match = matchrecordMapper.selectById(matchId);
        if (match != null && !MatchStatus.CANCELLED.equals(match.getStatus())) {
            String newStatus = determineMatchStatus(match.getMatchTime());
            if (!newStatus.equals(match.getStatus())) {
                match.setStatus(newStatus);
                match.setUpdateTime(LocalDateTime.now());
                matchrecordMapper.updateById(match);
            }
        }
        return match;
    }

    /**
     * 获取所有赛事时自动更新状态
     * 
     * @return 更新后的赛事列表
     */
    public List<Matchrecord> getAllMatchesWithStatusUpdate() {
        List<Matchrecord> matches = matchrecordMapper.selectList(null);
        
        for (Matchrecord match : matches) {
            if (!MatchStatus.CANCELLED.equals(match.getStatus())) {
                String newStatus = determineMatchStatus(match.getMatchTime());
                if (!newStatus.equals(match.getStatus())) {
                    match.setStatus(newStatus);
                    match.setUpdateTime(LocalDateTime.now());
                    matchrecordMapper.updateById(match);
                }
            }
        }
        
        // 重新查询更新后的列表
        return matchrecordMapper.selectList(new QueryWrapper<Matchrecord>().orderByDesc("match_time"));
    }
}

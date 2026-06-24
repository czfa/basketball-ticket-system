package com.example.ticket.controller;

import com.example.ticket.entity.Matchrecord;
import com.example.ticket.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赛事Controller
 * 负责处理用户端的赛事相关请求（列表展示、详情查询）
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchService matchService;

    /**
     * 获取赛事列表（分页、按时间排序）
     * GET /api/matches/list
     * 
     * @param pageNum 页码（从1开始），默认1
     * @param pageSize 每页数量，默认10
     * @param status 赛事状态筛选（可选）：UPCOMING/ONGOING/FINISHED/CANCELLED
     * @return 分页的赛事列表
     */
    @GetMapping("/list")
    public ResponseEntity<?> getMatchList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        
        try {
            List<Matchrecord> matches;
            
            // 如果有状态筛选，使用状态查询
            if (status != null && !status.trim().isEmpty()) {
                matches = matchService.getMatchesByStatus(status);
            } else {
                // 否则查询所有赛事（包括已结束的）
                matches = matchService.getAllMatches();
            }
            
            // 如果列表为空，直接返回空结果
            if (matches == null || matches.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", new java.util.ArrayList<>());
                response.put("total", 0);
                response.put("pageNum", pageNum);
                response.put("pageSize", pageSize);
                response.put("totalPages", 0);
                return ResponseEntity.ok(response);
            }
            
            // 手动分页（MyBatis-Plus分页插件需要特殊配置，这里先手动分页）
            int total = matches.size();
            int start = (pageNum - 1) * pageSize;
            int end = Math.min(start + pageSize, total);
            
            // 确保start不超出范围
            if (start >= total) {
                start = 0;
                end = Math.min(pageSize, total);
            }
            
            List<Matchrecord> pagedMatches = matches.subList(start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pagedMatches);
            response.put("total", total);
            response.put("pageNum", pageNum);
            response.put("pageSize", pageSize);
            response.put("totalPages", (total + pageSize - 1) / pageSize);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取赛事列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取单场赛事详情
     * GET /api/matches/{matchId}
     * 
     * @param matchId 赛事ID
     * @return 赛事详情（包含场馆、时间、票价、可用座位数等信息）
     */
    @GetMapping("/{matchId}")
    public ResponseEntity<?> getMatchDetail(@PathVariable Long matchId) {
        try {
            // 使用带状态更新的方法获取赛事信息
            Matchrecord match = matchService.getMatchByIdWithStatusUpdate(matchId);
            
            if (match == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "赛事不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            // 构建详细的赛事信息
            Map<String, Object> matchDetail = new HashMap<>();
            matchDetail.put("matchId", match.getMatchId());
            matchDetail.put("matchName", match.getMatchName());
            matchDetail.put("homeTeam", match.getHomeTeam());
            matchDetail.put("awayTeam", match.getAwayTeam());
            matchDetail.put("venue", match.getVenue());
            matchDetail.put("matchTime", match.getMatchTime());
            matchDetail.put("description", match.getDescription());
            matchDetail.put("totalSeats", match.getTotalSeats());
            matchDetail.put("availableSeats", match.getAvailableSeats());
            matchDetail.put("basePrice", match.getBasePrice());
            matchDetail.put("status", match.getStatus());
            
            // 添加状态中文描述
            matchDetail.put("statusText", getStatusText(match.getStatus()));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", matchDetail);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取赛事详情失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取赛事状态的中文描述
     * 
     * @param status 状态代码
     * @return 中文描述
     */
    private String getStatusText(String status) {
        switch (status) {
            case "UPCOMING":
                return "即将开始";
            case "ONGOING":
                return "进行中";
            case "FINISHED":
                return "已结束";
            case "CANCELLED":
                return "已取消";
            default:
                return "未知状态";
        }
    }

    /**
     * 搜索赛事（根据关键词搜索赛事名称、球队、场馆）
     * GET /api/matches/search
     * 
     * @param keyword 搜索关键词
     * @return 匹配的赛事列表
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMatches(@RequestParam String keyword) {
        try {
            List<Matchrecord> matches = matchService.searchMatches(keyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", matches);
            response.put("total", matches.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "搜索赛事失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取热门赛事（可用座位较少的比赛，表示受欢迎）
     * GET /api/matches/popular
     * 
     * @param limit 返回数量限制，默认5
     * @return 热门赛事列表
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularMatches(@RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<Matchrecord> matches = matchService.getPopularMatches(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", matches);
            response.put("total", matches.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取热门赛事失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

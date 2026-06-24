package com.example.ticket.controller;

import com.example.ticket.constant.MatchStatus;
import com.example.ticket.entity.Matchrecord;
import com.example.ticket.service.MatchService;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员赛事管理Controller
 * 负责处理管理员端的赛事信息管理（CRUD操作）
 */
@RestController
@RequestMapping("/api/admin/matches")
public class AdminMatchController {

    @Autowired
    private MatchService matchService;

    /**
     * 检查管理员登录状态
     * 只检查 admin_id 是否存在，不依赖 role 属性
     * 这样可以允许管理员和用户同时登录（各自独立的登录状态）
     */
    private boolean checkAdminLogin(HttpSession session) {
        Long adminId = (Long) session.getAttribute("admin_id");
        return adminId != null;
    }

    /**
     * 获取所有赛事列表（管理员端，不分页）
     * GET /api/admin/matches/list
     * 
     * @param session HTTP会话
     * @return 赛事列表
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAllMatches(HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            List<Matchrecord> matches = matchService.getAllMatches();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", matches);
            response.put("total", matches.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取赛事列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 新增赛事（管理员功能）
     * POST /api/admin/matches
     * 
     * @param matchrecord 赛事信息（请求体）
     * @param session HTTP会话
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<?> addMatch(@RequestBody Matchrecord matchrecord, HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            // 设置默认值
            if (matchrecord.getStatus() == null || matchrecord.getStatus().isEmpty()) {
                matchrecord.setStatus(MatchStatus.UPCOMING);
            }
            if (matchrecord.getAvailableSeats() == null) {
                matchrecord.setAvailableSeats(matchrecord.getTotalSeats() != null ? matchrecord.getTotalSeats() : 0);
            }

            boolean success = matchService.addMatch(matchrecord);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "赛事创建成功");
                response.put("data", matchrecord);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "赛事创建失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建赛事失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 更新赛事信息（管理员功能）
     * PUT /api/admin/matches/{matchId}
     * 
     * @param matchId 赛事ID
     * @param matchrecord 更新的赛事信息（请求体）
     * @param session HTTP会话
     * @return 更新结果
     */
    @PutMapping("/{matchId}")
    public ResponseEntity<?> updateMatch(
            @PathVariable Long matchId,
            @RequestBody Matchrecord matchrecord,
            HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            matchrecord.setMatchId(matchId);
            boolean success = matchService.updateMatch(matchrecord);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "赛事更新成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "赛事更新失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新赛事失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 删除赛事（管理员功能）
     * DELETE /api/admin/matches/{matchId}
     * 
     * @param matchId 赛事ID
     * @param session HTTP会话
     * @return 删除结果
     */
    @DeleteMapping("/{matchId}")
    public ResponseEntity<?> deleteMatch(@PathVariable Long matchId, HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            // 实际业务中应该检查是否有已售出的订单
            boolean success = matchService.deleteMatch(matchId);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "赛事删除成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "赛事删除失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除赛事失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 更新赛事状态（管理员功能）
     * PUT /api/admin/matches/{matchId}/status
     * 
     * @param matchId 赛事ID
     * @param request 包含status的请求体
     * @param session HTTP会话
     * @return 更新结果
     */
    @PutMapping("/{matchId}/status")
    public ResponseEntity<?> updateMatchStatus(
            @PathVariable Long matchId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "状态参数不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            boolean success = matchService.updateMatchStatus(matchId, status);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "赛事状态更新成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "赛事状态更新失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新赛事状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}

package com.example.ticket.controller;

import com.example.ticket.entity.Matchrecord;
import com.example.ticket.entity.Seat;
import com.example.ticket.mapper.SeatMapper;
import com.example.ticket.service.MatchService;
import com.example.ticket.service.SeatService;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员座位管理Controller
 * 负责处理管理员端的座位批量生成与维护
 */
@RestController
@RequestMapping("/api/admin/seats")
public class AdminSeatController {

    @Autowired
    private SeatService seatService;

    @Autowired
    private SeatMapper seatMapper;

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
     * 批量生成座位（管理员功能）
     * POST /api/admin/seats/generate
     * 
     * 请求体示例：
     * {
     *   "matchId": 1,
     *   "zones": [
     *     {"zone": "A区", "rows": 10, "colsPerRow": 20, "seatType": "STANDARD", "price": 100.00},
     *     {"zone": "B区", "rows": 5, "colsPerRow": 15, "seatType": "PREMIUM", "price": 200.00}
     *   ]
     * }
     * 
     * @param request 包含matchId和zones配置的请求体
     * @param session HTTP会话
     * @return 生成结果
     */
    @PostMapping("/generate")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> generateSeats(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            Long matchId = Long.valueOf(request.get("matchId").toString());
            
            // 验证赛事是否存在
            if (!matchService.existsMatch(matchId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "赛事不存在");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 检查该赛事是否已有座位
            Long existingSeatCount = seatService.countSeatsByMatchId(matchId);
            if (existingSeatCount > 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "该赛事已有座位，请先删除现有座位再重新生成");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> zones = (List<Map<String, Object>>) request.get("zones");
            
            if (zones == null || zones.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "座位配置不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            int totalSeats = 0;
            LocalDateTime now = LocalDateTime.now();

            // 遍历每个区域配置，批量生成座位
            for (Map<String, Object> zoneConfig : zones) {
                String zone = (String) zoneConfig.get("zone");
                Integer rows = Integer.valueOf(zoneConfig.get("rows").toString());
                // 兼容前端使用的 cols 参数名
                Integer colsPerRow = Integer.valueOf(zoneConfig.getOrDefault("cols", zoneConfig.get("colsPerRow")).toString());
                String seatType = (String) zoneConfig.getOrDefault("seatType", "STANDARD");
                BigDecimal price = new BigDecimal(zoneConfig.get("price").toString());

                // 生成该区域的所有座位
                for (int row = 1; row <= rows; row++) {
                    for (int col = 1; col <= colsPerRow; col++) {
                        Seat seat = new Seat();
                        seat.setMatchId(matchId);
                        seat.setSeatZone(zone);
                        seat.setSeatRow(row);
                        seat.setSeatCol(col);
                        seat.setSeatNumber(zone + row + "排" + col + "座");
                        seat.setSeatType(seatType);
                        seat.setPrice(price);
                        seat.setIsAvailable(true);
                        seat.setIsBooked(false);
                        seat.setOrderId(null);
                        seat.setCreateTime(now);
                        seat.setUpdateTime(now);

                        seatMapper.insert(seat);
                        totalSeats++;
                    }
                }
            }

            // 更新赛事的座位总数
            com.example.ticket.entity.Matchrecord match = matchService.getMatchById(matchId);
            if (match != null) {
                match.setTotalSeats(totalSeats);
                match.setAvailableSeats(totalSeats);
                matchService.updateMatch(match);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "座位生成成功");
            response.put("totalSeats", totalSeats);
            response.put("matchId", matchId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量生成座位失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取某场赛事的座位列表（管理员端）
     * GET /api/admin/seats/match/{matchId}
     * 
     * @param matchId 赛事ID
     * @param session HTTP会话
     * @return 座位列表
     */
    @GetMapping("/match/{matchId}")
    public ResponseEntity<?> getSeatsByMatch(@PathVariable Long matchId, HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            List<Seat> seats = seatService.getSeatsByMatchId(matchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", seats);
            response.put("total", seats.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取座位列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新座位信息（管理员功能，如设置不可用）
     * PUT /api/admin/seats/{seatId}
     * 
     * @param seatId 座位ID
     * @param request 包含更新字段的请求体（isAvailable, price等）
     * @param session HTTP会话
     * @return 更新结果
     */
    @PutMapping("/{seatId}")
    public ResponseEntity<?> updateSeat(
            @PathVariable Long seatId,
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            Seat seat = seatService.getSeatById(seatId);
            if (seat == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "座位不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 更新允许修改的字段
            if (request.containsKey("isAvailable")) {
                seat.setIsAvailable(Boolean.valueOf(request.get("isAvailable").toString()));
            }
            if (request.containsKey("price")) {
                seat.setPrice(new BigDecimal(request.get("price").toString()));
            }
            if (request.containsKey("seatType")) {
                seat.setSeatType((String) request.get("seatType"));
            }

            seat.setUpdateTime(LocalDateTime.now());

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Seat> updateWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            updateWrapper.eq("seat_id", seatId);
            int result = seatMapper.update(seat, updateWrapper);

            if (result > 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "座位更新成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "座位更新失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新座位失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 删除单个座位（管理员功能）
     * DELETE /api/admin/seats/{seatId}
     * 
     * @param seatId 座位ID
     * @param session HTTP会话
     * @return 删除结果
     */
    @DeleteMapping("/{seatId}")
    public ResponseEntity<?> deleteSeat(@PathVariable Long seatId, HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            Seat seat = seatService.getSeatById(seatId);
            if (seat == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "座位不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 检查座位是否已被预订
            if (Boolean.TRUE.equals(seat.getIsBooked())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "该座位已被预订，无法删除");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 删除座位
            int result = seatMapper.deleteById(seatId);

            if (result > 0) {
                // 更新赛事的座位数
                Long matchId = seat.getMatchId();
                Matchrecord match = matchService.getMatchById(matchId);
                if (match != null) {
                    match.setTotalSeats(match.getTotalSeats() - 1);
                    match.setAvailableSeats(match.getAvailableSeats() - 1);
                    matchService.updateMatch(match);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "座位删除成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "座位删除失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除座位失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量删除座位（管理员功能）
     * POST /api/admin/seats/batch-delete
     * 
     * @param request 包含seatIds列表的请求体
     * @param session HTTP会话
     * @return 删除结果
     */
    @PostMapping("/batch-delete")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> batchDeleteSeats(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<?> seatIdsRaw = (List<?>) request.get("seatIds");
            
            if (seatIdsRaw == null || seatIdsRaw.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "座位ID列表不能为空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 将前端传递的 Integer/Number 转换为 Long
            List<Long> seatIds = new java.util.ArrayList<>();
            for (Object id : seatIdsRaw) {
                if (id instanceof Number) {
                    seatIds.add(((Number) id).longValue());
                } else if (id instanceof String) {
                    seatIds.add(Long.parseLong((String) id));
                } else {
                    throw new IllegalArgumentException("座位ID格式不正确: " + id);
                }
            }

            int deletedCount = 0;
            Long matchId = null;

            // 遍历删除每个座位
            for (Long seatId : seatIds) {
                Seat seat = seatService.getSeatById(seatId);
                if (seat == null) {
                    continue;
                }

                // 检查座位是否已被预订
                if (Boolean.TRUE.equals(seat.getIsBooked())) {
                    continue;
                }

                int result = seatMapper.deleteById(seatId);
                if (result > 0) {
                    deletedCount++;
                    matchId = seat.getMatchId();
                }
            }

            // 更新赛事的座位数
            if (matchId != null && deletedCount > 0) {
                Matchrecord match = matchService.getMatchById(matchId);
                if (match != null) {
                    match.setTotalSeats(match.getTotalSeats() - deletedCount);
                    match.setAvailableSeats(match.getAvailableSeats() - deletedCount);
                    matchService.updateMatch(match);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量删除成功，共删除 " + deletedCount + " 个座位");
            response.put("deletedCount", deletedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量删除座位失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除某场赛事的所有座位（管理员功能，用于重新生成）
     * DELETE /api/admin/seats/match/{matchId}
     * 
     * @param matchId 赛事ID
     * @param session HTTP会话
     * @return 删除结果
     */
    @DeleteMapping("/match/{matchId}")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> deleteSeatsByMatch(@PathVariable Long matchId, HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            // 检查是否有已预订的座位
            List<Seat> bookedSeats = seatService.getBookedSeats(matchId);
            if (!bookedSeats.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "该赛事有已预订的座位，无法删除。已预订座位数: " + bookedSeats.size());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 删除该赛事的所有座位
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Seat> deleteWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            deleteWrapper.eq("match_id", matchId);
            int deletedCount = seatMapper.delete(deleteWrapper);

            // 更新赛事的座位数
            Matchrecord match = matchService.getMatchById(matchId);
            if (match != null) {
                match.setTotalSeats(0);
                match.setAvailableSeats(0);
                matchService.updateMatch(match);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "座位删除成功");
            response.put("deletedCount", deletedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除座位失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

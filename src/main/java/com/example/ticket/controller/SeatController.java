package com.example.ticket.controller;

import com.example.ticket.entity.Seat;
import com.example.ticket.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 座位Controller
 * 负责处理用户端的座位查询和选座相关请求
 */
@RestController
@RequestMapping("/api/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;

    /**
     * 获取某场赛事的座位列表
     * GET /api/seats/match/{matchId}
     * 
     * @param matchId 赛事ID
     * @return 该赛事的座位列表（包含座位状态：未售/已锁定/已售）
     */
    @GetMapping("/match/{matchId}")
    public ResponseEntity<?> getSeatsByMatch(@PathVariable Long matchId) {
        try {
            List<Seat> seats = seatService.getSeatsByMatchId(matchId);
            
            // 座位状态由前端根据 isAvailable 和 isBooked 字段判断：
            // is_available = false -> 不可用 (UNAVAILABLE)
            // is_available = true && is_booked = false -> 未售 (AVAILABLE)
            // is_available = true && is_booked = true -> 已售/已锁定 (BOOKED)
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", seats);
            response.put("total", seats.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取座位列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取某场赛事的可用座位列表（用于选座）
     * GET /api/seats/match/{matchId}/available
     * 
     * @param matchId 赛事ID
     * @return 可用座位列表（is_available=true 且 is_booked=false）
     */
    @GetMapping("/match/{matchId}/available")
    public ResponseEntity<?> getAvailableSeats(@PathVariable Long matchId) {
        try {
            List<Seat> seats = seatService.getAvailableSeats(matchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", seats);
            response.put("total", seats.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取可用座位失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据区域获取可用座位
     * GET /api/seats/match/{matchId}/zone/{zone}
     * 
     * @param matchId 赛事ID
     * @param zone 区域名称（如：A区）
     * @return 该区域的可用座位列表
     */
    @GetMapping("/match/{matchId}/zone/{zone}")
    public ResponseEntity<?> getSeatsByZone(
            @PathVariable Long matchId,
            @PathVariable String zone) {
        try {
            List<Seat> seats = seatService.getSeatsByZone(matchId, zone);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", seats);
            response.put("total", seats.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取区域座位失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据座位类型获取可用座位
     * GET /api/seats/match/{matchId}/type/{seatType}
     * 
     * @param matchId 赛事ID
     * @param seatType 座位类型（VIP/PREMIUM/STANDARD）
     * @return 该类型的可用座位列表
     */
    @GetMapping("/match/{matchId}/type/{seatType}")
    public ResponseEntity<?> getSeatsByType(
            @PathVariable Long matchId,
            @PathVariable String seatType) {
        try {
            List<Seat> seats = seatService.getSeatsByType(matchId, seatType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", seats);
            response.put("total", seats.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取座位类型失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据价格范围获取可用座位
     * GET /api/seats/match/{matchId}/price-range
     * 
     * @param matchId 赛事ID
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 价格范围内的可用座位列表
     */
    @GetMapping("/match/{matchId}/price-range")
    public ResponseEntity<?> getSeatsByPriceRange(
            @PathVariable Long matchId,
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice) {
        try {
            List<Seat> seats = seatService.getSeatsByPriceRange(matchId, minPrice, maxPrice);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", seats);
            response.put("total", seats.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取价格范围座位失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查座位是否可预订（用于前端实时验证）
     * POST /api/seats/check-availability
     * 
     * @param request 包含seatIds的请求体
     * @return 每个座位的可用性状态
     */
    @PostMapping("/check-availability")
    public ResponseEntity<?> checkSeatsAvailability(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<?> seatIdsRaw = (List<?>) request.get("seatIds");
            
            if (seatIdsRaw == null || seatIdsRaw.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "座位ID列表不能为空");
                return ResponseEntity.status(400).body(response);
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
            
            Map<String, Object> availabilityCheck = seatService.checkSeatsAvailability(seatIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", availabilityCheck);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检查座位可用性失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

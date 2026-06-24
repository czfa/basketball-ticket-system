package com.example.ticket.controller;

import com.example.ticket.constant.MatchStatus;
import com.example.ticket.constant.OrderStatus;
import com.example.ticket.entity.Order;
import com.example.ticket.service.MatchService;
import com.example.ticket.service.OrderService;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员统计Controller
 * 负责处理管理员端的订单管理与销售统计
 */
@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    @Autowired
    private OrderService orderService;

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
     * 获取所有订单列表（管理员端）
     * GET /api/admin/statistics/orders
     * 
     * @param session HTTP会话
     * @param status 订单状态筛选（可选）：PENDING/PAID/CANCELLED/REFUNDED/EXPIRED
     * @return 订单列表
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders(
            HttpSession session,
            @RequestParam(required = false) String status) {
        
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            List<Order> orders;
            
            if (status != null && !status.trim().isEmpty()) {
                orders = orderService.getOrdersByStatus(status);
            } else {
                orders = orderService.getAllOrders();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders);
            response.put("total", orders.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取订单列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取销售统计（总销售额、订单数量等）
     * GET /api/admin/statistics/sales
     * 
     * @param session HTTP会话
     * @return 销售统计数据
     */
    @GetMapping("/sales")
    public ResponseEntity<?> getSalesStatistics(HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            List<Order> allOrders = orderService.getAllOrders();
            
            // 计算总销售额（已支付订单）
            BigDecimal totalSales = allOrders.stream()
                    .filter(order -> OrderStatus.PAID.equals(order.getStatus()))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 计算订单统计
            long totalOrders = allOrders.size();
            long paidOrders = allOrders.stream().filter(order -> OrderStatus.PAID.equals(order.getStatus())).count();
            long pendingOrders = allOrders.stream().filter(order -> OrderStatus.PENDING.equals(order.getStatus())).count();
            long cancelledOrders = allOrders.stream().filter(order -> OrderStatus.CANCELLED.equals(order.getStatus())).count();
            long refundedOrders = allOrders.stream().filter(order -> OrderStatus.REFUNDED.equals(order.getStatus())).count();

            // 计算总票数（已支付订单）
            int totalTickets = allOrders.stream()
                    .filter(order -> OrderStatus.PAID.equals(order.getStatus()))
                    .mapToInt(order -> order.getSeatCount() != null ? order.getSeatCount() : 0)
                    .sum();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalSales", totalSales);
            statistics.put("totalOrders", totalOrders);
            statistics.put("paidOrders", paidOrders);
            statistics.put("pendingOrders", pendingOrders);
            statistics.put("cancelledOrders", cancelledOrders);
            statistics.put("refundedOrders", refundedOrders);
            statistics.put("totalTickets", totalTickets);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取销售统计失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取某场赛事的销售统计
     * GET /api/admin/statistics/match/{matchId}
     * 
     * @param matchId 赛事ID
     * @param session HTTP会话
     * @return 该赛事的销售统计数据
     */
    @GetMapping("/match/{matchId}")
    public ResponseEntity<?> getMatchStatistics(@PathVariable Long matchId, HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            // 获取赛事信息
            com.example.ticket.entity.Matchrecord match = matchService.getMatchById(matchId);
            if (match == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "赛事不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 获取该赛事的订单
            List<Order> matchOrders = orderService.getOrdersByMatchId(matchId);
            
            // 计算该赛事的销售额（已支付订单）
            BigDecimal matchSales = matchOrders.stream()
                    .filter(order -> OrderStatus.PAID.equals(order.getStatus()))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 计算该赛事的票数（已支付订单）
            int matchTickets = matchOrders.stream()
                    .filter(order -> OrderStatus.PAID.equals(order.getStatus()))
                    .mapToInt(order -> order.getSeatCount() != null ? order.getSeatCount() : 0)
                    .sum();

            // 计算该赛事的订单数
            long matchTotalOrders = matchOrders.size();
            long matchPaidOrders = matchOrders.stream().filter(order -> OrderStatus.PAID.equals(order.getStatus())).count();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("matchId", matchId);
            statistics.put("matchName", match.getMatchName());
            statistics.put("totalSales", matchSales);
            statistics.put("totalTickets", matchTickets);
            statistics.put("totalOrders", matchTotalOrders);
            statistics.put("paidOrders", matchPaidOrders);
            statistics.put("totalSeats", match.getTotalSeats());
            statistics.put("availableSeats", match.getAvailableSeats());
            statistics.put("soldSeats", match.getTotalSeats() - match.getAvailableSeats());
            statistics.put("salesRate", match.getTotalSeats() > 0 ? 
                (double)(match.getTotalSeats() - match.getAvailableSeats()) / match.getTotalSeats() * 100 : 0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取赛事统计失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取基础数据统计（场次销量、总销售额等汇总）
     * GET /api/admin/statistics/summary
     * 
     * @param session HTTP会话
     * @return 基础数据统计
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummaryStatistics(HttpSession session) {
        if (!checkAdminLogin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            // 赛事统计
            long totalMatches = matchService.countMatches();
            long upcomingMatches = matchService.countMatchesByStatus(MatchStatus.UPCOMING);
            long finishedMatches = matchService.countMatchesByStatus(MatchStatus.FINISHED);
            long cancelledMatches = matchService.countMatchesByStatus(MatchStatus.CANCELLED);

            // 订单统计
            List<Order> allOrders = orderService.getAllOrders();
            BigDecimal totalSales = allOrders.stream()
                    .filter(order -> OrderStatus.PAID.equals(order.getStatus()))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long totalOrders = allOrders.size();
            long paidOrders = allOrders.stream().filter(order -> OrderStatus.PAID.equals(order.getStatus())).count();

            // 票务统计
            int totalTickets = allOrders.stream()
                    .filter(order -> OrderStatus.PAID.equals(order.getStatus()))
                    .mapToInt(order -> order.getSeatCount() != null ? order.getSeatCount() : 0)
                    .sum();

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalMatches", totalMatches);
            summary.put("upcomingMatches", upcomingMatches);
            summary.put("finishedMatches", finishedMatches);
            summary.put("cancelledMatches", cancelledMatches);
            summary.put("totalSales", totalSales);
            summary.put("totalOrders", totalOrders);
            summary.put("paidOrders", paidOrders);
            summary.put("totalTickets", totalTickets);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取统计数据失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

package com.example.ticket.controller;

import com.example.ticket.constant.OrderStatus;
import com.example.ticket.entity.Matchrecord;
import com.example.ticket.entity.Order;
import com.example.ticket.entity.User;
import com.example.ticket.service.MatchService;
import com.example.ticket.service.OrderService;
import com.example.ticket.service.UserService;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单Controller
 * 负责处理用户端的订单相关请求（创建订单、查询订单、支付订单、取消订单）
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private MatchService matchService;

    /**
     * 创建订单（购票）
     * POST /api/orders/create
     * 
     * 请求体示例：
     * {
     *   "matchId": 1,
     *   "seatIds": [1, 2, 3]
     * }
     * 
     * @param request 包含matchId和seatIds的请求体
     * @param session HTTP会话，用于获取当前登录用户ID
     * @return 订单创建结果
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            // 优先从请求体获取 userId，如果没有则从 session 获取
            Long userId = null;
            Object userIdObj = request.get("userId");
            if (userIdObj != null) {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else {
                    userId = Long.valueOf(userIdObj.toString());
                }
            } else {
                // 从 session 获取
                userId = (Long) session.getAttribute("user_id");
            }
            
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用户未登录，请先登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 验证用户是否存在且有效
            User user = userService.getUserById(userId);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 获取请求参数
            Object matchIdObj = request.get("matchId");
            Long matchId;
            if (matchIdObj instanceof Number) {
                matchId = ((Number) matchIdObj).longValue();
            } else {
                matchId = Long.valueOf(matchIdObj.toString());
            }
            
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

            // 调用Service层创建订单并自动支付（包含事务控制、防重复购票）
            Map<String, Object> result = orderService.createOrderAndPay(userId, matchId, seatIds);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建订单失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取当前用户的订单列表
     * GET /api/orders/my-orders
     * 
     * @param session HTTP会话
     * @param status 订单状态筛选（可选）：PENDING/PAID/CANCELLED/REFUNDED/EXPIRED
     * @return 当前用户的订单列表
     */
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(
            HttpSession session,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "userId") Long userId) {
        
        // 优先从请求参数获取 userId，如果没有则从 session 获取
        if (userId == null) {
            userId = (Long) session.getAttribute("user_id");
        }
        
        // 调试日志
        System.out.println("getMyOrders - request param userId: " + userId);
        System.out.println("getMyOrders - session userId: " + session.getAttribute("user_id"));
        System.out.println("getMyOrders - final userId: " + userId);
        
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户未登录，请先登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 验证用户是否存在
        final Long finalUserId = userId; // 创建 final 变量用于 lambda
        User user = userService.getUserById(finalUserId);
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            List<Order> orders;
            
            if (status != null && !status.trim().isEmpty()) {
                // 查询特定状态的订单，但需要验证订单属于当前用户
                List<Order> allOrdersWithStatus = orderService.getOrdersByStatus(status);
                orders = allOrdersWithStatus.stream()
                        .filter(order -> order.getUserId().equals(finalUserId))
                        .collect(Collectors.toList());
            } else {
                // 查询当前用户的所有订单
                orders = orderService.getOrdersByUserId(finalUserId);
            }

            // 为每个订单添加赛事名称
            List<Map<String, Object>> ordersWithMatchName = orders.stream().map(order -> {
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("orderId", order.getOrderId());
                orderMap.put("orderNumber", order.getOrderNumber());
                orderMap.put("userId", order.getUserId());
                orderMap.put("matchId", order.getMatchId());
                orderMap.put("totalAmount", order.getTotalAmount());
                orderMap.put("actualAmount", order.getActualAmount());
                orderMap.put("seatCount", order.getSeatCount());
                orderMap.put("orderTime", order.getOrderTime());
                orderMap.put("payTime", order.getPayTime());
                orderMap.put("expireTime", order.getExpireTime());
                orderMap.put("status", order.getStatus());
                orderMap.put("payMethod", order.getPayMethod());
                orderMap.put("transactionId", order.getTransactionId());
                orderMap.put("refundTime", order.getRefundTime());
                orderMap.put("refundAmount", order.getRefundAmount());
                orderMap.put("refundReason", order.getRefundReason());
                orderMap.put("createTime", order.getCreateTime());
                orderMap.put("updateTime", order.getUpdateTime());
                
                // 查询赛事信息并添加赛事名称
                if (order.getMatchId() != null) {
                    Matchrecord match = matchService.getMatchById(order.getMatchId());
                    if (match != null) {
                        orderMap.put("matchName", match.getMatchName());
                    } else {
                        orderMap.put("matchName", "赛事信息不存在");
                    }
                } else {
                    orderMap.put("matchName", "未知赛事");
                }
                
                return orderMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ordersWithMatchName);
            response.put("total", ordersWithMatchName.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询订单失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取订单详情
     * GET /api/orders/{orderId}
     * 
     * @param orderId 订单ID
     * @param session HTTP会话
     * @return 订单详情
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(
            @PathVariable Long orderId, 
            HttpSession session,
            @RequestParam(required = false, name = "userId") Long userId) {
        
        // 优先从请求参数获取 userId，如果没有则从 session 获取
        if (userId == null) {
            userId = (Long) session.getAttribute("user_id");
        }
        
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 验证用户是否存在
        final Long finalUserId = userId; // 创建 final 变量
        User user = userService.getUserById(finalUserId);
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Order order = orderService.getOrderById(orderId);
            
            if (order == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "订单不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 验证订单是否属于当前用户
            if (!order.getUserId().equals(finalUserId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权查看此订单");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", order);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取订单详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 支付订单（模拟支付）
     * POST /api/orders/{orderId}/pay
     * 
     * @param orderId 订单ID
     * @param request 支付信息（payMethod, transactionId等）
     * @param session HTTP会话
     * @return 支付结果
     */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> payOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Order order = orderService.getOrderById(orderId);
            
            if (order == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "订单不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 验证订单是否属于当前用户
            if (!order.getUserId().equals(userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权操作此订单");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 验证订单状态
            if (!OrderStatus.PENDING.equals(order.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "订单状态不正确，只有待支付订单可以支付");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 验证订单是否过期
            if (order.getExpireTime() != null && 
                order.getExpireTime().isBefore(java.time.LocalDateTime.now())) {
                // 订单已过期，更新状态
                orderService.updateOrderStatus(orderId, OrderStatus.EXPIRED);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "订单已过期，请重新下单");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 模拟支付成功（实际项目中应调用支付接口）
            String payMethod = request.getOrDefault("payMethod", "ALIPAY");
            String transactionId = request.getOrDefault("transactionId", "TXN" + System.currentTimeMillis());

            // 支付订单（包含生成票务、更新订单状态等）
            Map<String, Object> paymentResult = orderService.payOrder(orderId, payMethod, transactionId);
            
            if ((Boolean) paymentResult.get("success")) {
                return ResponseEntity.ok(paymentResult);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(paymentResult);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "支付失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 取消订单
     * POST /api/orders/{orderId}/cancel
     * 
     * @param orderId 订单ID
     * @param session HTTP会话
     * @return 取消结果
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId, HttpSession session) {
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Order order = orderService.getOrderById(orderId);
            
            if (order == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "订单不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 验证订单是否属于当前用户
            if (!order.getUserId().equals(userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权操作此订单");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 只有待支付订单可以取消
            if (!OrderStatus.PENDING.equals(order.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "只有待支付订单可以取消");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 取消订单（会释放座位）
            boolean cancelled = orderService.cancelOrder(orderId);
            
            if (cancelled) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "订单取消成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "取消订单失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "取消订单失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 退票（退款）
     * POST /api/orders/{orderId}/refund
     * 
     * 请求体示例：
     * {
     *   "refundReason": "个人原因"
     * }
     * 
     * @param orderId 订单ID
     * @param request 包含退款原因的请求体
     * @param session HTTP会话
     * @return 退款结果
     */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<?> refundOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Order order = orderService.getOrderById(orderId);
            
            if (order == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "订单不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 验证订单是否属于当前用户
            if (!order.getUserId().equals(userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无权操作此订单");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 获取退款原因
            String refundReason = request.getOrDefault("refundReason", "用户申请退票");

            // 调用Service层进行退票
            Map<String, Object> result = orderService.refundOrder(orderId, refundReason);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "退票失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ticket.constant.OrderStatus;
import com.example.ticket.entity.Order;
import com.example.ticket.entity.Seat;
import com.example.ticket.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeatService seatService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private TicketsService ticketsService;

    /**
     * 更新订单状态
     */
    public boolean updateOrderStatus(Long orderId, String status) {
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            order.setStatus(status);
            order.setUpdateTime(LocalDateTime.now());

            // 如果是支付成功，设置支付时间
            if (OrderStatus.PAID.equals(status)) {
                order.setPayTime(LocalDateTime.now());
            }

            return orderMapper.updateById(order) > 0;
        }
        return false;
    }

    /**
     * 根据订单ID更新订单信息
     */
    public boolean updateOrder(Order order) {
        order.setUpdateTime(LocalDateTime.now());
        return orderMapper.updateById(order) > 0;
    }

    /**
     * 取消订单并释放座位（事务控制）
     * @param orderId 订单ID
     * @return 取消成功返回true
     * @throws RuntimeException 订单不存在或状态不正确时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId) {
        try {
            Order order = getOrderById(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }

            // 只有待支付订单可以取消
            if (!OrderStatus.PENDING.equals(order.getStatus())) {
                throw new RuntimeException("只有待支付订单可以取消");
            }

            // 查询该订单关联的所有座位
            List<Seat> orderSeats = seatService.getSeatsByOrderId(orderId);
            
            // 释放座位（将座位的is_booked设为false，order_id设为null）
            if (!orderSeats.isEmpty()) {
                List<Long> seatIds = orderSeats.stream()
                        .map(Seat::getSeatId)
                        .collect(Collectors.toList());
                seatService.releaseSeats(seatIds);
            }

            // 更新订单状态为已取消
            boolean cancelled = updateOrderStatus(orderId, OrderStatus.CANCELLED);

            return cancelled;
        } catch (RuntimeException e) {
            throw e; // 重新抛出，触发事务回滚
        } catch (Exception e) {
            throw new RuntimeException("取消订单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 退款订单（退票）
     * 使用事务控制保证原子性：更新订单状态 + 释放座位 + 更新赛事可用座位数
     * 
     * @param orderId 订单ID
     * @param refundReason 退款原因
     * @return 退款结果
     * @throws RuntimeException 订单不存在、状态不正确等情况抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refundOrder(Long orderId, String refundReason) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 查询订单
            Order order = getOrderById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            // 2. 验证订单状态（只有已支付订单可以退款）
            if (!OrderStatus.PAID.equals(order.getStatus())) {
                result.put("success", false);
                result.put("message", "只有已支付订单可以退款");
                return result;
            }

            // 3. 查询该订单关联的所有座位
            List<Seat> orderSeats = seatService.getSeatsByOrderId(orderId);
            
            if (orderSeats.isEmpty()) {
                result.put("success", false);
                result.put("message", "订单没有关联的座位");
                return result;
            }

            // 4. 释放座位（将座位的is_booked设为false，order_id设为null）
            List<Long> seatIds = orderSeats.stream()
                    .map(Seat::getSeatId)
                    .collect(Collectors.toList());
            seatService.releaseSeats(seatIds);

            // 5. 更新订单状态为已退款
            order.setStatus(OrderStatus.REFUNDED);
            order.setRefundAmount(order.getActualAmount()); // 全额退款
            order.setRefundReason(refundReason);
            order.setRefundTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            
            boolean updated = updateOrder(order);
            if (!updated) {
                throw new RuntimeException("更新订单状态失败");
            }

            // 6. 更新赛事可用座位数（退票后座位恢复可用）
            matchService.increaseAvailableSeats(order.getMatchId(), seatIds.size());

            result.put("success", true);
            result.put("message", "退款成功");
            result.put("orderId", orderId);
            result.put("refundAmount", order.getRefundAmount());
            result.put("refundTime", order.getRefundTime());

        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", "退款失败: " + e.getMessage());
            throw e; // 触发事务回滚
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "退款失败: " + e.getMessage());
            throw new RuntimeException("退款失败: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 创建订单（基础方法）
     * @param order 订单对象
     * @return 创建成功返回true
     */
    public boolean createOrder(Order order) {
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        return orderMapper.insert(order) > 0;
    }

    /**
     * 创建订单并预订座位（核心购票方法）
     * 使用事务控制保证原子性：订单创建 + 座位锁定 + 票务生成 要么全部成功，要么全部回滚
     * 防止重复购票：数据库层通过唯一索引，业务层通过座位状态检查
     * 
     * @param userId 用户ID
     * @param matchId 赛事ID
     * @param seatIds 座位ID列表
     * @return 包含订单信息的Map
     * @throws RuntimeException 座位不可用、赛事不存在、重复购票等情况抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrderWithSeats(Long userId, Long matchId, List<Long> seatIds) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证赛事是否存在且可预订
            if (!matchService.isMatchBookable(matchId)) {
                throw new RuntimeException("该赛事当前不可预订");
            }

            // 2. 检查座位可用性（防重复购票 - 业务层检查）
            Map<String, Object> availabilityCheck = seatService.checkSeatsAvailability(seatIds);
            Boolean allAvailable = (Boolean) availabilityCheck.get("allAvailable");
            @SuppressWarnings("unchecked")
            List<Long> unavailableSeats = (List<Long>) availabilityCheck.get("unavailableSeats");

            if (!allAvailable) {
                throw new RuntimeException("部分座位不可用，请重新选择。不可用座位ID: " + unavailableSeats);
            }

            // 3. 计算总金额（从座位表获取价格）
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Long seatId : seatIds) {
                Seat seat = seatService.getSeatById(seatId);
                if (seat == null) {
                    throw new RuntimeException("座位ID " + seatId + " 不存在");
                }
                // 验证座位属于该赛事
                if (!seat.getMatchId().equals(matchId)) {
                    throw new RuntimeException("座位ID " + seatId + " 不属于该赛事");
                }
                totalAmount = totalAmount.add(seat.getPrice());
            }

            // 4. 生成订单号（使用时间戳 + 随机数，避免并发重复）
            String orderNumber = "ORD" + System.currentTimeMillis() + (int)(Math.random() * 1000);

            // 5. 创建订单
            Order order = new Order();
            order.setOrderNumber(orderNumber);
            order.setUserId(userId);
            order.setMatchId(matchId);
            order.setTotalAmount(totalAmount);
            order.setActualAmount(totalAmount);
            order.setSeatCount(seatIds.size());
            order.setOrderTime(LocalDateTime.now());
            order.setExpireTime(LocalDateTime.now().plusMinutes(30)); // 30分钟过期
            order.setStatus(OrderStatus.PENDING);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            // 保存订单
            boolean orderCreated = createOrder(order);
            if (!orderCreated) {
                throw new RuntimeException("创建订单失败");
            }

            // 6. 获取刚创建的订单ID（通过订单号查询，避免并发问题）
            Order savedOrder = getOrderByOrderNumber(orderNumber);
            if (savedOrder == null) {
                throw new RuntimeException("订单创建失败，无法获取订单ID");
            }
            Long orderId = savedOrder.getOrderId();

            // 7. 预订座位（锁定座位，防止重复购票 - 数据库层锁定）
            // 使用悲观锁：UPDATE ... WHERE seat_id = ? AND is_booked = 0 AND is_available = 1
            // 这里通过bookSeats方法实现，该方法会检查座位状态并更新
            boolean seatsBooked = seatService.bookSeats(seatIds, orderId);
            if (!seatsBooked) {
                throw new RuntimeException("预订座位失败，可能座位已被其他用户预订");
            }

            // 8. 生成电子票（支付成功后生成，这里先预留）
            // 注意：实际业务中，票务应该在支付成功后生成，这里只是创建订单

            // 9. 更新赛事可用座位数
            // matchService.decreaseAvailableSeats(matchId, seatIds.size());

            result.put("success", true);
            result.put("orderId", orderId);
            result.put("orderNumber", orderNumber);
            result.put("totalAmount", totalAmount);
            result.put("seatCount", seatIds.size());
            result.put("expireTime", order.getExpireTime());
            result.put("message", "订单创建成功，请在30分钟内完成支付");

        } catch (RuntimeException e) {
            // 事务会自动回滚
            result.put("success", false);
            result.put("message", "创建订单失败: " + e.getMessage());
            throw e; // 重新抛出异常，触发事务回滚
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建订单失败: " + e.getMessage());
            throw new RuntimeException("创建订单失败: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 创建订单并自动支付（用于简化流程，无需支付二维码）
     * 使用事务控制保证原子性：订单创建 + 座位锁定 + 自动支付 + 票务生成
     * 
     * @param userId 用户ID
     * @param matchId 赛事ID
     * @param seatIds 座位ID列表
     * @return 包含订单信息的Map
     * @throws RuntimeException 座位不可用、赛事不存在、重复购票等情况抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrderAndPay(Long userId, Long matchId, List<Long> seatIds) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证赛事是否存在且可预订
            if (!matchService.isMatchBookable(matchId)) {
                throw new RuntimeException("该赛事当前不可预订");
            }

            // 2. 检查座位可用性（防重复购票 - 业务层检查）
            Map<String, Object> availabilityCheck = seatService.checkSeatsAvailability(seatIds);
            Boolean allAvailable = (Boolean) availabilityCheck.get("allAvailable");
            @SuppressWarnings("unchecked")
            List<Long> unavailableSeats = (List<Long>) availabilityCheck.get("unavailableSeats");

            if (!allAvailable) {
                throw new RuntimeException("部分座位不可用，请重新选择。不可用座位ID: " + unavailableSeats);
            }

            // 3. 计算总金额（从座位表获取价格）
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Long seatId : seatIds) {
                Seat seat = seatService.getSeatById(seatId);
                if (seat == null) {
                    throw new RuntimeException("座位ID " + seatId + " 不存在");
                }
                // 验证座位属于该赛事
                if (!seat.getMatchId().equals(matchId)) {
                    throw new RuntimeException("座位ID " + seatId + " 不属于该赛事");
                }
                totalAmount = totalAmount.add(seat.getPrice());
            }

            // 4. 生成订单号（使用时间戳 + 随机数，避免并发重复）
            String orderNumber = "ORD" + System.currentTimeMillis() + (int)(Math.random() * 1000);

            // 5. 创建订单（直接设置为已支付状态）
            Order order = new Order();
            order.setOrderNumber(orderNumber);
            order.setUserId(userId);
            order.setMatchId(matchId);
            order.setTotalAmount(totalAmount);
            order.setActualAmount(totalAmount);
            order.setSeatCount(seatIds.size());
            order.setOrderTime(LocalDateTime.now());
            order.setPayTime(LocalDateTime.now()); // 立即支付
            order.setStatus(OrderStatus.PAID); // 直接设置为已支付
            // pay_method 设置为 null，因为这是系统自动支付，不是用户选择的支付方式
            // 数据库 pay_method 枚举只允许 WECHAT/ALIPAY/BANK_CARD，不允许 SYSTEM
            order.setPayMethod(null);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            // 保存订单
            boolean orderCreated = createOrder(order);
            if (!orderCreated) {
                throw new RuntimeException("创建订单失败");
            }

            // 6. 获取刚创建的订单ID（通过订单号查询，避免并发问题）
            Order savedOrder = getOrderByOrderNumber(orderNumber);
            if (savedOrder == null) {
                throw new RuntimeException("订单创建失败，无法获取订单ID");
            }
            Long orderId = savedOrder.getOrderId();

            // 7. 预订座位（锁定座位，防止重复购票 - 数据库层锁定）
            boolean seatsBooked = seatService.bookSeats(seatIds, orderId);
            if (!seatsBooked) {
                throw new RuntimeException("预订座位失败，可能座位已被其他用户预订");
            }

            // 8. 生成电子票（支付成功，立即生成票务）
            boolean ticketsIssued = ticketsService.issueTicketsForOrder(orderId, seatIds);
            if (!ticketsIssued) {
                throw new RuntimeException("生成电子票失败");
            }

            // 9. 更新赛事可用座位数（已支付，座位真正售出）
            matchService.decreaseAvailableSeats(matchId, seatIds.size());

            result.put("success", true);
            result.put("orderId", orderId);
            result.put("orderNumber", orderNumber);
            result.put("totalAmount", totalAmount);
            result.put("seatCount", seatIds.size());
            result.put("message", "订单创建并支付成功，电子票已生成");

        } catch (RuntimeException e) {
            // 事务会自动回滚
            result.put("success", false);
            result.put("message", "创建订单失败: " + e.getMessage());
            throw e; // 重新抛出异常，触发事务回滚
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建订单失败: " + e.getMessage());
            throw new RuntimeException("创建订单失败: " + e.getMessage(), e);
        }

        return result;
    }
    
    // 根据用户ID查询订单
    public List<Order> getOrdersByUserId(Long userId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("create_time");
        return orderMapper.selectList(queryWrapper);
    }

    // 根据订单状态查询
    public List<Order> getOrdersByStatus(String status) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status)
                .orderByDesc("create_time");
        return orderMapper.selectList(queryWrapper);
    }

    // 根据订单号查询
    public Order getOrderByOrderNumber(String orderNumber) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_number", orderNumber);
        return orderMapper.selectOne(queryWrapper);
    }

    // 根据比赛ID查询订单
    public List<Order> getOrdersByMatchId(Long matchId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .orderByDesc("create_time");
        return orderMapper.selectList(queryWrapper);
    }

    // 查询所有订单
    public List<Order> getAllOrders() {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        return orderMapper.selectList(queryWrapper);
    }

    // 根据订单ID查询
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    /**
     * 支付订单（支付成功后调用）
     * 支付成功后需要：更新订单状态 + 生成电子票 + 更新赛事可用座位数
     * 
     * @param orderId 订单ID
     * @param payMethod 支付方式
     * @param transactionId 支付交易号
     * @return 支付结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> payOrder(Long orderId, String payMethod, String transactionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Order order = getOrderById(orderId);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }

            // 验证订单状态
            if (!OrderStatus.PENDING.equals(order.getStatus())) {
                result.put("success", false);
                result.put("message", "订单状态不正确，只有待支付订单可以支付");
                return result;
            }

            // 验证订单是否过期
            if (order.getExpireTime() != null && 
                order.getExpireTime().isBefore(LocalDateTime.now())) {
                // 订单已过期，更新状态
                updateOrderStatus(orderId, OrderStatus.EXPIRED);
                result.put("success", false);
                result.put("message", "订单已过期，请重新下单");
                return result;
            }

            // 1. 获取订单关联的座位ID列表
            List<Seat> orderSeats = seatService.getSeatsByOrderId(orderId);
            List<Long> seatIds = orderSeats.stream()
                    .map(Seat::getSeatId)
                    .collect(Collectors.toList());

            // 2. 生成电子票（为每个座位生成一张票）
            boolean ticketsIssued = ticketsService.issueTicketsForOrder(orderId, seatIds);
            if (!ticketsIssued) {
                throw new RuntimeException("生成电子票失败");
            }

            // 3. 更新订单状态为已支付
            boolean paymentSuccess = updateOrderStatus(orderId, OrderStatus.PAID);
            if (!paymentSuccess) {
                throw new RuntimeException("更新订单状态失败");
            }

            // 4. 更新订单的支付相关信息
            order.setPayMethod(payMethod);
            order.setTransactionId(transactionId);
            updateOrder(order);

            // 5. 更新赛事可用座位数（已支付，座位真正售出）
            matchService.decreaseAvailableSeats(order.getMatchId(), seatIds.size());

            result.put("success", true);
            result.put("message", "支付成功，电子票已生成");
            result.put("orderId", orderId);
            result.put("orderNumber", order.getOrderNumber());
            result.put("transactionId", transactionId);
            result.put("ticketCount", seatIds.size());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "支付失败: " + e.getMessage());
            throw new RuntimeException("支付失败: " + e.getMessage(), e); // 触发事务回滚
        }

        return result;
    }
    public List<Order> debugGetOrdersByUserId(Long userId) {
        System.out.println("DEBUG: 查询用户ID " + userId + " 的订单");

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("create_time");

        List<Order> orders = orderMapper.selectList(queryWrapper);
        System.out.println("DEBUG: 找到 " + orders.size() + " 个订单");

        return orders;
    }
}

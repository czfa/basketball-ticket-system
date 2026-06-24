package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.ticket.constant.CheckStatus;
import com.example.ticket.constant.TicketStatus;
import com.example.ticket.entity.Tickets;
import com.example.ticket.mapper.TicketsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketsService {
    @Autowired
    private TicketsMapper ticketsMapper;

    // 根据订单ID查询票务信息
    public List<Tickets> getTicketsByOrderId(Long orderId) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .orderByDesc("create_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 根据座位ID查询票务信息
    public List<Tickets> getTicketsBySeatId(Long seatId) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("seat_id", seatId)
                .orderByDesc("create_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 根据票号查询票务信息
    public Tickets getTicketByTicketNumber(String ticketNumber) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ticket_number", ticketNumber);
        return ticketsMapper.selectOne(queryWrapper);
    }

    // 根据检票状态查询票务信息
    public List<Tickets> getTicketsByCheckStatus(String checkStatus) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("check_status", checkStatus)
                .orderByDesc("create_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 根据票状态查询票务信息
    public List<Tickets> getTicketsByStatus(String status) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status)
                .orderByDesc("create_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    /**
     * 查询所有未检票的票务
     * 
     * @return 未检票的票务列表
     */
    public List<Tickets> getUncheckedTickets() {
        return getTicketsByCheckStatus(CheckStatus.UNCHECKED);
    }

    /**
     * 查询所有已检票的票务
     * 
     * @return 已检票的票务列表
     */
    public List<Tickets> getCheckedTickets() {
        return getTicketsByCheckStatus(CheckStatus.CHECKED);
    }

    /**
     * 查询所有有效的票务
     * 
     * @return 有效的票务列表
     */
    public List<Tickets> getValidTickets() {
        return getTicketsByStatus(TicketStatus.VALID);
    }

    /**
     * 查询所有已使用的票务
     * 
     * @return 已使用的票务列表
     */
    public List<Tickets> getUsedTickets() {
        return getTicketsByStatus(TicketStatus.USED);
    }

    /**
     * 查询所有已退款的票务
     * 
     * @return 已退款的票务列表
     */
    public List<Tickets> getRefundedTickets() {
        return getTicketsByStatus(TicketStatus.REFUNDED);
    }

    /**
     * 查询所有已过期的票务
     * 
     * @return 已过期的票务列表
     */
    public List<Tickets> getExpiredTickets() {
        return getTicketsByStatus(TicketStatus.EXPIRED);
    }

    // 根据票务ID查询具体票务信息
    public Tickets getTicketById(Long ticketId) {
        return ticketsMapper.selectById(ticketId);
    }

    // 查询所有票务信息
    public List<Tickets> getAllTickets() {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 统计票务数量
    public Long countTickets() {
        return ticketsMapper.selectCount(null);
    }

    // 统计特定状态的票务数量
    public Long countTicketsByStatus(String status) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        return ticketsMapper.selectCount(queryWrapper);
    }

    // 统计特定检票状态的票务数量
    public Long countTicketsByCheckStatus(String checkStatus) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("check_status", checkStatus);
        return ticketsMapper.selectCount(queryWrapper);
    }

    // 根据订单ID统计票务数量
    public Long countTicketsByOrderId(Long orderId) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        return ticketsMapper.selectCount(queryWrapper);
    }

   /* // 新增票务记录（出票）
    @Transactional
    public boolean issueTicket(Tickets ticket) {
        ticket.setCreateTime(LocalDateTime.now());
        ticket.setUpdateTime(LocalDateTime.now());
        ticket.setIssueTime(LocalDateTime.now());
        ticket.setStatus("VALID");
        ticket.setCheckStatus("UNCHECKED");
        return ticketsMapper.insert(ticket) > 0;
    }

    // 批量出票
    @Transactional
    public boolean batchIssueTickets(List<Tickets> tickets) {
        LocalDateTime now = LocalDateTime.now();
        for (Tickets ticket : tickets) {
            ticket.setCreateTime(now);
            ticket.setUpdateTime(now);
            ticket.setIssueTime(now);
            ticket.setStatus("VALID");
            ticket.setCheckStatus("UNCHECKED");
        }
        // 注意：这里需要TicketsMapper支持批量插入
        return tickets.forEach(ticket -> ticketsMapper.insert(ticket)) > 0;
    }

    // 更新票务信息
    @Transactional
    public boolean updateTicket(Tickets ticket) {
        ticket.setUpdateTime(LocalDateTime.now());
        return ticketsMapper.updateById(ticket) > 0;
    }

    // 删除票务
    @Transactional
    public boolean deleteTicket(Long ticketId) {
        return ticketsMapper.deleteById(ticketId) > 0;
    }

    // 批量删除票务
    @Transactional
    public boolean batchDeleteTickets(List<Long> ticketIds) {
        return ticketsMapper.deleteBatchIds(ticketIds) > 0;
    }

    // 检票操作
    @Transactional
    public boolean checkTicket(Long ticketId, String operator) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("check_status", "CHECKED")
                .set("check_time", LocalDateTime.now())
                .set("check_operator", operator)
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 根据票号检票
    @Transactional
    public boolean checkTicketByNumber(String ticketNumber, String operator) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_number", ticketNumber)
                .eq("check_status", "UNCHECKED") // 确保未检票
                .set("check_status", "CHECKED")
                .set("check_time", LocalDateTime.now())
                .set("check_operator", operator)
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 取消检票（用于误操作）
    @Transactional
    public boolean cancelCheckTicket(Long ticketId) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("check_status", "UNCHECKED")
                .set("check_time", null)
                .set("check_operator", null)
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 更新票状态
    @Transactional
    public boolean updateTicketStatus(Long ticketId, String status) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("status", status)
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 批量更新票状态
    @Transactional
    public boolean batchUpdateTicketStatus(List<Long> ticketIds, String status) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("ticket_id", ticketIds)
                .set("status", status)
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 退款操作
    @Transactional
    public boolean refundTicket(Long ticketId) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("status", "REFUNDED")
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 批量退款
    @Transactional
    public boolean batchRefundTickets(List<Long> ticketIds) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("ticket_id", ticketIds)
                .set("status", "REFUNDED")
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 标记票为已使用
    @Transactional
    public boolean markTicketAsUsed(Long ticketId) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("status", "USED")
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 标记票为过期
    @Transactional
    public boolean markTicketAsExpired(Long ticketId) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("status", "EXPIRED")
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 批量标记票为过期
    @Transactional
    public boolean batchMarkTicketsAsExpired(List<Long> ticketIds) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("ticket_id", ticketIds)
                .set("status", "EXPIRED")
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    // 更新二维码数据
    @Transactional
    public boolean updateQrCode(Long ticketId, String qrCode) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("qr_code", qrCode)
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }*/

    /**
     * 检查票是否有效（可检票）
     * 
     * @param ticketId 票务ID
     * @return 可检票返回true
     */
    public boolean isTicketValidForCheck(Long ticketId) {
        Tickets ticket = ticketsMapper.selectById(ticketId);
        return ticket != null &&
                TicketStatus.VALID.equals(ticket.getStatus()) &&
                CheckStatus.UNCHECKED.equals(ticket.getCheckStatus());
    }

    /**
     * 检查票号是否有效（可检票）
     * 
     * @param ticketNumber 票号
     * @return 可检票返回true
     */
    public boolean isTicketNumberValidForCheck(String ticketNumber) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ticket_number", ticketNumber)
                .eq("status", TicketStatus.VALID)
                .eq("check_status", CheckStatus.UNCHECKED);
        return ticketsMapper.selectCount(queryWrapper) > 0;
    }

    // 根据订单ID和状态查询票务
    public List<Tickets> getTicketsByOrderIdAndStatus(Long orderId, String status) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .eq("status", status)
                .orderByDesc("create_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 根据订单ID和检票状态查询票务
    public List<Tickets> getTicketsByOrderIdAndCheckStatus(Long orderId, String checkStatus) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .eq("check_status", checkStatus)
                .orderByDesc("create_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 查询特定时间范围内创建的票务
    public List<Tickets> getTicketsByCreateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("create_time", startTime, endTime)
                .orderByDesc("create_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 查询特定时间范围内出票的票务
    public List<Tickets> getTicketsByIssueTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("issue_time", startTime, endTime)
                .orderByDesc("issue_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 查询特定时间范围内检票的票务
    public List<Tickets> getTicketsByCheckTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.between("check_time", startTime, endTime)
                .orderByDesc("check_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 根据检票操作员查询票务
    public List<Tickets> getTicketsByCheckOperator(String checkOperator) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("check_operator", checkOperator)
                .orderByDesc("check_time");
        return ticketsMapper.selectList(queryWrapper);
    }

    // 检查票号是否已存在
    public boolean existsTicketNumber(String ticketNumber) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ticket_number", ticketNumber);
        return ticketsMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 获取订单的有效票数量
     * 
     * @param orderId 订单ID
     * @return 有效票数量
     */
    public Long countValidTicketsByOrderId(Long orderId) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .eq("status", TicketStatus.VALID);
        return ticketsMapper.selectCount(queryWrapper);
    }

    /**
     * 获取订单的已检票数量
     * 
     * @param orderId 订单ID
     * @return 已检票数量
     */
    public Long countCheckedTicketsByOrderId(Long orderId) {
        QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .eq("check_status", CheckStatus.CHECKED);
        return ticketsMapper.selectCount(queryWrapper);
    }

    // 获取今天创建的票务
    public List<Tickets> getTodayCreatedTickets() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return getTicketsByCreateTimeRange(startOfDay, endOfDay);
    }

    // 获取今天检票的票务
    public List<Tickets> getTodayCheckedTickets() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return getTicketsByCheckTimeRange(startOfDay, endOfDay);
    }
    /**
     * 为订单生成票务（支付成功后调用）
     * 为订单关联的每个座位生成一张电子票
     * 
     * @param orderId 订单ID
     * @param seatIds 座位ID列表
     * @return 生成成功返回true
     * @throws RuntimeException 生成失败时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean issueTicketsForOrder(Long orderId, List<Long> seatIds) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            for (Long seatId : seatIds) {
                // 生成唯一票号（使用订单ID + 座位ID + 时间戳，避免重复）
                String ticketNumber = "TICK" + orderId + "-" + seatId + "-" + System.currentTimeMillis();
                
                // 检查票号是否已存在（防止重复）
                Tickets existingTicket = getTicketByTicketNumber(ticketNumber);
                if (existingTicket != null) {
                    // 如果票号重复，重新生成
                    ticketNumber = "TICK" + orderId + "-" + seatId + "-" + System.currentTimeMillis() + (int)(Math.random() * 1000);
                }
                
                Tickets ticket = new Tickets();
                ticket.setTicketNumber(ticketNumber);
                ticket.setOrderId(orderId);
                ticket.setSeatId(seatId);
                ticket.setStatus(TicketStatus.VALID); // 票状态：有效
                ticket.setCheckStatus(CheckStatus.UNCHECKED); // 检票状态：未检票
                ticket.setIssueTime(now); // 出票时间
                ticket.setCreateTime(now);
                ticket.setUpdateTime(now);
                // 生成二维码数据（包含订单ID、座位ID、票号等信息，用于检票验证）
                String qrCodeData = String.format("ORDER:%d|SEAT:%d|TICKET:%s|TIME:%d", 
                    orderId, seatId, ticketNumber, System.currentTimeMillis());
                ticket.setQrCode(qrCodeData);

                boolean issued = issueTicket(ticket);
                if (!issued) {
                    throw new RuntimeException("生成电子票失败，座位ID: " + seatId);
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("批量生成电子票失败: " + e.getMessage(), e);
        }
    }

    /**
     * 出票方法（单个票务记录插入）
     * @param ticket 票务对象
     * @return 插入成功返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean issueTicket(Tickets ticket) {
        if (ticket.getCreateTime() == null) {
            ticket.setCreateTime(LocalDateTime.now());
        }
        if (ticket.getUpdateTime() == null) {
            ticket.setUpdateTime(LocalDateTime.now());
        }
        if (ticket.getIssueTime() == null) {
            ticket.setIssueTime(LocalDateTime.now());
        }
        return ticketsMapper.insert(ticket) > 0;
    }
    /**
     * 更新票务信息
     */
    public boolean updateTicket(Tickets ticket) {
        ticket.setUpdateTime(LocalDateTime.now());
        return ticketsMapper.updateById(ticket) > 0;
    }

    /**
     * 更新票务状态
     */
    public boolean updateTicketStatus(Long ticketId, String status) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("status", status)
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }

    /**
     * 更新检票状态
     */
    public boolean updateCheckStatus(Long ticketId, String checkStatus, String operator) {
        UpdateWrapper<Tickets> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ticket_id", ticketId)
                .set("check_status", checkStatus)
                .set("check_time", LocalDateTime.now())
                .set("check_operator", operator)
                .set("update_time", LocalDateTime.now());
        return ticketsMapper.update(null, updateWrapper) > 0;
    }
    /**
     * 根据订单ID更新票务状态
    *//**
     * 根据订单ID更新票务状态
     */
    /*public boolean updateTicketsStatusByOrderId(Long orderId, String status) {
        try {
            QueryWrapper<Tickets> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("order_id", orderId);

            List<Tickets> tickets = ticketsMapper.selectList(queryWrapper);
            if (tickets.isEmpty()) {
                return false;
            }

            for (Tickets ticket : tickets) {
                ticket.setStatus(status);
                ticket.setUpdateTime(LocalDateTime.now());

                int result = ticketsMapper.updateById(ticket);
                if (result <= 0) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("更新票务状态失败: " + e.getMessage());
        }
    }*/



}

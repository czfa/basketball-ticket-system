package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ticket.entity.Seat;
import com.example.ticket.mapper.SeatMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SeatService {
    @Autowired
    private SeatMapper seatMapper;

    // 根据比赛ID查询座位
    public List<Seat> getSeatsByMatchId(Long matchId) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .orderByAsc("seat_zone", "seat_row", "seat_col");
        return seatMapper.selectList(queryWrapper);
    }

    // 根据区域查询座位
    public List<Seat> getSeatsByZone(Long matchId, String zone) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .eq("seat_zone", zone)
                .orderByAsc("seat_row", "seat_col");
        return seatMapper.selectList(queryWrapper);
    }

    // 根据座位类型查询
    public List<Seat> getSeatsByType(Long matchId, String seatType) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .eq("seat_type", seatType)
                .orderByAsc("seat_zone", "seat_row", "seat_col");
        return seatMapper.selectList(queryWrapper);
    }

    // 查询可用的座位
    public List<Seat> getAvailableSeats(Long matchId) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .eq("is_available", true)
                .eq("is_booked", false)
                .orderByAsc("seat_zone", "seat_row", "seat_col");
        return seatMapper.selectList(queryWrapper);
    }

    // 查询已预订的座位
    public List<Seat> getBookedSeats(Long matchId) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .eq("is_booked", true)
                .orderByAsc("seat_zone", "seat_row", "seat_col");
        return seatMapper.selectList(queryWrapper);
    }

    // 根据价格范围查询座位
    public List<Seat> getSeatsByPriceRange(Long matchId, Double minPrice, Double maxPrice) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .between("price", minPrice, maxPrice)
                .orderByAsc("price", "seat_zone", "seat_row");
        return seatMapper.selectList(queryWrapper);
    }

    // 根据座位ID查询具体座位
    public Seat getSeatById(Long seatId) {
        return seatMapper.selectById(seatId);
    }

    // 根据座位号查询座位
    public Seat getSeatByNumber(Long matchId, String seatNumber) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .eq("seat_number", seatNumber);
        return seatMapper.selectOne(queryWrapper);
    }

    /**
     * 根据订单ID查询座位列表
     * @param orderId 订单ID
     * @return 该订单关联的座位列表
     */
    public List<Seat> getSeatsByOrderId(Long orderId) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId)
                .orderByAsc("seat_zone", "seat_row", "seat_col");
        return seatMapper.selectList(queryWrapper);
    }

    /**
     * 释放座位（取消预订时调用）
     * @param seatIds 座位ID列表
     * @return 是否释放成功
     */
    public boolean releaseSeats(List<Long> seatIds) {
        try {
            for (Long seatId : seatIds) {
                Seat seat = getSeatById(seatId);
                if (seat == null) {
                    throw new RuntimeException("座位ID " + seatId + " 不存在");
                }

                // 释放座位
                seat.setIsBooked(false);
                seat.setOrderId(null);
                seat.setUpdateTime(LocalDateTime.now());

                QueryWrapper<Seat> updateWrapper = new QueryWrapper<>();
                updateWrapper.eq("seat_id", seatId);
                int result = seatMapper.update(seat, updateWrapper);

                if (result <= 0) {
                    throw new RuntimeException("释放座位 " + seat.getSeatNumber() + " 失败");
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("释放座位失败: " + e.getMessage());
        }
    }

    // 查询所有座位
    public List<Seat> getAllSeats() {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("match_id", "seat_zone", "seat_row", "seat_col");
        return seatMapper.selectList(queryWrapper);
    }

    // 统计某个比赛的座位数量
    public Long countSeatsByMatchId(Long matchId) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId);
        return seatMapper.selectCount(queryWrapper);
    }

    public SeatMapper getSeatMapper() {
        return seatMapper;
    }
    // 统计某个比赛可用座位数量
    public Long countAvailableSeatsByMatchId(Long matchId) {
        QueryWrapper<Seat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("match_id", matchId)
                .eq("is_available", true)
                .eq("is_booked", false);
        return seatMapper.selectCount(queryWrapper);
    }
    /**
     * 检查座位可用性（用于购票前验证）
     * @param seatIds 座位ID列表
     * @return 包含可用座位、不可用座位和是否全部可用的Map
     */
    public Map<String, Object> checkSeatsAvailability(List<Long> seatIds) {
        Map<String, Object> result = new java.util.HashMap<>();
        List<Long> availableSeats = new java.util.ArrayList<>();
        List<Long> unavailableSeats = new java.util.ArrayList<>();

        for (Long seatId : seatIds) {
            Seat seat = getSeatById(seatId);
            if (seat == null) {
                unavailableSeats.add(seatId);
                continue;
            }

            // 检查座位是否可用且未预订
            if (Boolean.TRUE.equals(seat.getIsAvailable()) && 
                Boolean.FALSE.equals(seat.getIsBooked())) {
                availableSeats.add(seatId);
            } else {
                unavailableSeats.add(seatId);
            }
        }

        result.put("availableSeats", availableSeats);
        result.put("unavailableSeats", unavailableSeats);
        result.put("allAvailable", unavailableSeats.isEmpty());

        return result;
    }

    /**
     * 预订座位（用于订单创建时的座位锁定）
     * @param seatIds 座位ID列表
     * @param orderId 订单ID
     * @return 是否预订成功
     * @throws RuntimeException 座位不存在或不可预订时抛出异常
     */
    public boolean bookSeats(List<Long> seatIds, Long orderId) {
        try {
            for (Long seatId : seatIds) {
                Seat seat = getSeatById(seatId);
                if (seat == null) {
                    throw new RuntimeException("座位ID " + seatId + " 不存在");
                }

                if (!Boolean.TRUE.equals(seat.getIsAvailable()) ||
                        Boolean.TRUE.equals(seat.getIsBooked())) {
                    throw new RuntimeException("座位 " + seat.getSeatNumber() + " 不可预订");
                }

                // 更新座位状态为已预订
                seat.setIsBooked(true);
                seat.setOrderId(orderId);
                seat.setUpdateTime(LocalDateTime.now());

                // 更新数据库
                QueryWrapper<Seat> updateWrapper = new QueryWrapper<>();
                updateWrapper.eq("seat_id", seatId);
                int result = seatMapper.update(seat, updateWrapper);

                if (result <= 0) {
                    throw new RuntimeException("预订座位 " + seat.getSeatNumber() + " 失败");
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("预订座位失败: " + e.getMessage());
        }
    }
}

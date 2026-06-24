package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ticket.entity.*;
import com.example.ticket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    public boolean bookTickets(String username, Long matchId, List<Long> seatIds) {
        return false;
    }
}
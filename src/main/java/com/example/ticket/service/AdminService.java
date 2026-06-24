package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ticket.entity.Admin;
import com.example.ticket.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员Service
 * 负责管理员相关的业务逻辑
 */
@Service
public class AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private PasswordEncoder passwordEncoder; // 用于密码加密和校验

    /**
     * 管理员登录验证逻辑
     * @param username 管理员用户名
     * @param rawPassword 原始密码（明文）
     * @return 登录成功返回Admin对象，失败返回null
     */
    public Admin login(String username, String rawPassword) {
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", username));

        if (admin != null && "ACTIVE".equals(admin.getStatus())) {
            String storedPassword = admin.getPassword();
            boolean passwordMatches = false;
            
            // 判断密码存储格式：BCrypt 加密的密码以 $2a$、$2b$ 或 $2y$ 开头
            if (storedPassword != null && (storedPassword.startsWith("$2a$") || 
                    storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$"))) {
                // 使用BCrypt密码匹配器验证加密密码
                passwordMatches = passwordEncoder.matches(rawPassword, storedPassword);
            } else {
                // 明文密码直接比较（兼容旧数据）
                passwordMatches = rawPassword != null && rawPassword.equals(storedPassword);
            }
            
            if (passwordMatches) {
                // 更新最后登录时间
                admin.setLastLoginTime(LocalDateTime.now());
                admin.setUpdateTime(LocalDateTime.now());
                adminMapper.updateById(admin);
                return admin;
            }
        }
        return null;
    }

    /**
     * 根据管理员ID获取管理员信息
     * @param adminId 管理员ID
     * @return Admin对象
     */
    public Admin getAdminById(Long adminId) {
        return adminMapper.selectById(adminId);
    }

    /**
     * 根据用户名获取管理员信息
     * @param username 用户名
     * @return Admin对象
     */
    public Admin getAdminByUsername(String username) {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return adminMapper.selectOne(queryWrapper);
    }

    /**
     * 查询所有管理员
     * @return 管理员列表
     */
    public List<Admin> getAllAdmins() {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        return adminMapper.selectList(queryWrapper);
    }
}

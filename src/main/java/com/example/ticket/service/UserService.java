package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ticket.constant.UserRole;
import com.example.ticket.constant.UserStatus;
import com.example.ticket.entity.User;
import com.example.ticket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder; // 用于密码加密和校验

    /**
     * 用户注册逻辑
     * @param user 用户实体对象
     * @throws RuntimeException 用户名或邮箱已存在时抛出异常
     */
    public void register(User user) {
        // 检查用户名是否已存在
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", user.getUsername())) != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 检查邮箱是否已存在
        if (user.getEmail() != null && userMapper.selectOne(new QueryWrapper<User>().eq("email", user.getEmail())) != null) {
            throw new RuntimeException("邮箱已存在");
        }

        // 密码加密存储
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        LocalDateTime now = LocalDateTime.now();
        user.setRegisterTime(now);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdateTime(now);

        // 插入用户记录
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录验证逻辑
     * @param username 用户名
     * @param rawPassword 原始密码（明文）
     * @return 登录成功返回User对象，失败返回null
     */
    public User login(String username, String rawPassword) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));

        if (user != null && UserStatus.ACTIVE.equals(user.getStatus())) {
            String storedPassword = user.getPassword();
            
            // 兼容明文密码和BCrypt加密密码
            boolean passwordMatch = false;
            
            // 检查是否是BCrypt加密密码（以$2a$或$2b$或$2y$开头）
            if (storedPassword != null && storedPassword.startsWith("$2")) {
                // BCrypt加密密码，使用密码匹配器验证
                passwordMatch = passwordEncoder.matches(rawPassword, storedPassword);
            } else {
                // 明文密码，直接比较
                passwordMatch = rawPassword.equals(storedPassword);
                
                // 如果明文密码匹配，自动升级为BCrypt加密
                if (passwordMatch) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setUpdateTime(LocalDateTime.now());
                    userMapper.updateById(user);
                }
            }
            
            if (passwordMatch) {
                // 更新最后登录时间
                user.setLastLoginTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(user);
                return user;
            }
        }
        return null;
    }
    // 根据用户名获取用户ID
    public Long getUserIdByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        return user != null ? user.getUserId() : null;
    }

    // 根据用户名获取用户信息
    public User getUserByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
    public boolean updateUserInfo(Long userId, User updatedUser) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新允许修改的字段
        if (updatedUser.getEmail() != null) {
            // 检查邮箱是否被其他用户使用
            User userWithSameEmail = userMapper.selectOne(new QueryWrapper<User>()
                    .eq("email", updatedUser.getEmail())
                    .ne("user_id", userId));
            if (userWithSameEmail != null) {
                throw new RuntimeException("邮箱已被其他用户使用");
            }
            existingUser.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getPhone() != null) {
            existingUser.setPhone(updatedUser.getPhone());
        }

        if (updatedUser.getRealName() != null) {
            existingUser.setRealName(updatedUser.getRealName());
        }

        existingUser.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(existingUser);
        return result > 0;
    }
    /**
     * 修改密码方法
     * @param userId 用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @return 修改成功返回true，失败返回false
     * @throws RuntimeException 用户不存在或原密码错误时抛出异常
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码（使用BCrypt匹配器）
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 加密新密码并更新
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        return result > 0;
    }
}

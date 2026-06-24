package com.example.ticket.controller;

import com.example.ticket.entity.Admin;
import com.example.ticket.service.AdminService;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员Controller
 * 负责处理管理员端的认证相关请求（登录、登出、个人信息）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 管理员登录
     * POST /api/admin/login
     * 
     * @param loginRequest 登录请求体（包含username和password）
     * @param session HTTP会话
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpSession session) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        Admin admin = adminService.login(username, password);

        if (admin != null) {
            // 设置session
            session.setAttribute("admin_id", admin.getAdminId());
            session.setAttribute("admin_username", admin.getUsername());
            session.setAttribute("admin_role", admin.getRole());
            session.setAttribute("role", "ADMIN"); // 用于权限判断

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("admin_info", Map.of(
                    "adminId", admin.getAdminId(),
                    "username", admin.getUsername(),
                    "role", admin.getRole(),
                    "realName", admin.getRealName() != null ? admin.getRealName() : ""
            ));
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * 管理员登出
     * POST /api/admin/logout
     * 
     * @param session HTTP会话
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true, "message", "退出登录成功"));
    }

    /**
     * 获取管理员个人信息
     * GET /api/admin/profile
     * 
     * @param session HTTP会话
     * @return 管理员信息
     */
    /**
     * 获取管理员个人信息
     * GET /api/admin/profile
     * 
     * @param session HTTP会话
     * @return 管理员信息
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        // 只检查 admin_id 是否存在，不依赖 role 属性
        // 这样可以允许管理员和用户同时登录（各自独立的登录状态）
        Long adminId = (Long) session.getAttribute("admin_id");
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "管理员未登录"));
        }

        try {
            Admin admin = adminService.getAdminById(adminId);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "管理员不存在"));
            }

            // 返回管理员信息（排除敏感信息）
            Map<String, Object> adminInfo = new HashMap<>();
            adminInfo.put("adminId", admin.getAdminId());
            adminInfo.put("username", admin.getUsername());
            adminInfo.put("realName", admin.getRealName());
            adminInfo.put("role", admin.getRole());
            adminInfo.put("email", admin.getEmail());
            adminInfo.put("phone", admin.getPhone());
            adminInfo.put("status", admin.getStatus());
            adminInfo.put("lastLoginTime", admin.getLastLoginTime());
            adminInfo.put("createTime", admin.getCreateTime());

            return ResponseEntity.ok(Map.of("success", true, "admin", adminInfo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "获取管理员信息失败"));
        }
    }
}

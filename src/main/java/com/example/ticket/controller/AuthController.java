package com.example.ticket.controller;

import com.example.ticket.entity.User;
import com.example.ticket.service.UserService;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    // --- 注册 POST /api/auth/register ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            userService.register(user);
            response.put("success", true);
            response.put("message", "注册成功！");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // --- 登录 POST /api/auth/login ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpSession session) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        User user = userService.login(username, password);

        if (user != null) {
            // 设置session
            session.setAttribute("user_id", user.getUserId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("user_info", Map.of(
                    "userId", user.getUserId(),
                    "username", user.getUsername(),
                    "role", user.getRole()
            ));
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户名或密码错误。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // --- 登出 POST /api/auth/logout ---
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true, "message", "退出登录成功。"));
    }
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "用户未登录"));
        }

        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "用户不存在"));
            }

            // 返回用户信息（排除敏感信息）
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone());
            userInfo.put("realName", user.getRealName());
            userInfo.put("role", user.getRole());
            userInfo.put("registerTime", user.getRegisterTime());
            userInfo.put("lastLoginTime", user.getLastLoginTime());

            return ResponseEntity.ok(Map.of("success", true, "user", userInfo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "获取用户信息失败"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> updateRequest, HttpSession session) {
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "用户未登录"));
        }

        try {
            User updatedUser = new User();
            updatedUser.setEmail((String) updateRequest.get("email"));
            updatedUser.setPhone((String) updateRequest.get("phone"));
            updatedUser.setRealName((String) updateRequest.get("realName"));

            boolean result = userService.updateUserInfo(userId, updatedUser);

            if (result) {
                return ResponseEntity.ok(Map.of("success", true, "message", "个人信息更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "更新失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "更新失败"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordRequest, HttpSession session) {
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "用户未登录"));
        }

        try {
            String oldPassword = passwordRequest.get("oldPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "密码不能为空"));
            }

            boolean result = userService.changePassword(userId, oldPassword, newPassword);

            if (result) {
                return ResponseEntity.ok(Map.of("success", true, "message", "密码修改成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "密码修改失败"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "密码修改失败"));
        }
    }
}

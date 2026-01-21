package com.ai.haha.springaistudyweb.controller.moyoo;

import com.ai.haha.springaistudyservice.service.moyoo.dto.UserLoginDTO;
import com.ai.haha.springaistudyservice.service.moyoo.dto.UserRegisterDTO;
import com.ai.haha.springaistudyservice.service.moyoo.entity.User;
import com.ai.haha.springaistudyservice.service.moyoo.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/moyoo/user")
public class UserController {
    
    @Resource
    private UserService userService;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody UserRegisterDTO registerDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.register(registerDTO);
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserLoginDTO loginDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.login(loginDTO);
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findById(userId);
            response.put("success", true);
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            userService.logout(userId);
            response.put("success", true);
            response.put("message", "退出登录成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 强制退出登录（用于登录态过期等情况）
     */
    @PostMapping("/force-logout")
    public ResponseEntity<Map<String, Object>> forceLogout(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            userService.forceLogout(userId);
            response.put("success", true);
            response.put("message", "强制退出登录成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}


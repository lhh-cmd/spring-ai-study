package com.ai.haha.springaistudyweb.controller.cvm;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmUserLoginDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmUserRegisterDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmUserService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * CVM用户控制器
 */
@RestController
@RequestMapping("/api/cvm/user")
public class CvmUserController {

    @Resource
    private CvmUserService cvmUserService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody CvmUserRegisterDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            CvmUser user = cvmUserService.register(dto);
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
    public ResponseEntity<Map<String, Object>> login(@RequestBody CvmUserLoginDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            CvmUser user = cvmUserService.login(dto);
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
}

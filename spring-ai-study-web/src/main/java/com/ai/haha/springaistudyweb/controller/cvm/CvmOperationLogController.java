package com.ai.haha.springaistudyweb.controller.cvm;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmOperationLogView;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CVM操作日志控制器
 */
@RestController
@RequestMapping("/api/cvm/log")
public class CvmOperationLogController {

    @Resource
    private CvmOperationLogService cvmOperationLogService;

    /**
     * 项目操作日志
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) Long projectId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmOperationLogView> logs = projectId != null
                    ? cvmOperationLogService.listByProject(projectId)
                    : cvmOperationLogService.listAll();
            response.put("success", true);
            response.put("data", logs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

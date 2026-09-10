package com.ai.haha.springaistudyweb.controller.cvm;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmRequirementCreateDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmRequirement;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmRequirementService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CVM需求控制器
 */
@RestController
@RequestMapping("/api/cvm/requirement")
public class CvmRequirementController {

    @Resource
    private CvmRequirementService cvmRequirementService;

    /**
     * 创建需求（新建/拉取分支，进入 dev 待合并列表）
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody CvmRequirementCreateDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            CvmRequirement requirement = cvmRequirementService.createRequirement(dto);
            response.put("success", true);
            response.put("message", "需求创建成功，分支已进入开发环境待合并列表");
            response.put("data", requirement);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 需求列表：按项目（projectId）或按创建人（userId），均可不传
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) Long projectId,
                                                    @RequestParam(required = false) Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmRequirement> requirements;
            if (userId != null) {
                requirements = cvmRequirementService.listByUser(userId);
            } else if (projectId != null) {
                requirements = cvmRequirementService.listByProject(projectId);
            } else {
                requirements = cvmRequirementService.listByProject(null);
            }
            response.put("success", true);
            response.put("data", requirements);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

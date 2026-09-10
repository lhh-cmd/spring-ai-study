package com.ai.haha.springaistudyweb.controller.cvm;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmProjectCreateDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmProjectDetailDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmProjectService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CVM项目控制器
 */
@RestController
@RequestMapping("/api/cvm/project")
public class CvmProjectController {

    @Resource
    private CvmProjectService cvmProjectService;

    /**
     * 注册项目（自动初始化 dev/test/preview/release 环境）
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(@RequestBody CvmProjectCreateDTO dto,
                                                      @RequestParam Long creatorUserId) {
        Map<String, Object> response = new HashMap<>();
        try {
            CvmProject project = cvmProjectService.createProject(dto, creatorUserId);
            response.put("success", true);
            response.put("message", "项目注册成功，已初始化开发/测试/预发/正式 4 个环境");
            response.put("data", project);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 项目列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmProject> projects = cvmProjectService.listProjects();
            response.put("success", true);
            response.put("data", projects);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 项目详情（含环境、需求）
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> detail(@RequestParam Long projectId) {
        Map<String, Object> response = new HashMap<>();
        try {
            CvmProjectDetailDTO detail = cvmProjectService.getProjectDetail(projectId);
            response.put("success", true);
            response.put("data", detail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

package com.ai.haha.springaistudyweb.controller.cvm;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmMergeRecordView;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmMergeService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CVM合并控制器（合并/冲突解决/进入下一环境/发布/合master）
 */
@RestController
@RequestMapping("/api/cvm/merge")
public class CvmMergeController {

    @Resource
    private CvmMergeService cvmMergeService;

    /**
     * 合并分支到当前环境公共分支
     */
    @PostMapping("/merge")
    public ResponseEntity<Map<String, Object>> merge(@RequestParam Long mergeId, @RequestParam Long operatorUserId) {
        Map<String, Object> response = new HashMap<>();
        try {
            CvmMergeRecordView view = cvmMergeService.merge(mergeId, operatorUserId);
            boolean conflict = "CONFLICT".equals(view.getStatus());
            response.put("success", true);
            response.put("conflict", conflict);
            response.put("message", conflict ? "合并出现冲突，请按解决步骤处理" : "合并成功");
            response.put("data", view);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 退出集成：将已合入公共分支的分支退出，回到待集成列表
     */
    @PostMapping("/exit-integration")
    public ResponseEntity<Map<String, Object>> exitIntegration(@RequestParam Long mergeId, @RequestParam Long operatorUserId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String message = cvmMergeService.exitIntegration(mergeId, operatorUserId);
            response.put("success", true);
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 解决冲突并重新合并
     */
    @PostMapping("/resolve-conflict")
    public ResponseEntity<Map<String, Object>> resolveConflict(@RequestParam Long mergeId, @RequestParam Long operatorUserId) {
        Map<String, Object> response = new HashMap<>();
        try {
            CvmMergeRecordView view = cvmMergeService.resolveConflict(mergeId, operatorUserId);
            response.put("success", true);
            response.put("message", "冲突已解决并合并成功");
            response.put("data", view);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 进入正式环境：预发环境已集成的全部分支（通过CR）合入 release
     */
    @PostMapping("/enter-release")
    public ResponseEntity<Map<String, Object>> enterRelease(@RequestParam Long projectId, @RequestParam Long operatorUserId) {
        Map<String, Object> response = new HashMap<>();
        try {
            int entered = cvmMergeService.enterRelease(projectId, operatorUserId);
            response.put("success", true);
            response.put("message", entered + " 个分支已进入正式环境，合并到 release 分支");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 合并master：正式环境上线分支合入master，逻辑删除上线分支与需求并重建环境分支
     */
    @PostMapping("/merge-master")
    public ResponseEntity<Map<String, Object>> mergeMaster(@RequestParam Long projectId, @RequestParam Long operatorUserId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String message = cvmMergeService.mergeMaster(projectId, operatorUserId);
            response.put("success", true);
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 某环境的合并记录列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam Long projectId, @RequestParam String envCode) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmMergeRecordView> list = cvmMergeService.listByEnv(projectId, envCode);
            response.put("success", true);
            response.put("data", list);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 项目所有环境的合并记录
     */
    @GetMapping("/list-all")
    public ResponseEntity<Map<String, Object>> listAll(@RequestParam Long projectId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmMergeRecordView> list = cvmMergeService.listAllByProject(projectId);
            response.put("success", true);
            response.put("data", list);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

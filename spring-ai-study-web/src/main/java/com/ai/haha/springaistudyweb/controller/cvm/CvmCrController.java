package com.ai.haha.springaistudyweb.controller.cvm;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrAuditDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrItemDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrSubmitDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmCrService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CVM代码评审(CR)控制器
 */
@RestController
@RequestMapping("/api/cvm/cr")
public class CvmCrController {

    @Resource
    private CvmCrService cvmCrService;

    /**
     * 发起CR：提交给指定评审人审核
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody CvmCrSubmitDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            cvmCrService.submit(dto);
            response.put("success", true);
            response.put("message", "CR已发起，等待审核人审核");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * CR 审核（PASS/REJECT）
     */
    @PostMapping("/audit")
    public ResponseEntity<Map<String, Object>> audit(@RequestBody CvmCrAuditDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            cvmCrService.audit(dto);
            response.put("success", true);
            response.put("message", "审核完成");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 我发起的CR列表
     */
    @GetMapping("/my-submitted")
    public ResponseEntity<Map<String, Object>> mySubmitted(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmCrItemDTO> list = cvmCrService.listSubmitted(userId);
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
     * 需要我审核的CR列表
     */
    @GetMapping("/my-pending")
    public ResponseEntity<Map<String, Object>> myPending(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmCrItemDTO> list = cvmCrService.listPendingByReviewer(userId);
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
     * 项目可指定的CR评审人列表
     */
    @GetMapping("/reviewers")
    public ResponseEntity<Map<String, Object>> reviewers(@RequestParam Long projectId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmUser> list = cvmCrService.listReviewers(projectId);
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
     * 合并记录的CR记录列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam Long mergeId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CvmCrRecord> list = cvmCrService.listByMerge(mergeId);
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

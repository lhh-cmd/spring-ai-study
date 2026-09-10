package com.ai.haha.springaistudyweb.controller.cvm;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrAuditDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;
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

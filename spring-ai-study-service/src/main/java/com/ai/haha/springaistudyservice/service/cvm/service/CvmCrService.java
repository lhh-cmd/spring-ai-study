package com.ai.haha.springaistudyservice.service.cvm.service;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrAuditDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;

import java.util.List;

/**
 * CVM代码评审(CR)服务接口
 */
public interface CvmCrService {

    /**
     * CR 审核（PASS/REJECT）
     */
    void audit(CvmCrAuditDTO dto);

    /**
     * 判断指定合并记录是否已通过CR
     */
    boolean hasPassed(Long mergeId);

    /**
     * 查询合并记录的所有CR记录
     */
    List<CvmCrRecord> listByMerge(Long mergeId);
}

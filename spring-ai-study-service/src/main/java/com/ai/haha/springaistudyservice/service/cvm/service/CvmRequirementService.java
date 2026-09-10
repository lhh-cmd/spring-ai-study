package com.ai.haha.springaistudyservice.service.cvm.service;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmRequirementCreateDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmRequirement;

import java.util.List;

/**
 * CVM需求服务接口
 */
public interface CvmRequirementService {

    /**
     * 创建需求（新建/拉取分支，并生成 dev 环境待合并记录）
     */
    CvmRequirement createRequirement(CvmRequirementCreateDTO dto);

    /**
     * 根据项目查询需求列表
     */
    List<CvmRequirement> listByProject(Long projectId);

    /**
     * 根据创建人查询需求列表
     */
    List<CvmRequirement> listByUser(Long creatorUserId);

    /**
     * 根据需求ID查询
     */
    CvmRequirement getById(Long requirementId);
}

package com.ai.haha.springaistudyservice.service.cvm.dto;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmEnvironment;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmRequirement;
import lombok.Data;

import java.util.List;

/**
 * CVM项目详情DTO
 */
@Data
public class CvmProjectDetailDTO {

    /**
     * 项目信息
     */
    private CvmProject project;

    /**
     * 环境列表（dev/test/preview/release）
     */
    private List<CvmEnvironment> environments;

    /**
     * 需求列表
     */
    private List<CvmRequirement> requirements;
}

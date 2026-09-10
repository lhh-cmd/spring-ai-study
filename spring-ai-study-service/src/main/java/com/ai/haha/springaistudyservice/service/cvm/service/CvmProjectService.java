package com.ai.haha.springaistudyservice.service.cvm.service;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmProjectCreateDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmProjectDetailDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;

import java.util.List;

/**
 * CVM项目服务接口
 */
public interface CvmProjectService {

    /**
     * 注册项目（并初始化 dev/test/preview/release 4 个标准环境）
     */
    CvmProject createProject(CvmProjectCreateDTO dto, Long creatorUserId);

    /**
     * 项目列表
     */
    List<CvmProject> listProjects();

    /**
     * 根据项目ID查询
     */
    CvmProject getById(Long projectId);

    /**
     * 项目详情（含环境、需求）
     */
    CvmProjectDetailDTO getProjectDetail(Long projectId);
}

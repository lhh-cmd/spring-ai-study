package com.ai.haha.springaistudyservice.service.cvm.service;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmEnvironment;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;

import java.util.List;

/**
 * CVM环境服务接口
 */
public interface CvmEnvironmentService {

    /**
     * 初始化项目的 dev/test/preview/release 4 个标准环境，并确保各环境公共分支存在
     */
    void initStandardEnvironments(CvmProject project);

    /**
     * 根据项目查询环境列表（按排序）
     */
    List<CvmEnvironment> listByProject(Long projectId);

    /**
     * 根据项目和env_code查询环境
     */
    CvmEnvironment getByProjectAndCode(Long projectId, String envCode);
}

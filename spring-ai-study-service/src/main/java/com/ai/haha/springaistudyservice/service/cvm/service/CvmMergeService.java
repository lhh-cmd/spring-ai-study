package com.ai.haha.springaistudyservice.service.cvm.service;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmMergeRecordView;

import java.util.List;

/**
 * CVM合并服务接口（核心流程：合并/冲突解决/进入下一环境/发布/合master）
 */
public interface CvmMergeService {

    /**
     * 点击合并：将分支合并到目标环境公共分支
     *
     * @return 合并后的记录视图（冲突时 status 为 CONFLICT）
     */
    CvmMergeRecordView merge(Long mergeId, Long operatorUserId);

    /**
     * 解决冲突并重新合并
     */
    CvmMergeRecordView resolveConflict(Long mergeId, Long operatorUserId);

    /**
     * 当前环境验证完成后，将分支放入下一环境待合并列表
     */
    void nextEnv(Long requirementId, Long operatorUserId);

    /**
     * 发布到线上（正式环境）
     */
    void publish(Long requirementId, Long operatorUserId);

    /**
     * 合并master：分支合入master，其他环境退出已合并分支、基于master更新dev、已合并分支重新合并
     */
    void mergeToMaster(Long requirementId, Long operatorUserId);

    /**
     * 查询项目下某环境的所有合并记录（待合并/冲突/已合并）
     */
    List<CvmMergeRecordView> listByEnv(Long projectId, String envCode);

    /**
     * 查询项目下所有环境的合并记录
     */
    List<CvmMergeRecordView> listAllByProject(Long projectId);
}

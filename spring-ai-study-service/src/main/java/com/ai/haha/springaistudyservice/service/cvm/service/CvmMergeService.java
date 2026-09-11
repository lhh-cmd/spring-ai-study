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
     * 退出集成：将已合入当前环境公共分支的分支退出，回到待集成列表。
     * 目标环境分支先恢复与 master 一致，再将其余仍已集成分支按原顺序重新合并，遇冲突停住待解决后继续。
     *
     * @return 退出结果描述（含重建重合并情况与冲突提示）
     */
    String exitIntegration(Long mergeId, Long operatorUserId);

    /**
     * 进入正式环境：将预发环境已集成的全部分支（均需通过CR）合入 release 分支，加入正式环境合并列表
     *
     * @return 本次进入正式环境的分支数量
     */
    int enterRelease(Long projectId, Long operatorUserId);

    /**
     * 合并 master：将正式环境上线分支合入 master，逻辑删除上线分支与需求，
     * 并重建 dev/test/preview/release 环境分支（对齐 master 后重新合并仍存续的已集成分支，遇冲突停住待解决后继续）
     *
     * @return 操作结果描述（含各环境重建冲突情况）
     */
    String mergeMaster(Long projectId, Long operatorUserId);

    /**
     * 查询项目下某环境的所有合并记录（待合并/冲突/已合并）
     */
    List<CvmMergeRecordView> listByEnv(Long projectId, String envCode);

    /**
     * 查询项目下所有环境的合并记录
     */
    List<CvmMergeRecordView> listAllByProject(Long projectId);
}

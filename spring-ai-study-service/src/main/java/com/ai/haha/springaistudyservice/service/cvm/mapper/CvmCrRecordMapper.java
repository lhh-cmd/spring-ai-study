package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrItemDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CVM代码评审(CR)记录Mapper
 */
@Mapper
public interface CvmCrRecordMapper extends BaseMapper<CvmCrRecord> {

    /**
     * 根据关联合并记录ID查找所有CR记录
     */
    @Select("SELECT * FROM cvm_cr_record WHERE merge_id = #{mergeId} AND deleted = 0 ORDER BY create_time ASC")
    List<CvmCrRecord> selectByMergeId(Long mergeId);

    /**
     * 根据需求ID查找该分支的所有CR记录（CR按分支维度统一判断，跨环境共享）
     */
    @Select("SELECT * FROM cvm_cr_record WHERE requirement_id = #{requirementId} AND deleted = 0 " +
            "ORDER BY create_time ASC, id ASC")
    List<CvmCrRecord> selectByRequirementId(Long requirementId);

    /**
     * 判断指定合并记录是否已通过CR（最新一次审核为PASS才算通过，被后续REJECT覆盖则不算）
     */
    @Select("SELECT COUNT(*) > 0 FROM cvm_cr_record cr1 " +
            "WHERE cr1.merge_id = #{mergeId} AND cr1.cr_status = 'PASS' AND cr1.deleted = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM cvm_cr_record cr2 WHERE cr2.merge_id = #{mergeId} AND cr2.deleted = 0 " +
            "  AND cr2.id > cr1.id AND cr2.cr_status = 'REJECT')")
    boolean hasPassed(Long mergeId);

    /**
     * 判断该分支（需求）是否已通过CR（跨环境统一判断）
     */
    @Select("SELECT COUNT(*) > 0 FROM cvm_cr_record cr1 " +
            "WHERE cr1.requirement_id = #{requirementId} AND cr1.cr_status = 'PASS' AND cr1.deleted = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM cvm_cr_record cr2 WHERE cr2.requirement_id = #{requirementId} AND cr2.deleted = 0 " +
            "  AND cr2.id > cr1.id AND cr2.cr_status = 'REJECT')")
    boolean hasPassedByRequirement(Long requirementId);

    /**
     * 判断指定合并记录是否已有待审核的CR
     */
    @Select("SELECT COUNT(*) > 0 FROM cvm_cr_record WHERE merge_id = #{mergeId} AND cr_status = 'PENDING' AND deleted = 0")
    boolean hasPending(Long mergeId);

    /**
     * 判断该分支（需求）是否已有待审核的CR
     */
    @Select("SELECT COUNT(*) > 0 FROM cvm_cr_record WHERE requirement_id = #{requirementId} " +
            "AND cr_status = 'PENDING' AND deleted = 0")
    boolean hasPendingByRequirement(Long requirementId);

    /**
     * 查询合并记录上指定评审人的待审核CR
     */
    @Select("SELECT * FROM cvm_cr_record WHERE merge_id = #{mergeId} AND reviewer_user_id = #{reviewerUserId} " +
            "AND cr_status = 'PENDING' AND deleted = 0 ORDER BY id DESC LIMIT 1")
    CvmCrRecord selectPendingByMergeAndReviewer(Long mergeId, Long reviewerUserId);

    /**
     * 我发起的CR列表（关联分支/需求/用户信息）
     */
    @Select("SELECT cr.cr_id, cr.merge_id, cr.project_id, cr.requirement_id, " +
            "mr.branch_name, mr.requirement_name, mr.target_env, " +
            "cr.submitter_user_id, su.nickname AS submitter_name, " +
            "cr.reviewer_user_id, ru.nickname AS reviewer_name, " +
            "cr.cr_status, cr.cr_comment, cr.create_time, cr.cr_time " +
            "FROM cvm_cr_record cr " +
            "JOIN cvm_merge_record mr ON mr.merge_id = cr.merge_id AND mr.deleted = 0 " +
            "LEFT JOIN cvm_user su ON su.user_id = cr.submitter_user_id " +
            "LEFT JOIN cvm_user ru ON ru.user_id = cr.reviewer_user_id " +
            "WHERE cr.submitter_user_id = #{userId} AND cr.deleted = 0 " +
            "ORDER BY cr.create_time DESC")
    List<CvmCrItemDTO> selectSubmittedByUserId(Long userId);

    /**
     * 需要我审核的CR列表（关联分支/需求/用户信息）
     */
    @Select("SELECT cr.cr_id, cr.merge_id, cr.project_id, cr.requirement_id, " +
            "mr.branch_name, mr.requirement_name, mr.target_env, " +
            "cr.submitter_user_id, su.nickname AS submitter_name, " +
            "cr.reviewer_user_id, ru.nickname AS reviewer_name, " +
            "cr.cr_status, cr.cr_comment, cr.create_time, cr.cr_time " +
            "FROM cvm_cr_record cr " +
            "JOIN cvm_merge_record mr ON mr.merge_id = cr.merge_id AND mr.deleted = 0 " +
            "LEFT JOIN cvm_user su ON su.user_id = cr.submitter_user_id " +
            "LEFT JOIN cvm_user ru ON ru.user_id = cr.reviewer_user_id " +
            "WHERE cr.reviewer_user_id = #{userId} AND cr.cr_status = 'PENDING' AND cr.deleted = 0 " +
            "ORDER BY cr.create_time DESC")
    List<CvmCrItemDTO> selectPendingByReviewerUserId(Long userId);
}
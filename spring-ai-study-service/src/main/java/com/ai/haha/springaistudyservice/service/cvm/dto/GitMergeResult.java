package com.ai.haha.springaistudyservice.service.cvm.dto;

import lombok.Data;

import java.util.List;

/**
 * Git合并结果DTO
 */
@Data
public class GitMergeResult {

    /**
     * 是否合并成功
     */
    private boolean success;

    /**
     * 冲突文件列表
     */
    private List<String> conflictFiles;

    /**
     * 结果描述
     */
    private String message;

    /**
     * 合并后的提交ID
     */
    private String commitId;

    public GitMergeResult() {
    }

    public GitMergeResult(boolean success, List<String> conflictFiles, String message, String commitId) {
        this.success = success;
        this.conflictFiles = conflictFiles;
        this.message = message;
        this.commitId = commitId;
    }

    public static GitMergeResult ok(String commitId, String message) {
        return new GitMergeResult(true, null, message, commitId);
    }

    public static GitMergeResult conflict(List<String> conflictFiles, String message) {
        return new GitMergeResult(false, conflictFiles, message, null);
    }

    public static GitMergeResult fail(String message) {
        return new GitMergeResult(false, null, message, null);
    }
}
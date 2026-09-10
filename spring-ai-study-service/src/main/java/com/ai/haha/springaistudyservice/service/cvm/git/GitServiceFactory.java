package com.ai.haha.springaistudyservice.service.cvm.git;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Git 服务工厂。
 *
 * <p>项目配置了 git 地址时使用 {@link JGitGitService} 真实实现；
 * 未配置（git地址/权限暂时留空）时使用 {@link SimulatedGitService} 模拟实现。</p>
 */
@Component
public class GitServiceFactory {

    @Resource
    private SimulatedGitService simulatedGitService;

    @Resource
    private JGitGitService jGitGitService;

    public GitOperationService getService(CvmProject project) {
        if (project != null && StringUtils.hasText(project.getGitUrl())) {
            return jGitGitService;
        }
        return simulatedGitService;
    }
}

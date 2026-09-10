package com.ai.haha.springaistudyservice.service.cvm.git;

/**
 * 冲突解决步骤生成工具类
 */
public class GitConflictGuide {

    /**
     * 生成给用户展示的中文冲突解决步骤
     *
     * <p>约定：冲突解决在【目标分支】上进行——把当前冲突分支合并到目标分支（如 dev），
     * 在目标分支上逐行解决并提交、推送，避免把 dev 上他人代码合进自己的分支。</p>
     */
    public static String buildResolveSteps(String sourceBranch, String targetBranch) {
        return String.join("\n",
                "1. 打开本地终端，进入你的项目目录",
                "2. 先拉取远程更新，再切换到目标分支并同步到最新：git fetch origin && git checkout " + targetBranch + " && git pull origin " + targetBranch,
                "3. 将你的当前分支合并到目标分支：git merge " + sourceBranch,
                "4. 系统会提示出现冲突，使用 IDE（IDEA / VSCode）打开冲突文件，逐行解决（保留双方修改或与相关同学协商取舍）",
                "5. 解决完成后，将文件标记为已解决：git add .",
                "6. 在目标分支上提交解决结果：git commit -m \"merge " + sourceBranch + " into " + targetBranch + ": resolve conflict\"",
                "7. 将目标分支推送到远程：git push origin " + targetBranch,
                "8. 回到本系统页面，点击【冲突已解决】按钮，系统会检测冲突是否已解决并自动完成合并");
    }
}
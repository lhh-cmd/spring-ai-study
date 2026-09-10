package com.ai.haha.springaistudyservice.service.cvm.git;

/**
 * 冲突解决步骤生成工具类
 */
public class GitConflictGuide {

    /**
     * 生成给用户展示的中文冲突解决步骤
     */
    public static String buildResolveSteps(String sourceBranch, String targetBranch) {
        return String.join("\n",
                "1. 打开本地终端，进入你的项目目录",
                "2. 拉取远程最新代码：git fetch origin",
                "3. 切换到你的开发分支：git checkout " + sourceBranch,
                "4. 将最新的目标分支合并进来：git merge origin/" + targetBranch,
                "5. 系统会提示出现冲突，使用 IDE（IDEA / VSCode）打开冲突文件，逐行解决（保留双方修改或与相关同学协商取舍）",
                "6. 解决完成后，将文件标记为已解决：git add .",
                "7. 提交解决结果：git commit -m \"resolve conflict with " + targetBranch + "\"",
                "8. 推送远程分支：git push origin " + sourceBranch,
                "9. 回到本系统页面，点击【解决冲突并合并】按钮完成合并");
    }
}
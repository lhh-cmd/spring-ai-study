# 代码版本管理系统（CVM）实现方案

## 一、目标
在现有 spring-ai-study 多模块项目上新增一套「代码版本管理系统」，用于**部门日常真实使用**。管理完整分支工作流：项目注册 → 环境初始化 → 需求创建 → 分支跨环境合并（dev/test/preview/release）→ release 前 CR 拦截 → 发布 → 合 master 后环境重置。

Git 操作以**真实 JGit 实现为主**；git 地址未配置时自动降级为**模拟实现**，保证系统在接入真实仓库前也能完整跑通试用。需要前端管理页面。

## 二、技术方案
- 沿用现有技术栈：Spring Boot 3.4.1 + Java 17 + MyBatis-Plus + MySQL（已装）+ 静态前端页面
- 新增依赖：`org.eclipse.jgit:org.eclipse.jgit`（6.10.0，真实 Git 操作）
- 沿用现有 moyoo 分层：service 模块放 entity/mapper/enum/service/git，web 模块放 controller + 静态页面
- Git 抽象：`GitOperationService` 接口 + `JGitGitService`（真实实现）+ `SimulatedGitService`（降级实现）+ `GitServiceFactory`（按项目 git 地址自动选择）
- 前端：新增 `cvm.html` + `js/cvm.js` + `css/cvm.css`，与现有摸鱼页面并列，互相加入口链接

## 三、数据库设计（新增 sql/cvm-schema.sql，共 8 张表，沿用现有 moyoo 库）

| 表 | 说明 | 关键字段 |
|---|---|---|
| cvm_user | 用户 | user_id(雪花), username(唯一), password, nickname |
| cvm_project | 项目 | project_code(用户填写的项目ID), project_name, project_desc, git_url(可空), git_username, git_token, creator_user_id |
| cvm_environment | 环境 | project_id, env_code(DEV/TEST/PREVIEW/RELEASE), env_name, branch_name, sort_order；唯一(project_id, env_code) |
| cvm_branch | 分支登记 | project_id, branch_name, branch_type(ENV/REQUIREMENT/MASTER), base_branch, source(CREATED/PULLED)；唯一(project_id, branch_name) |
| cvm_requirement | 需求 | requirement_name, requirement_url, branch_name, branch_source(NEW/REMOTE), creator_user_id, current_env, status |
| cvm_merge_record | 合并记录/待合并列表（核心） | requirement_id, requirement_name, branch_name, user_id, target_env, target_branch, status(PENDING/CONFLICT/MERGED/REJECTED), conflict_files(JSON), conflict_detail, resolve_steps, merge_commit, cr_required, merge_time |
| cvm_cr_record | CR 审核 | requirement_id, merge_id, reviewer_user_id, cr_status(PASS/REJECT), cr_comment |
| cvm_operation_log | 操作日志 | project_id, requirement_id, merge_id, operator_user_id, action, detail |

所有表均含 id(自增主键) + 业务ID(雪花, `@JsonFormat(STRING)` 防 JS 精度丢失) + create_time/update_time。

## 四、核心流程

### 1. 项目注册
填项目名称、项目描述、项目ID、git 地址（可空）、git 账号/令牌（可空）。注册成功后自动初始化 dev/test/preview/release 4 个环境：每个环境校验对应公共分支，远程存在则不创建，否则创建；同时登记 master 分支。

### 2. 创建需求
填需求名称、需求地址、分支名称，选择分支来源（新建 / 从远程已有拉取）。创建后：新建或拉取分支；为 DEV 环境生成一条待合并记录（PENDING）；需求进入开发中状态，出现在 dev 待合并列表。

### 3. dev 合并 + 冲突处理
点合并 → `mergeBranch(dev, 分支)`：
- 成功 → 记录 MERGED + merge_commit + 操作日志；dev「已合并列表」展示分支、需求、合并人、时间（多人可见）
- 冲突 → 记录 CONFLICT + 冲突文件/详情，自动生成中文解决步骤（fetch → checkout → merge origin/dev → 本地解决冲突 → add/commit/push → 回到系统重新合并）
- 解决后点「解决冲突并合并」→ 重新 merge → MERGED

### 4. 进入下一环境
dev 验证通过点「进入下一环境」→ 为 TEST 生成新待合并记录，current_env→TEST。test 流程与 dev 一致，以此类推；PREVIEW 验证后进入 RELEASE。

### 5. RELEASE 环境（CR 拦截）
提交到 RELEASE 的待合并记录 cr_required=true，**必须存在 cr_status=PASS 的审核记录才能合并**，否则提示「请先完成代码评审」。页面提供 CR 通过/驳回操作。

### 6. 发布
release 合并验证后点「发布」→ 状态 PUBLISHED（实际部署走外部流程，系统记录状态）。

### 7. 合 master + 环境重置
release 环境点「合并master」：
- 分支合并到 master 并推送
- 其他环境将已合并分支退出（从各环境分支移除）
- 基于 master 重置 dev
- 将本次合入 master 的分支重新合并到更新后的 dev
- 需求状态 → MERGED_MASTER（终态）

## 五、Git 抽象层
- `GitOperationService`：checkBranchExists / createBranch / pullBranch / mergeBranch→MergeResult{success, conflictFiles, message, commitId} / updateBranchFrom / getResolveSteps
- `SimulatedGitService`（git_url 为空）：分支存在性查 cvm_branch；冲突启发式 = 分支创建后目标分支已被其他分支合并过 → 判冲突；返回模拟 commitId
- `JGitGitService`（git_url 非空）：RepoManager 在本地工作区（默认 ./cvm-workspace/{projectId}）克隆/打开仓库，按项目加锁防并发；真实 branchCreate/merge/reset 并推送；冲突时解析 conflicting 文件清单
- `GitServiceFactory`：按 project.gitUrl 选择实现

## 六、REST API
- `/api/cvm/user/register | login`
- `/api/cvm/project/create | list | detail`
- `/api/cvm/requirement/create | list`
- `/api/cvm/merge/list?envCode= | merge | resolve-conflict | next-env`
- `/api/cvm/cr/audit`
- `/api/cvm/requirement/publish | merge-master`
- `/api/cvm/log/list`

## 七、前端页面（cvm.html）
1. 登录/注册
2. 项目列表 + 新建项目表单
3. 项目详情：环境页签（dev/test/preview/release）
   - 需求创建表单 + 需求列表
   - 当前环境待合并列表：合并 / 解决冲突 / 冲突详情与解决步骤 / CR / 进入下一环境
   - 已合并列表：分支、需求、合并人、时间
   - RELEASE 页签：CR 审核区 + 发布 + 合并master
4. 操作日志

## 八、配置改动
- `application.properties` 新增：`cvm.workspace.dir`、`cvm.git.username`、`cvm.git.token`
- `MyBatisPlusConfig` 的 `@MapperScan` 扩展为同时扫描 moyoo + cvm 两个 mapper 包
- service/pom 增加 JGit 依赖
- index.html 增加 CVM 入口链接

## 九、实施步骤
1. cvm-schema.sql + 配置 + JGit 依赖 + MapperScan 扩展
2. 枚举 + 8 张表实体 + Mapper
3. Git 抽象层（接口 + 模拟实现 + JGit 实现 + 工厂 + RepoManager）
4. Service 层（用户/项目/环境/需求/合并/CR/日志）
5. Controller 层
6. 前端 cvm.html/js/css + 入口链接
7. 建库建表、编译、启动、接口验证

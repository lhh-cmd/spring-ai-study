// ============ 海兴GitFlow 前端逻辑 ============

const API_BASE = '/api/cvm';

const ENV_CONFIG = {
    DEV:     { name: '开发环境', branch: 'dev',     badge: 'badge-dev' },
    TEST:    { name: '测试环境', branch: 'test',    badge: 'badge-test' },
    PREVIEW: { name: '预发环境', branch: 'preview', badge: 'badge-preview' },
    RELEASE: { name: '正式环境', branch: 'release', badge: 'badge-release' }
};

// 环境 tab 顺序
const ENV_LIST = [
    { code: 'DEV', name: '开发环境', branch: 'dev' },
    { code: 'TEST', name: '测试环境', branch: 'test' },
    { code: 'PREVIEW', name: '预发环境', branch: 'preview' },
    { code: 'RELEASE', name: '正式环境', branch: 'release' }
];

const REQUIREMENT_STATUS = {
    DEVELOPING: '开发中',
    CONFLICT: '冲突待解决',
    MERGED: '已合并',
    RELEASED: '已上线'
};

const MERGE_STATUS = {
    PENDING: '待合并',
    CONFLICT: '冲突待解决',
    MERGED: '已合并',
    REJECTED: '已驳回'
};

let currentUser = null;
let currentProject = null; // 项目详情（含 project/environments/requirements）
let currentEnvTab = 'DEV';
let createReqProjectId = null; // 弹窗创建需求时所属项目ID
let envRecords = []; // 当前环境合并记录缓存（供解决冲突弹窗查找记录）

// ============ 工具函数 ============

function $(id) { return document.getElementById(id); }

function esc(str) {
    if (str === null || str === undefined) return '';
    return String(str).replace(/[&<>"']/g, function (c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
}

function qs(params) {
    return Object.keys(params).map(function (k) {
        return encodeURIComponent(k) + '=' + encodeURIComponent(params[k]);
    }).join('&');
}

function showMsg(text, type) {
    const icon = $('msg-modal-icon');
    const isError = type === 'error';
    icon.className = 'feedback-icon ' + (isError ? 'feedback-icon-error' : 'feedback-icon-success');
    icon.textContent = isError ? '✕' : '✓';
    $('msg-modal-text').textContent = text;
    $('msg-modal').style.display = 'flex';
}

function closeMsgModal() {
    $('msg-modal').style.display = 'none';
}

// 确认弹窗（替代原生 confirm）
let confirmCallback = null;

function confirmDialog(message) {
    $('confirm-modal-text').textContent = message;
    $('confirm-modal').style.display = 'flex';
    return new Promise(function (resolve) { confirmCallback = resolve; });
}

function confirmResult(val) {
    $('confirm-modal').style.display = 'none';
    const cb = confirmCallback;
    confirmCallback = null;
    if (cb) cb(val);
}

// 意见输入弹窗（替代原生 prompt）
let promptCallback = null;

function promptDialog(title) {
    $('prompt-modal-title').textContent = title || '请输入';
    $('prompt-modal-input').value = '';
    $('prompt-modal').style.display = 'flex';
    $('prompt-modal-input').focus();
    return new Promise(function (resolve) { promptCallback = resolve; });
}

function promptResult(val) {
    $('prompt-modal').style.display = 'none';
    const cb = promptCallback;
    promptCallback = null;
    if (cb) cb(val === 'ok' ? $('prompt-modal-input').value.trim() : null);
}

document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    if ($('confirm-modal').style.display === 'flex') confirmResult(false);
    else if ($('prompt-modal').style.display === 'flex') promptResult(null);
    else if ($('msg-modal').style.display === 'flex') closeMsgModal();
    else if ($('conflict-modal').style.display === 'flex') closeConflictModal();
    else if ($('project-modal').style.display === 'flex') closeProjectModal();
    else if ($('req-modal').style.display === 'flex') closeReqModal();
    else if ($('cr-modal').style.display === 'flex') closeCrModal();
});

async function api(path, options) {
    options = options || {};
    const res = await fetch(API_BASE + path, Object.assign({ headers: { 'Content-Type': 'application/json' } }, options));
    let json;
    try { json = await res.json(); } catch (e) { throw new Error('服务异常，请稍后重试'); }
    if (!json.success) { throw new Error(json.message || '请求失败'); }
    return json.data;
}

// ============ 登录 / 注册 ============

document.addEventListener('DOMContentLoaded', function () {
    const saved = localStorage.getItem('cvmUser');
    if (saved) {
        try {
            currentUser = JSON.parse(saved);
            if (currentUser && currentUser.userId) {
                currentUser.userId = String(currentUser.userId);
                enterApp();
                return;
            }
        } catch (e) { /* ignore */ }
        localStorage.removeItem('cvmUser');
    }
    showAuth();
});

function showAuth() {
    $('auth-section').style.display = 'block';
    $('main-section').style.display = 'none';
}

function enterApp() {
    $('auth-section').style.display = 'none';
    $('main-section').style.display = 'block';
    $('top-user').textContent = '👤 ' + (currentUser.nickname || currentUser.username);
    $('btn-logout').style.display = 'inline-block';
    switchSideNav('project');
}

function switchAuthTab(tab, el) {
    const tabs = document.querySelectorAll('#auth-section .tab-btn');
    tabs.forEach(function (t) { t.classList.remove('active'); });
    if (el) el.classList.add('active');
    $('login-form').style.display = tab === 'login' ? 'block' : 'none';
    $('register-form').style.display = tab === 'register' ? 'block' : 'none';
}

async function handleLogin(event) {
    event.preventDefault();
    try {
        const user = await api('/user/login', {
            method: 'POST',
            body: JSON.stringify({
                username: $('login-username').value.trim(),
                password: $('login-password').value
            })
        });
        user.userId = String(user.userId);
        currentUser = user;
        localStorage.setItem('cvmUser', JSON.stringify(user));
        showMsg('登录成功，欢迎 ' + (user.nickname || user.username), 'success');
        enterApp();
    } catch (e) {
        showMsg(e.message, 'error');
    }
}

async function handleRegister(event) {
    event.preventDefault();
    try {
        const user = await api('/user/register', {
            method: 'POST',
            body: JSON.stringify({
                username: $('reg-username').value.trim(),
                password: $('reg-password').value,
                nickname: $('reg-nickname').value.trim()
            })
        });
        // 注册成功后自动登录
        const loginUser = await api('/user/login', {
            method: 'POST',
            body: JSON.stringify({
                username: $('reg-username').value.trim(),
                password: $('reg-password').value
            })
        });
        loginUser.userId = String(loginUser.userId);
        currentUser = loginUser;
        localStorage.setItem('cvmUser', JSON.stringify(loginUser));
        showMsg('注册成功，已自动登录', 'success');
        enterApp();
    } catch (e) {
        showMsg(e.message, 'error');
    }
}

function logout() {
    localStorage.removeItem('cvmUser');
    currentUser = null;
    currentProject = null;
    showAuth();
}

// ============ 项目 ============

function openCreateProject() {
    ['cp-code', 'cp-name', 'cp-desc', 'cp-git-url'].forEach(function (id) { $(id).value = ''; });
    $('project-modal').style.display = 'flex';
    $('cp-code').focus();
}

function closeProjectModal() {
    $('project-modal').style.display = 'none';
}

async function loadProjects() {
    try {
        const data = await api('/project/list');
        const list = $('project-list');
        if (!data || !data.length) {
            list.innerHTML = '<div class="text-muted">暂无项目，点击右上角「+ 注册项目」创建第一个项目。</div>';
            return;
        }
        list.innerHTML = data.map(function (p) {
            return '' +
                '<div class="project-card">' +
                '  <div>' +
                '    <div class="proj-name">' + esc(p.projectName) + ' <span class="badge badge-dev">' + esc(p.projectCode) + '</span></div>' +
                '    <div class="proj-meta">' + esc(p.projectDesc || '暂无描述') + '</div>' +
                '    <div class="proj-git">' + (p.gitUrl ? 'Git: ' + esc(p.gitUrl) : 'Git地址未配置（当前为模拟模式）') + '</div>' +
                '  </div>' +
                '  <div class="project-card-actions">' +
                '    <button class="btn btn-sm btn-primary" onclick="openCreateRequirement(\'' + p.projectId + '\')">创建需求</button>' +
                '    <button class="btn btn-sm" onclick="enterEnvironment(\'' + p.projectId + '\')">进入环境</button>' +
                '  </div>' +
                '</div>';
        }).join('');
    } catch (e) {
        showMsg('加载项目列表失败：' + e.message, 'error');
    }
}

async function createProject() {
    const code = $('cp-code').value.trim();
    const name = $('cp-name').value.trim();
    const gitUrl = $('cp-git-url').value.trim();
    if (!code || !name) { showMsg('请填写项目ID（编号）和项目名称', 'error'); return; }
    if (!gitUrl) { showMsg('请填写 Git 地址', 'error'); return; }
    if (!/^https?:\/\/.+/.test(gitUrl)) { showMsg('Git 地址需以 http(s):// 开头', 'error'); return; }
    try {
        await api('/project/create?creatorUserId=' + currentUser.userId, {
            method: 'POST',
            body: JSON.stringify({
                projectCode: code,
                projectName: name,
                projectDesc: $('cp-desc').value.trim(),
                gitUrl: gitUrl
            })
        });
        showMsg('项目注册成功，已初始化 4 个环境', 'success');
        closeProjectModal();
        ['cp-code', 'cp-name', 'cp-desc', 'cp-git-url'].forEach(function (id) { $(id).value = ''; });
        loadProjects();
    } catch (e) {
        showMsg('注册失败：' + e.message, 'error');
    }
}

// ============ 侧边导航 ============

function switchSideNav(view) {
    ['project', 'requirement', 'cr', 'environment'].forEach(function (v) {
        $('view-' + v).style.display = v === view ? 'block' : 'none';
        $('nav-' + v).classList.toggle('active', v === view);
    });
    if (view === 'project') {
        loadProjects();
    } else if (view === 'requirement') {
        loadMyRequirements();
    } else if (view === 'cr') {
        loadCrManagement();
    } else if (view === 'environment') {
        if (!currentProject) {
            showMsg('请先在「项目管理」中点击项目旁的【进入环境】', 'error');
            switchSideNav('project');
            return;
        }
        renderEnvTabs();
        switchEnvTab(currentEnvTab || 'DEV');
    }
}

async function enterEnvironment(projectId) {
    try {
        const detail = await api('/project/detail?projectId=' + projectId);
        currentProject = detail;
        currentEnvTab = 'DEV';
        const p = detail.project;
        $('env-project-title').textContent = '环境管理：' + p.projectName + '（' + p.projectCode + '）'
            + (p.gitUrl ? '' : '　（当前为模拟模式）');
        switchSideNav('environment');
    } catch (e) {
        showMsg('进入环境失败：' + e.message, 'error');
    }
}

function backToProjectList() {
    currentProject = null;
    switchSideNav('project');
}

function renderEnvTabs() {
    const tabs = $('env-tabs');
    tabs.innerHTML = ENV_LIST.map(function (env) {
        return '<button class="env-tab" data-tab="' + env.code + '">' + env.name + '（' + env.branch + '）</button>';
    }).join('');
    tabs.querySelectorAll('.env-tab').forEach(function (t) {
        t.addEventListener('click', function () { switchEnvTab(t.dataset.tab); });
    });
}

function switchEnvTab(tab) {
    currentEnvTab = tab;
    document.querySelectorAll('#env-tabs .env-tab').forEach(function (t) {
        t.classList.toggle('active', t.dataset.tab === tab);
    });
    renderEnv(tab);
}

// ============ 需求 ============

function openCreateRequirement(projectId) {
    createReqProjectId = projectId;
    ['req-name', 'req-url', 'req-branch'].forEach(function (id) { $(id).value = ''; });
    $('req-source').value = 'NEW';
    $('req-modal').style.display = 'flex';
    $('req-name').focus();
}

function closeReqModal() {
    $('req-modal').style.display = 'none';
    createReqProjectId = null;
}

async function createRequirement() {
    const name = $('req-name').value.trim();
    const branch = $('req-branch').value.trim();
    if (!name || !branch) { showMsg('请填写需求名称和分支名称', 'error'); return; }
    if (!createReqProjectId) { showMsg('未选择所属项目', 'error'); return; }
    try {
        await api('/requirement/create', {
            method: 'POST',
            body: JSON.stringify({
                projectId: createReqProjectId,
                requirementName: name,
                requirementUrl: $('req-url').value.trim(),
                branchName: branch,
                branchSource: $('req-source').value,
                creatorUserId: currentUser.userId
            })
        });
        showMsg('需求创建成功，分支已进入 开发/测试/预发 三个环境待集成列表', 'success');
        closeReqModal();
        loadMyRequirements();
    } catch (e) {
        showMsg('创建需求失败：' + e.message, 'error');
    }
}

// 当前用户的需求列表（需求管理）
async function loadMyRequirements() {
    const tbody = $('my-requirement-list');
    tbody.innerHTML = '<tr><td colspan="8" class="text-muted">加载中…</td></tr>';
    try {
        const requirements = await api('/requirement/list?userId=' + currentUser.userId);
        const projects = await api('/project/list');
        const projectNames = {};
        projects.forEach(function (p) { projectNames[p.projectId] = p.projectName; });
        if (!requirements || !requirements.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-muted">暂无需求，请在「项目管理」中点击项目旁的【创建需求】</td></tr>';
            return;
        }
        tbody.innerHTML = requirements.map(function (r) {
            return '<tr>' +
                '<td>' + esc(r.requirementName) + '</td>' +
                '<td>' + esc(projectNames[r.projectId] || '—') + '</td>' +
                '<td>' + (r.requirementUrl ? '<a href="' + esc(r.requirementUrl) + '" target="_blank">' + esc(r.requirementUrl) + '</a>' : '—') + '</td>' +
                '<td><code>' + esc(r.branchName) + '</code></td>' +
                '<td>' + (r.branchSource === 'NEW' ? '新建' : '远程拉取') + '</td>' +
                '<td>' + envName(r.currentEnv) + '</td>' +
                '<td>' + statusBadge(r.status) + '</td>' +
                '<td>' + fmtTime(r.createTime) + '</td>' +
                '</tr>';
        }).join('');
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-muted">加载需求失败：' + esc(e.message) + '</td></tr>';
    }
}

// ============ 环境视图 ============

async function renderEnv(envCode) {
    const box = $('env-content');
    box.innerHTML = '<div class="text-muted">加载中...</div>';
    try {
        const records = await api('/merge/list?projectId=' + currentProject.project.projectId + '&envCode=' + envCode);
        renderEnvContent(envCode, records);
    } catch (e) {
        box.innerHTML = '<div class="msg msg-error">加载失败：' + esc(e.message) + '</div>';
    }
}

function renderEnvContent(envCode, records) {
    envRecords = records || [];
    // 冲突分支保留在已集成列表，仅待合并/已驳回的分支进入待集成列表
    const merged = envRecords.filter(function (r) { return r.status === 'MERGED' || r.status === 'CONFLICT'; });
    const pending = envRecords.filter(function (r) { return r.status !== 'MERGED' && r.status !== 'CONFLICT'; });
    const isPreview = envCode === 'PREVIEW';
    const isRelease = envCode === 'RELEASE';
    const box = $('env-content');

    // 已集成列表（置顶）
    let html = '<div class="env-list-card">' +
        '<div class="env-list-head"><span class="dot dot-merged"></span>已集成列表</div>';
    if (!merged.length) {
        html += '<div class="text-muted">暂无已合并的分支</div>';
    } else {
        html += '<table class="data-table"><thead><tr>' +
            '<th>分支名称</th><th>需求名称</th><th>代码CR</th><th>操作</th>' +
            '</tr></thead><tbody>';
        merged.forEach(function (r) {
            html += '<tr>' +
                '<td><code>' + esc(r.branchName) + '</code></td>' +
                '<td>' + esc(r.requirementName) + '</td>' +
                '<td>' + crColumnHtml(r) + '</td>' +
                '<td>' + mergedActions(envCode, r) + '</td>' +
                '</tr>';
        });
        html += '</tbody></table>';
    }
    html += '</div>';

    // 环境级操作区（预发：进入正式环境；正式：合并 master），带 ? 说明提示
    if (isPreview || isRelease) {
        const tipText = isPreview
            ? '将预发环境已通过 CR 的全部已集成分支合入 release 正式分支；预发环境的合并列表保持不变。'
            : '将 release 上已上线的分支合并到 master，逻辑删除这些分支与需求，并重建 dev/test/preview/release 环境分支。';
        html += '<div class="env-action-bar">' +
            '<span class="tip-wrap">?<span class="tip-text">' + esc(tipText) + '</span></span>';
        if (isPreview) {
            html += '<button class="btn btn-success" onclick="enterRelease()">进入正式环境</button>';
        } else {
            html += '<button class="btn btn-warning" onclick="mergeMaster()">合并 master</button>';
        }
        html += '</div>';
    }

    // 待集成列表（底部；正式环境没有待集成列表）
    if (!isRelease) {
        html += '<div class="env-list-card env-list-pending">' +
            '<div class="env-list-head"><span class="dot dot-pending"></span>待集成列表</div>';
        if (!pending.length) {
            html += '<div class="text-muted">当前没有待集成的分支</div>';
        } else {
            html += '<table class="data-table"><thead><tr>' +
                '<th>分支名称</th><th>需求名称</th><th>代码CR</th><th>操作</th>' +
                '</tr></thead><tbody>';
            pending.forEach(function (r) {
                html += '<tr>' +
                    '<td><code>' + esc(r.branchName) + '</code></td>' +
                    '<td>' + esc(r.requirementName) + '</td>' +
                    '<td>' + crColumnHtml(r) + '</td>' +
                    '<td>' + pendingActions(envCode, r) + '</td>' +
                    '</tr>';
            });
            html += '</tbody></table>';
        }
        html += '</div>';
    }

    box.innerHTML = html;
}

// 分支的 CR 操作/状态展示（发起cr / 已cr / 重新发起cr）
function crActionHtml(r) {
    const id = r.mergeId;
    if (r.crStatus === 'PASS') {
        return '<span class="badge-status st-merged">cr完成</span>';
    }
    if (r.crStatus === 'PENDING') {
        return '<span class="badge-status st-pending">cr待审核</span>';
    }
    if (r.crStatus === 'REJECT') {
        return '<button class="btn btn-warning btn-sm" onclick="openCrModal(\'' + id + '\')">重新发起cr</button>';
    }
    return '<button class="btn btn-sm" onclick="openCrModal(\'' + id + '\')">发起cr</button>';
}

// 代码CR列：已上线的需求无需评审，其余展示 CR 状态/操作
function crColumnHtml(r) {
    if (requirementStatus(r.requirementId) === 'RELEASED') {
        return '<span class="text-muted">—</span>';
    }
    return crActionHtml(r);
}

function pendingActions(envCode, r) {
    if (requirementStatus(r.requirementId) === 'RELEASED') {
        return '<span class="badge-status st-released">已上线</span>';
    }
    if (r.status === 'REJECTED') {
        return '';
    }
    const verb = '集成到 ' + ENV_CONFIG[envCode].branch;
    return '<button class="btn btn-primary btn-sm" onclick="doMerge(\'' + r.mergeId + '\')">' + verb + '</button>';
}

function mergedActions(envCode, r) {
    const reqStatus = requirementStatus(r.requirementId);
    if (reqStatus === 'RELEASED') {
        return '<span class="badge-status st-released">已上线</span>';
    }
    // 冲突分支：展示「冲突」提示 + 解决冲突按钮 + 退出集成按钮
    if (r.status === 'CONFLICT') {
        return '<span class="badge-status st-conflict">冲突</span> ' +
            '<button class="btn btn-warning btn-sm" onclick="openConflictModal(\'' + r.mergeId + '\')">解决冲突</button> ' +
            '<button class="btn btn-danger btn-sm" onclick="exitIntegration(\'' + r.mergeId + '\')">退出集成</button>';
    }
    return '<button class="btn btn-danger btn-sm" onclick="exitIntegration(\'' + r.mergeId + '\')">退出集成</button>';
}

// ============ 解决冲突弹窗 ============

let conflictModalMergeId = null;

function findMergeRecord(mergeId) {
    return envRecords.find(function (r) { return String(r.mergeId) === String(mergeId); });
}

function openConflictModal(mergeId) {
    const record = findMergeRecord(mergeId);
    if (!record) return;
    conflictModalMergeId = mergeId;
    $('conflict-modal-error').style.display = 'none';
    $('conflict-modal-info').textContent = '分支 ' + record.branchName + ' 合并到 ' + record.targetBranch + ' 时出现冲突：' + (record.conflictDetail || '');
    const files = (record.conflictFiles || []).map(function (f) { return '<li>' + esc(f) + '</li>'; }).join('');
    $('conflict-modal-files').innerHTML = files ? '<ul>' + files + '</ul>' : '<span class="text-muted">—</span>';
    $('conflict-modal-steps').textContent = record.resolveSteps || '请在本地按提示解决冲突并推送后，点击【冲突已解决】。';
    $('conflict-modal').style.display = 'flex';
}

function closeConflictModal() {
    $('conflict-modal').style.display = 'none';
    conflictModalMergeId = null;
}

async function confirmConflictResolved() {
    if (!conflictModalMergeId) return;
    try {
        await api('/merge/resolve-conflict?mergeId=' + conflictModalMergeId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        closeConflictModal();
        showMsg('冲突已解决并合并成功', 'success');
        reloadEnv();
    } catch (e) {
        // 冲突仍存在：弹窗保留，提示用户继续解决
        const errorBox = $('conflict-modal-error');
        errorBox.textContent = '冲突仍存在，请继续按上述步骤解决后再次点击【冲突已解决】：' + e.message;
        errorBox.style.display = 'block';
    }
}

// ============ 合并 / 流程操作 ============

async function doMerge(mergeId) {
    if (!(await confirmDialog('确认将分支合并到当前环境公共分支？'))) return;
    try {
        const res = await fetch(API_BASE + '/merge/merge?mergeId=' + mergeId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        const json = await res.json();
        showMsg(json.message, json.success && !json.conflict ? 'success' : 'error');
        reloadEnv();
    } catch (e) {
        showMsg('合并失败：' + e.message, 'error');
    }
}

async function exitIntegration(mergeId) {
    if (!(await confirmDialog('确认将该分支退出集成？\n公共分支将恢复到与 master 一致，再按原顺序重新合并其余已集成分支；若出现冲突会标记待解决，解决后自动继续合并后续分支。'))) return;
    try {
        const res = await fetch(API_BASE + '/merge/exit-integration?mergeId=' + mergeId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        const json = await res.json();
        showMsg(json.message, json.success ? 'success' : 'error');
        await refreshProjectDetail();
        reloadEnv();
    } catch (e) {
        showMsg('退出集成失败：' + e.message, 'error');
    }
}

async function enterRelease() {
    if (!(await confirmDialog('确认将预发环境已集成的全部分支（需已完成 CR 审核）合入 release 正式分支？\n预发环境合并列表保持不变。'))) return;
    try {
        const res = await fetch(API_BASE + '/merge/enter-release?projectId=' + currentProject.project.projectId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        const json = await res.json();
        showMsg(json.message, json.success ? 'success' : 'error');
        if (json.success) {
            await refreshProjectDetail();
            reloadEnv();
        }
    } catch (e) {
        showMsg('进入正式环境失败：' + e.message, 'error');
    }
}

async function mergeMaster() {
    if (!(await confirmDialog('确认执行合并 master？\n将 release 上已上线分支合并到 master，逻辑删除这些分支与需求，并重建 dev/test/preview/release 环境分支。'))) return;
    try {
        const res = await fetch(API_BASE + '/merge/merge-master?projectId=' + currentProject.project.projectId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        const json = await res.json();
        showMsg(json.message, json.success ? 'success' : 'error');
        if (json.success) {
            await refreshProjectDetail();
            reloadEnv();
        }
    } catch (e) {
        showMsg('合并 master 失败：' + e.message, 'error');
    }
}

// ============ CR管理 ============

let crModalMergeId = null;

function openCrModal(mergeId) {
    crModalMergeId = mergeId;
    const sel = $('cr-reviewer');
    sel.innerHTML = '<option value="">加载中…</option>';
    $('cr-comment-input').value = '';
    const projectId = currentProject ? currentProject.project.projectId : null;
    $('cr-modal-info').textContent = projectId ? '提交分支的代码评审请求给指定审核人。' : '';
    api('/cr/reviewers?projectId=' + projectId).then(function (users) {
        const me = String(currentUser.userId);
        const options = (users || []).filter(function (u) { return String(u.userId) !== me; })
            .map(function (u) { return '<option value="' + u.userId + '">' + esc(u.nickname || u.username) + '（' + esc(u.username) + '）</option>'; })
            .join('');
        sel.innerHTML = options || '<option value="">暂无其他用户可指定</option>';
    }).catch(function (e) {
        sel.innerHTML = '<option value="">加载失败</option>';
        showMsg('加载CR审核人失败：' + e.message, 'error');
    });
    $('cr-modal').style.display = 'flex';
}

function closeCrModal() {
    $('cr-modal').style.display = 'none';
    crModalMergeId = null;
}

async function submitCr() {
    const reviewerUserId = $('cr-reviewer').value;
    if (!crModalMergeId || !reviewerUserId) { showMsg('请选择CR审核人', 'error'); return; }
    try {
        await api('/cr/submit', {
            method: 'POST',
            body: JSON.stringify({
                mergeId: crModalMergeId,
                submitterUserId: currentUser.userId,
                reviewerUserId: reviewerUserId,
                crComment: $('cr-comment-input').value.trim()
            })
        });
        showMsg('CR已提交，等待审核人审核', 'success');
        closeCrModal();
        reloadEnv();
    } catch (e) {
        showMsg('发起CR失败：' + e.message, 'error');
    }
}

async function loadCrManagement() {
    const pendingBody = $('cr-pending-list');
    const submittedBody = $('cr-submitted-list');
    pendingBody.innerHTML = '<tr><td colspan="7" class="text-muted">加载中…</td></tr>';
    submittedBody.innerHTML = '<tr><td colspan="7" class="text-muted">加载中…</td></tr>';
    try {
        const pending = await api('/cr/my-pending?userId=' + currentUser.userId);
        const submitted = await api('/cr/my-submitted?userId=' + currentUser.userId);
        if (!pending || !pending.length) {
            pendingBody.innerHTML = '<tr><td colspan="7" class="text-muted">暂无需要我审核的 CR</td></tr>';
        } else {
            pendingBody.innerHTML = pending.map(function (c) {
                return '<tr>' +
                    '<td><code>' + esc(c.branchName) + '</code></td>' +
                    '<td>' + esc(c.requirementName) + '</td>' +
                    '<td>' + envName(c.targetEnv) + '</td>' +
                    '<td>' + esc(c.submitterName || c.submitterUserId) + '</td>' +
                    '<td>' + esc(c.crComment || '—') + '</td>' +
                    '<td>' + fmtTime(c.createTime) + '</td>' +
                    '<td>' +
                    '<button class="btn btn-success btn-sm" onclick="crAudit(\'' + c.mergeId + '\',\'PASS\')">通过</button> ' +
                    '<button class="btn btn-danger btn-sm" onclick="crAudit(\'' + c.mergeId + '\',\'REJECT\')">驳回</button>' +
                    '</td>' +
                    '</tr>';
            }).join('');
        }
        if (!submitted || !submitted.length) {
            submittedBody.innerHTML = '<tr><td colspan="7" class="text-muted">暂无我提交的 CR</td></tr>';
        } else {
            const clsMap = { PASS: 'st-merged', REJECT: 'st-rejected', PENDING: 'st-pending' };
            const labelMap = { PASS: 'CR通过', REJECT: 'CR驳回', PENDING: '待审核' };
            submittedBody.innerHTML = submitted.map(function (c) {
                return '<tr>' +
                    '<td><code>' + esc(c.branchName) + '</code></td>' +
                    '<td>' + esc(c.requirementName) + '</td>' +
                    '<td>' + envName(c.targetEnv) + '</td>' +
                    '<td>' + esc(c.reviewerName || c.reviewerUserId) + '</td>' +
                    '<td><span class="badge-status ' + (clsMap[c.crStatus] || 'st-pending') + '">' + esc(labelMap[c.crStatus] || c.crStatus) + '</span></td>' +
                    '<td>' + esc(c.crComment || '—') + '</td>' +
                    '<td>' + fmtTime(c.createTime) + '</td>' +
                    '</tr>';
            }).join('');
        }
    } catch (e) {
        pendingBody.innerHTML = '<tr><td colspan="7" class="text-muted">加载失败：' + esc(e.message) + '</td></tr>';
        submittedBody.innerHTML = '<tr><td colspan="7" class="text-muted">加载失败：' + esc(e.message) + '</td></tr>';
    }
}

async function crAudit(mergeId, status) {
    let comment = '';
    if (status === 'REJECT') {
        comment = await promptDialog('请输入驳回意见：');
        if (comment === null) { return; }
        if (!comment.trim()) { showMsg('驳回必须填写意见', 'error'); return; }
    }
    try {
        await api('/cr/audit', {
            method: 'POST',
            body: JSON.stringify({
                mergeId: mergeId,
                reviewerUserId: currentUser.userId,
                crStatus: status,
                crComment: comment.trim()
            })
        });
        showMsg(status === 'PASS' ? 'CR 已通过' : 'CR 已驳回', 'success');
        loadCrManagement();
    } catch (e) {
        showMsg('CR 审核失败：' + e.message, 'error');
    }
}

async function reloadEnv() {
    renderEnv(currentEnvTab);
}

async function refreshProjectDetail() {
    currentProject = await api('/project/detail?projectId=' + currentProject.project.projectId);
}

// ============ 展示辅助 ============

function requirementStatus(requirementId) {
    const list = (currentProject && currentProject.requirements) || [];
    const r = list.find(function (x) { return String(x.requirementId) === String(requirementId); });
    return r ? r.status : '';
}

function envName(envCode) {
    return (ENV_CONFIG[envCode] && ENV_CONFIG[envCode].name) || envCode || '';
}

function statusBadge(status, desc) {
    const map = {
        PENDING: 'st-pending', CONFLICT: 'st-conflict', MERGED: 'st-merged', REJECTED: 'st-rejected',
        DEVELOPING: 'st-developing', RELEASED: 'st-released'
    };
    const label = desc || MERGE_STATUS[status] || REQUIREMENT_STATUS[status] || status;
    return '<span class="badge-status ' + (map[status] || 'st-pending') + '">' + esc(label) + '</span>';
}

function fmtTime(t) {
    if (!t) return '—';
    return String(t).replace('T', ' ').substring(0, 19);
}

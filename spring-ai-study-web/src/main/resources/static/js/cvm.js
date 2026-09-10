// ============ 代码版本管理系统 CVM 前端逻辑 ============

const API_BASE = '/api/cvm';

const ENV_CONFIG = {
    DEV:     { name: '开发环境', branch: 'dev',     badge: 'badge-dev' },
    TEST:    { name: '测试环境', branch: 'test',    badge: 'badge-test' },
    PREVIEW: { name: '预发环境', branch: 'preview', badge: 'badge-preview' },
    RELEASE: { name: '正式环境', branch: 'release', badge: 'badge-release' }
};

const REQUIREMENT_STATUS = {
    DEVELOPING: '开发中/待合并',
    CONFLICT: '冲突待解决',
    MERGED: '已合并',
    PUBLISHED: '已发布',
    MERGED_MASTER: '已合master'
};

const MERGE_STATUS = {
    PENDING: '待合并',
    CONFLICT: '冲突待解决',
    MERGED: '已合并',
    REJECTED: '已驳回'
};

let currentUser = null;
let currentProject = null; // 项目详情（含 project/environments/requirements）
let currentEnvTab = 'REQ';

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
    const box = $('msg-box');
    box.className = 'msg ' + (type === 'error' ? 'msg-error' : 'msg-success');
    box.textContent = text;
    box.style.display = 'block';
    setTimeout(function () { box.style.display = 'none'; }, 6000);
}

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
    loadProjects();
    loadLogs();
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

function toggleCreateProject(show) {
    const form = $('create-project-form');
    form.style.display = show === undefined ? (form.style.display === 'none' ? 'flex' : 'none') : (show ? 'flex' : 'none');
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
                '<div class="project-card" onclick="openProject(\'' + p.projectId + '\')">' +
                '  <div>' +
                '    <div class="proj-name">' + esc(p.projectName) + ' <span class="badge badge-dev">' + esc(p.projectCode) + '</span></div>' +
                '    <div class="proj-meta">' + esc(p.projectDesc || '暂无描述') + '</div>' +
                '    <div class="proj-git">' + (p.gitUrl ? 'Git: ' + esc(p.gitUrl) : 'Git地址未配置（当前为模拟模式）') + '</div>' +
                '  </div>' +
                '  <span class="btn btn-sm">进入管理 →</span>' +
                '</div>';
        }).join('');
    } catch (e) {
        showMsg('加载项目列表失败：' + e.message, 'error');
    }
}

async function createProject() {
    const code = $('cp-code').value.trim();
    const name = $('cp-name').value.trim();
    if (!code || !name) { showMsg('请填写项目ID（编号）和项目名称', 'error'); return; }
    try {
        await api('/project/create?creatorUserId=' + currentUser.userId, {
            method: 'POST',
            body: JSON.stringify({
                projectCode: code,
                projectName: name,
                projectDesc: $('cp-desc').value.trim(),
                gitUrl: $('cp-git-url').value.trim(),
                gitUsername: $('cp-git-user').value.trim(),
                gitToken: $('cp-git-token').value.trim()
            })
        });
        showMsg('项目注册成功，已初始化 4 个环境', 'success');
        toggleCreateProject(false);
        ['cp-code', 'cp-name', 'cp-desc', 'cp-git-url', 'cp-git-user', 'cp-git-token'].forEach(function (id) { $(id).value = ''; });
        loadProjects();
        loadLogs();
    } catch (e) {
        showMsg('注册失败：' + e.message, 'error');
    }
}

async function openProject(projectId) {
    try {
        currentProject = await api('/project/detail?projectId=' + projectId);
        const p = currentProject.project;
        $('project-title').textContent = '🔀 ' + p.projectName + '（' + p.projectCode + '）';
        $('project-meta').textContent = (p.projectDesc || '') +
            (p.gitUrl ? ' ｜ Git: ' + p.gitUrl : ' ｜ Git地址未配置（当前为模拟模式）');
        $('project-detail-panel').style.display = 'block';
        renderEnvTabs();
        switchEnvTab('REQ');
        loadLogs(p.projectId);
        $('project-detail-panel').scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (e) {
        showMsg('打开项目失败：' + e.message, 'error');
    }
}

function backToProjects() {
    currentProject = null;
    $('project-detail-panel').style.display = 'none';
    loadLogs();
}

function renderEnvTabs() {
    const tabs = $('env-tabs');
    let html = '<button class="env-tab" data-tab="REQ">📋 需求</button>';
    Object.keys(ENV_CONFIG).forEach(function (code) {
        const cfg = ENV_CONFIG[code];
        html += '<button class="env-tab" data-tab="' + code + '">' + cfg.name + '（' + cfg.branch + '）</button>';
    });
    tabs.innerHTML = html;
    tabs.querySelectorAll('.env-tab').forEach(function (t) {
        t.addEventListener('click', function () { switchEnvTab(t.dataset.tab); });
    });
}

function switchEnvTab(tab) {
    currentEnvTab = tab;
    document.querySelectorAll('.env-tab').forEach(function (t) {
        t.classList.toggle('active', t.dataset.tab === tab);
    });
    $('requirement-content').style.display = tab === 'REQ' ? 'block' : 'none';
    $('env-content').style.display = tab === 'REQ' ? 'none' : 'block';
    if (tab === 'REQ') {
        renderRequirements();
    } else {
        renderEnv(tab);
    }
}

// ============ 需求 ============

function renderRequirements() {
    const list = currentProject.requirements || [];
    const tbody = $('requirement-list');
    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-muted">暂无需求，请在上方创建</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(function (r) {
        return '<tr>' +
            '<td>' + esc(r.requirementName) + '</td>' +
            '<td>' + (r.requirementUrl ? '<a href="' + esc(r.requirementUrl) + '" target="_blank">' + esc(r.requirementUrl) + '</a>' : '—') + '</td>' +
            '<td><code>' + esc(r.branchName) + '</code></td>' +
            '<td>' + (r.branchSource === 'NEW' ? '新建' : '远程拉取') + '</td>' +
            '<td>' + envName(r.currentEnv) + '</td>' +
            '<td>' + statusBadge(r.status) + '</td>' +
            '<td>' + fmtTime(r.createTime) + '</td>' +
            '</tr>';
    }).join('');
}

async function createRequirement() {
    const name = $('req-name').value.trim();
    const branch = $('req-branch').value.trim();
    if (!name || !branch) { showMsg('请填写需求名称和分支名称', 'error'); return; }
    try {
        await api('/requirement/create', {
            method: 'POST',
            body: JSON.stringify({
                projectId: currentProject.project.projectId,
                requirementName: name,
                requirementUrl: $('req-url').value.trim(),
                branchName: branch,
                branchSource: $('req-source').value,
                creatorUserId: currentUser.userId
            })
        });
        showMsg('需求创建成功，分支已进入开发环境待合并列表', 'success');
        ['req-name', 'req-url', 'req-branch'].forEach(function (id) { $(id).value = ''; });
        await refreshProjectDetail();
        renderRequirements();
        loadLogs(currentProject.project.projectId);
    } catch (e) {
        showMsg('创建需求失败：' + e.message, 'error');
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
    const cfg = ENV_CONFIG[envCode];
    const pending = (records || []).filter(function (r) { return r.status !== 'MERGED'; });
    const merged = (records || []).filter(function (r) { return r.status === 'MERGED'; });
    const box = $('env-content');

    let html = '<div class="sub-section">' +
        '<h4>🕐 ' + cfg.name + ' 待合并列表（合并目标：' + cfg.branch + '）' +
        (envCode === 'RELEASE' ? '　<span class="text-muted">正式环境合并前需完成 CR 审核</span>' : '') +
        '</h4>';

    if (!pending.length) {
        html += '<div class="text-muted">当前没有待合并的分支</div>';
    } else {
        html += '<table class="data-table"><thead><tr>' +
            '<th>分支</th><th>需求</th><th>开发人</th><th>状态</th><th>CR</th><th>操作</th>' +
            '</tr></thead><tbody>';
        pending.forEach(function (r) {
            html += '<tr>' +
                '<td><code>' + esc(r.branchName) + '</code></td>' +
                '<td>' + esc(r.requirementName) + '</td>' +
                '<td>' + esc(r.userName) + '</td>' +
                '<td>' + statusBadge(r.status, r.statusDesc) + '</td>' +
                '<td>' + crBadge(r) + '</td>' +
                '<td>' + pendingActions(envCode, r) + '</td>' +
                '</tr>';
            if (r.status === 'CONFLICT') {
                html += '<tr><td colspan="6" class="mt-8">' + conflictBlock(r) + '</td></tr>';
            }
        });
        html += '</tbody></table>';
    }
    html += '</div>';

    html += '<div class="sub-section"><h4>✅ ' + cfg.name + ' 已合并列表（分支 + 合并人 + 时间）</h4>';
    if (!merged.length) {
        html += '<div class="text-muted">暂无已合并的分支</div>';
    } else {
        html += '<table class="data-table"><thead><tr>' +
            '<th>分支</th><th>需求</th><th>合并人</th><th>Commit</th><th>合并时间</th><th>操作</th>' +
            '</tr></thead><tbody>';
        merged.forEach(function (r) {
            html += '<tr>' +
                '<td><code>' + esc(r.branchName) + '</code></td>' +
                '<td>' + esc(r.requirementName) + '</td>' +
                '<td>' + esc(r.userName) + '</td>' +
                '<td><code>' + esc(shortCommit(r.mergeCommit)) + '</code></td>' +
                '<td>' + fmtTime(r.mergeTime) + '</td>' +
                '<td>' + mergedActions(envCode, r) + '</td>' +
                '</tr>';
        });
        html += '</tbody></table>';
    }
    html += '</div>';

    box.innerHTML = html;
}

function pendingActions(envCode, r) {
    if (r.status === 'CONFLICT') {
        return '<button class="btn btn-warning btn-sm" onclick="resolveConflict(\'' + r.mergeId + '\')">解决冲突并合并</button>';
    }
    if (r.status === 'REJECTED') {
        return '<span class="text-muted">CR 已驳回，可通过 CR 后重新合并</span>';
    }
    // PENDING
    if (envCode === 'RELEASE') {
        if (r.crStatus === 'PASS') {
            return '<button class="btn btn-primary btn-sm" onclick="doMerge(\'' + r.mergeId + '\')">合并到 ' + ENV_CONFIG[envCode].branch + '</button>';
        }
        return '<span class="text-muted">待CR审核</span> ' +
            '<input type="text" id="cr-comment-' + r.mergeId + '" placeholder="审核意见（可空）" style="width:180px"> ' +
            '<button class="btn btn-success btn-sm" onclick="crAudit(\'' + r.mergeId + '\',\'PASS\')">CR通过</button> ' +
            '<button class="btn btn-danger btn-sm" onclick="crAudit(\'' + r.mergeId + '\',\'REJECT\')">CR驳回</button>';
    }
    return '<button class="btn btn-primary btn-sm" onclick="doMerge(\'' + r.mergeId + '\')">合并到 ' + ENV_CONFIG[envCode].branch + '</button>';
}

function mergedActions(envCode, r) {
    const reqStatus = requirementStatus(r.requirementId);
    if (envCode !== 'RELEASE') {
        if (reqStatus === 'MERGED_MASTER' || reqStatus === 'PUBLISHED') {
            return '<span class="text-muted">' + statusText(reqStatus) + '</span>';
        }
        return '<button class="btn btn-success btn-sm" onclick="nextEnv(\'' + r.requirementId + '\')">验证完成，进入下一环境</button>';
    }
    // RELEASE
    if (reqStatus === 'MERGED') {
        return '<button class="btn btn-success btn-sm" onclick="publish(\'' + r.requirementId + '\')">发布到线上</button>';
    }
    if (reqStatus === 'PUBLISHED') {
        return '<button class="btn btn-warning btn-sm" onclick="mergeMaster(\'' + r.requirementId + '\')">合并 master</button> <span class="text-muted">已发布</span>';
    }
    if (reqStatus === 'MERGED_MASTER') {
        return '<span class="badge badge-status st-master">已合 master</span>';
    }
    return '<span class="text-muted">' + statusText(reqStatus) + '</span>';
}

function conflictBlock(r) {
    const files = (r.conflictFiles || []).map(function (f) { return '<li>' + esc(f) + '</li>'; }).join('');
    return '<div class="msg msg-error" style="display:block">⚠️ 合并冲突：' + esc(r.conflictDetail || '') + '</div>' +
        (files ? '<div class="text-muted">冲突文件：<ul style="margin:4px 0 8px 18px">' + files + '</ul></div>' : '') +
        '<div class="text-muted" style="margin-bottom:4px">解决冲突步骤：</div>' +
        '<div class="resolve-steps">' + esc(r.resolveSteps || '') + '</div>';
}

function crBadge(r) {
    if (r.crRequired !== true) return '<span class="text-muted">—</span>';
    const map = { PASS: 'CR通过', REJECT: 'CR驳回', PENDING: '待审核' };
    const label = map[r.crStatus] || r.crStatus || '待审核';
    const cls = r.crStatus === 'PASS' ? 'st-merged' : (r.crStatus === 'REJECT' ? 'st-rejected' : 'st-pending');
    return '<span class="badge-status ' + cls + '">' + label + '</span>';
}

// ============ 合并 / 流程操作 ============

async function doMerge(mergeId) {
    if (!confirm('确认将分支合并到当前环境公共分支？')) return;
    try {
        const res = await fetch(API_BASE + '/merge/merge?mergeId=' + mergeId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        const json = await res.json();
        showMsg(json.message, json.success && !json.conflict ? 'success' : 'error');
        reloadEnv();
    } catch (e) {
        showMsg('合并失败：' + e.message, 'error');
    }
}

async function resolveConflict(mergeId) {
    if (!confirm('已在本地按步骤解决冲突并推送后，点击确认重新合并？')) return;
    try {
        await api('/merge/resolve-conflict?mergeId=' + mergeId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        showMsg('冲突已解决并合并成功', 'success');
        reloadEnv();
    } catch (e) {
        showMsg('解决冲突失败：' + e.message, 'error');
    }
}

async function nextEnv(requirementId) {
    if (!confirm('当前环境验证完成，确认将分支放入下一环境待合并列表？')) return;
    try {
        await api('/merge/next-env?requirementId=' + requirementId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        showMsg('已进入下一环境待合并列表', 'success');
        await refreshProjectDetail();
        reloadEnv();
        loadLogs(currentProject.project.projectId);
    } catch (e) {
        showMsg('进入下一环境失败：' + e.message, 'error');
    }
}

async function publish(requirementId) {
    if (!confirm('确认将该分支发布到线上系统？')) return;
    try {
        await api('/merge/publish?requirementId=' + requirementId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        showMsg('发布成功', 'success');
        await refreshProjectDetail();
        reloadEnv();
        loadLogs(currentProject.project.projectId);
    } catch (e) {
        showMsg('发布失败：' + e.message, 'error');
    }
}

async function mergeMaster(requirementId) {
    if (!confirm('确认合并 master？其他环境将退出该分支并基于 master 更新 dev。')) return;
    try {
        await api('/merge/merge-master?requirementId=' + requirementId + '&operatorUserId=' + currentUser.userId, { method: 'POST' });
        showMsg('已合并 master，其他环境已基于 master 更新', 'success');
        await refreshProjectDetail();
        reloadEnv();
        loadLogs(currentProject.project.projectId);
    } catch (e) {
        showMsg('合并 master 失败：' + e.message, 'error');
    }
}

async function crAudit(mergeId, status) {
    const comment = ($('cr-comment-' + mergeId) ? $('cr-comment-' + mergeId).value : '');
    if (status === 'REJECT' && !comment) { showMsg('请填写驳回意见', 'error'); return; }
    try {
        await api('/cr/audit', {
            method: 'POST',
            body: JSON.stringify({
                mergeId: mergeId,
                reviewerUserId: currentUser.userId,
                crStatus: status,
                crComment: comment
            })
        });
        showMsg(status === 'PASS' ? 'CR 已通过' : 'CR 已驳回', 'success');
        reloadEnv();
    } catch (e) {
        showMsg('CR 审核失败：' + e.message, 'error');
    }
}

async function reloadEnv() {
    if (currentEnvTab === 'REQ') {
        await refreshProjectDetail();
        renderRequirements();
    } else {
        renderEnv(currentEnvTab);
    }
}

async function refreshProjectDetail() {
    currentProject = await api('/project/detail?projectId=' + currentProject.project.projectId);
}

// ============ 操作日志 ============

async function loadLogs(projectId) {
    try {
        const url = projectId ? '/log/list?projectId=' + projectId : '/log/list';
        const logs = await api(url);
        const tbody = $('log-list');
        if (!logs || !logs.length) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-muted">暂无操作日志</td></tr>';
            return;
        }
        tbody.innerHTML = logs.map(function (l) {
            return '<tr><td>' + fmtTime(l.createTime) + '</td><td>' + esc(actionText(l.action)) + '</td><td>' + esc(l.detail || '') + '</td></tr>';
        }).join('');
    } catch (e) {
        $('log-list').innerHTML = '<tr><td colspan="3" class="text-muted">加载日志失败</td></tr>';
    }
}

function actionText(action) {
    const map = {
        REGISTER_USER: '注册用户', LOGIN_USER: '用户登录', CREATE_PROJECT: '注册项目',
        INIT_ENVIRONMENT: '初始化环境', CREATE_ENVIRONMENT: '创建环境', CREATE_REQUIREMENT: '创建需求',
        CREATE_BRANCH: '创建分支', PULL_BRANCH: '拉取分支', MERGE: '合并', MERGE_CONFLICT: '合并冲突',
        RESOLVE_CONFLICT: '解决冲突', NEXT_ENV: '进入下一环境', CR_PASS: 'CR通过', CR_REJECT: 'CR驳回',
        REJECT_MERGE: '驳回合并', PUBLISH: '发布', MERGE_MASTER: '合并master', RESET_ENV: '环境重置'
    };
    return map[action] || action;
}

// ============ 展示辅助 ============

function requirementStatus(requirementId) {
    const list = (currentProject && currentProject.requirements) || [];
    const r = list.find(function (x) { return String(x.requirementId) === String(requirementId); });
    return r ? r.status : '';
}

function statusText(status) {
    return REQUIREMENT_STATUS[status] || status || '';
}

function envName(envCode) {
    return (ENV_CONFIG[envCode] && ENV_CONFIG[envCode].name) || envCode || '';
}

function statusBadge(status, desc) {
    const map = {
        PENDING: 'st-pending', CONFLICT: 'st-conflict', MERGED: 'st-merged', REJECTED: 'st-rejected',
        DEVELOPING: 'st-developing', PUBLISHED: 'st-published', MERGED_MASTER: 'st-master'
    };
    const label = desc || MERGE_STATUS[status] || REQUIREMENT_STATUS[status] || status;
    return '<span class="badge-status ' + (map[status] || 'st-pending') + '">' + esc(label) + '</span>';
}

function shortCommit(commit) {
    if (!commit) return '—';
    return commit.length > 12 ? commit.substring(0, 12) + '…' : commit;
}

function fmtTime(t) {
    if (!t) return '—';
    return String(t).replace('T', ' ').substring(0, 19);
}

// ============ 操作日志页 CVM 前端逻辑（独立页面，不依赖 cvm.js） ============

const API_BASE = '/api/cvm';

function $(id) { return document.getElementById(id); }

function esc(str) {
    if (str === null || str === undefined) return '';
    return String(str).replace(/[&<>"']/g, function (c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
}

function fmtTime(t) {
    if (!t) return '—';
    return String(t).replace('T', ' ').substring(0, 19);
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

async function api(path, options) {
    options = options || {};
    const res = await fetch(API_BASE + path, Object.assign({ headers: { 'Content-Type': 'application/json' } }, options));
    let json;
    try { json = await res.json(); } catch (e) { throw new Error('服务异常，请稍后重试'); }
    if (!json.success) { throw new Error(json.message || '请求失败'); }
    return json.data;
}

async function loadProjects() {
    const projects = await api('/project/list');
    const sel = $('filter-project');
    projects.forEach(function (p) {
        const opt = document.createElement('option');
        opt.value = p.projectId;
        opt.textContent = p.projectName + '（' + p.projectCode + '）';
        sel.appendChild(opt);
    });
}

async function loadLogs() {
    const projectId = $('filter-project').value;
    const url = projectId ? '/log/list?projectId=' + projectId : '/log/list';
    const tbody = $('log-list');
    try {
        const logs = await api(url);
        $('log-count').textContent = '共 ' + logs.length + ' 条';
        if (!logs || !logs.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-muted">暂无操作日志</td></tr>';
            return;
        }
        tbody.innerHTML = logs.map(function (l) {
            return '<tr>'
                + '<td>' + esc(fmtTime(l.createTime)) + '</td>'
                + '<td>' + esc(l.operatorName || (l.operatorUserId ? '用户' + String(l.operatorUserId).slice(-4) : '系统')) + '</td>'
                + '<td>' + esc(l.projectName || '—') + '</td>'
                + '<td>' + esc(actionText(l.action)) + '</td>'
                + '<td>' + esc(l.detail || '') + '</td>'
                + '</tr>';
        }).join('');
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-muted">加载日志失败：' + esc(e.message) + '</td></tr>';
    }
}

// 显示当前登录人（无则隐藏）
(function init() {
    const saved = localStorage.getItem('cvmUser');
    if (saved) {
        try {
            const u = JSON.parse(saved);
            if (u && (u.nickname || u.username)) {
                $('top-user').textContent = '👤 ' + (u.nickname || u.username);
            }
        } catch (e) { /* ignore */ }
    }
    loadProjects().then(function () { return loadLogs(); }).catch(function (e) {
        $('log-list').innerHTML = '<tr><td colspan="5" class="text-muted">加载失败：' + esc(e.message) + '</td></tr>';
    });
})();

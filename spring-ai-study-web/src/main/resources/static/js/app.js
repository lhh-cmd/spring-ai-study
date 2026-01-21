// API 基础地址
const API_BASE = 'http://localhost:8080/api/moyoo';

// 当前用户信息
let currentUser = null;
let moyooTimer = null;
let isPaused = false;
let pausedStartTime = null; // 暂停开始时间
let totalPausedMs = 0; // 累计暂停时长（毫秒）
let moyooStartTime = null; // 摸鱼开始时间（从服务器获取后保存）

// 长按结束按钮相关
let longPressTimer = null;
const LONG_PRESS_DURATION = 3000; // 长按3秒

// 页面加载时检查登录状态
document.addEventListener('DOMContentLoaded', function() {
    const savedUser = localStorage.getItem('currentUser');
    if (savedUser) {
        try {
            currentUser = JSON.parse(savedUser);
            // 验证缓存的用户数据
            if (!currentUser || !currentUser.userId) {
                console.warn('缓存的用户数据异常，清除缓存');
                localStorage.removeItem('currentUser');
                currentUser = null;
            } else {
                // 确保 userId 是字符串类型（避免大整数精度丢失）
                if (typeof currentUser.userId === 'number') {
                    currentUser.userId = String(currentUser.userId);
                }
                console.log('从缓存恢复用户，用户ID:', currentUser.userId, '(类型:', typeof currentUser.userId + ')');
                showMainSection();
                checkCurrentMoYoo();
            }
        } catch (error) {
            console.error('解析用户数据失败:', error);
            localStorage.removeItem('currentUser');
            currentUser = null;
        }
    }
    // 加载职业列表
    loadProfessions();
});

// 格式化数字
function formatNumber(num, decimals = 0) {
    if (num === null || num === undefined) return '0';
    
    const number = typeof num === 'string' ? parseFloat(num) : num;
    
    if (decimals > 0) {
        return number.toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    } else {
        return number.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }
}

// 切换密码显示/隐藏（确保在全局作用域）
window.togglePassword = function(inputId, toggleElement) {
    const input = document.getElementById(inputId);
    if (input.type === 'password') {
        input.type = 'text';
        toggleElement.classList.add('active');
    } else {
        input.type = 'password';
        toggleElement.classList.remove('active');
    }
}

// 切换登录/注册标签（确保在全局作用域）
window.switchTab = function(tab, clickedElement) {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const tabs = document.querySelectorAll('.tab-btn');
    
    tabs.forEach(t => t.classList.remove('active'));
    if (clickedElement) {
        clickedElement.classList.add('active');
    } else {
        // 如果没有传递元素，根据tab参数设置
        tabs.forEach(t => {
            if ((tab === 'login' && t.textContent === '登录') || 
                (tab === 'register' && t.textContent === '注册')) {
                t.classList.add('active');
            }
        });
    }
    
    if (tab === 'login') {
        loginForm.style.display = 'block';
        registerForm.style.display = 'none';
    } else {
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
    }
}

// 处理登录（确保在全局作用域）
window.handleLogin = async function(event) {
    event.preventDefault();
    console.log('handleLogin 函数被调用');
    
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    
    // 验证输入
    if (!username) {
        showMessage('请输入用户名', 'error');
        return;
    }
    if (!password) {
        showMessage('请输入密码', 'error');
        return;
    }
    
    try {
        console.log('开始登录，用户名:', username);
        const response = await fetch(`${API_BASE}/user/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        console.log('登录响应状态:', response.status);
        
        // 检查响应状态
        if (!response.ok) {
            const errorText = await response.text();
            console.error('登录失败，响应状态:', response.status, '响应内容:', errorText);
            showMessage(`登录失败：服务器错误 (${response.status})`, 'error');
            return;
        }
        
        const data = await response.json();
        console.log('登录响应数据:', data);
        
        if (data.success) {
            currentUser = data.data;
            // 验证返回的用户数据
            if (!currentUser || !currentUser.userId) {
                console.error('登录返回数据异常:', currentUser);
                showMessage('登录返回数据异常，请重试', 'error');
                return;
            }
            // 确保 userId 是字符串类型（避免大整数精度丢失）
            if (typeof currentUser.userId === 'string') {
                // 已经是字符串，直接使用
            } else if (typeof currentUser.userId === 'number') {
                // 如果是数字，转换为字符串
                currentUser.userId = String(currentUser.userId);
            }
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            console.log('登录成功，用户ID:', currentUser.userId, '(类型:', typeof currentUser.userId + ')');
            showMessage('登录成功！', 'success');
            showMainSection();
            checkCurrentMoYoo();
        } else {
            console.error('登录失败:', data.message);
            showMessage(data.message || '登录失败', 'error');
        }
    } catch (error) {
        console.error('登录网络错误:', error);
        showMessage('网络错误：' + error.message + '。请检查服务器是否启动。', 'error');
    }
};

// 加载职业列表
async function loadProfessions() {
    try {
        const response = await fetch(`${API_BASE}/profession/list`);
        const data = await response.json();
        
        if (data.success) {
            const select = document.getElementById('register-profession');
            data.data.forEach(profession => {
                const option = document.createElement('option');
                option.value = profession.value;
                option.textContent = profession.label;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('加载职业列表失败:', error);
    }
}

// 处理注册
async function handleRegister(event) {
    event.preventDefault();
    
    const username = document.getElementById('register-username').value;
    const password = document.getElementById('register-password').value;
    const profession = document.getElementById('register-profession').value;
    const dailyWorkHours = parseFloat(document.getElementById('register-work-hours').value);
    
    try {
        const response = await fetch(`${API_BASE}/user/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password, profession, dailyWorkHours })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('注册成功！请登录', 'success');
            switchTab('login', null);
            document.getElementById('login-username').value = username;
        } else {
            showMessage(data.message || '注册失败', 'error');
        }
    } catch (error) {
        showMessage('网络错误：' + error.message, 'error');
    }
}

// 显示主页面
function showMainSection() {
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('main-section').style.display = 'block';
    document.getElementById('current-username').textContent = currentUser.username;
    // 显示用户状态
    updateUserStatus(currentUser.status);
}

// 更新用户状态显示
function updateUserStatus(status) {
    const statusElement = document.getElementById('user-status');
    if (!statusElement) return;
    
    // 移除所有状态类
    statusElement.classList.remove('offline', 'online', 'moyooing');
    
    // status 可能是 code (数字) 或 description (字符串)
    let statusCode = status;
    if (typeof status === 'string') {
        // 如果是字符串，尝试转换为数字
        const statusMap = {
            '离线': 0,
            'OFFLINE': 0,
            '在线': 1,
            'ONLINE': 1,
            '摸鱼中': 2,
            'MOYOOING': 2
        };
        statusCode = statusMap[status] !== undefined ? statusMap[status] : parseInt(status);
    }
    
    // 根据 code 显示状态
    if (statusCode === 0 || statusCode === '0') {
        statusElement.textContent = '离线';
        statusElement.classList.add('offline');
    } else if (statusCode === 1 || statusCode === '1') {
        statusElement.textContent = '在线';
        statusElement.classList.add('online');
    } else if (statusCode === 2 || statusCode === '2') {
        statusElement.textContent = '摸鱼中';
        statusElement.classList.add('moyooing');
    } else {
        statusElement.textContent = '离线';
        statusElement.classList.add('offline');
    }
}

// 退出登录（确保在全局作用域）
window.logout = async function() {
    // 如果有用户信息，调用后端接口更新状态
    if (currentUser && currentUser.userId) {
        try {
            await fetch(`${API_BASE}/user/logout?userId=${currentUser.userId}`, {
                method: 'POST'
            });
        } catch (error) {
            console.error('退出登录时更新状态失败:', error);
        }
    }
    
    // 清除前端状态
    currentUser = null;
    localStorage.removeItem('currentUser');
    document.getElementById('auth-section').style.display = 'block';
    document.getElementById('main-section').style.display = 'none';
    if (moyooTimer) {
        clearInterval(moyooTimer);
        moyooTimer = null;
    }
}

// 强制退出登录（用于登录态过期等情况）
async function forceLogout() {
    // 如果有用户信息，调用后端接口强制更新状态
    if (currentUser && currentUser.userId) {
        try {
            await fetch(`${API_BASE}/user/force-logout?userId=${currentUser.userId}`, {
                method: 'POST'
            });
        } catch (error) {
            console.error('强制退出登录时更新状态失败:', error);
        }
    }
    
    // 清除前端状态
    currentUser = null;
    localStorage.removeItem('currentUser');
    document.getElementById('auth-section').style.display = 'block';
    document.getElementById('main-section').style.display = 'none';
    if (moyooTimer) {
        clearInterval(moyooTimer);
        moyooTimer = null;
    }
}

// 开始摸鱼（确保在全局作用域）
window.startMoYoo = async function() {
    // 验证用户信息
    if (!currentUser || !currentUser.userId) {
        showMessage('用户信息异常，请重新登录', 'error');
        logout();
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/start?userId=${currentUser.userId}`, {
            method: 'POST'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('开始摸鱼！', 'success');
            // 重置所有计时相关状态
            isPaused = false;
            pausedStartTime = null;
            totalPausedMs = 0;
            moyooStartTime = new Date(data.data.startTime);
            updateMoYooStatus(true, data.data);
            startTimer();
            // 更新用户状态为摸鱼中
            updateUserStatus(2);
            // 更新当前用户对象
            if (currentUser) {
                currentUser.status = 2;
            }
        } else {
            // 如果是用户不存在错误，提示重新登录并强制退出
            if (data.message && data.message.includes('用户不存在')) {
                showMessage('用户信息已过期，请重新登录', 'error');
                setTimeout(() => {
                    forceLogout();
                }, 2000);
            } else {
                showMessage(data.message || '开始摸鱼失败', 'error');
            }
        }
    } catch (error) {
        showMessage('网络错误：' + error.message, 'error');
    }
}

// 暂停摸鱼（确保在全局作用域）
window.pauseMoYoo = function() {
    isPaused = true;
    pausedStartTime = new Date(); // 记录暂停开始时间
    
    // 停止计时器
    if (moyooTimer) {
        clearInterval(moyooTimer);
        moyooTimer = null;
    }
    
    console.log('暂停摸鱼，暂停开始时间:', pausedStartTime);
    
    // 更新UI：显示结束按钮和继续按钮
    updatePausedStatus();
    showMessage('摸鱼已暂停', 'info');
}

// 继续摸鱼（确保在全局作用域）
window.resumeMoYoo = function() {
    // 计算本次暂停时长并累加
    if (pausedStartTime) {
        const pausedDuration = new Date() - pausedStartTime;
        totalPausedMs += pausedDuration;
        console.log('本次暂停时长(ms):', pausedDuration, '累计暂停时长(ms):', totalPausedMs);
    }
    
    isPaused = false;
    pausedStartTime = null; // 清除暂停开始时间
    
    // 恢复计时器
    startTimerWithOffset();
    
    // 更新UI：显示暂停按钮
    updateRunningStatus();
    showMessage('继续摸鱼！', 'success');
}

// 更新暂停状态UI
function updatePausedStatus() {
    const timerDisplay = document.getElementById('moyoo-timer');
    const startBtn = document.getElementById('start-btn');
    const pauseBtn = document.getElementById('pause-btn');
    const pausedControls = document.getElementById('paused-controls');
    
    // 隐藏开始按钮和暂停按钮
    startBtn.style.display = 'none';
    pauseBtn.style.display = 'none';
    
    // 显示结束按钮和继续按钮
    pausedControls.style.display = 'flex';
    
    // 保持时间显示
    timerDisplay.style.display = 'block';
}

// 更新运行中状态UI
function updateRunningStatus() {
    const timerDisplay = document.getElementById('moyoo-timer');
    const startBtn = document.getElementById('start-btn');
    const pauseBtn = document.getElementById('pause-btn');
    const pausedControls = document.getElementById('paused-controls');
    
    // 隐藏开始按钮和暂停控制按钮组
    startBtn.style.display = 'none';
    pausedControls.style.display = 'none';
    
    // 显示暂停按钮
    pauseBtn.style.display = 'flex';
    
    // 显示时间
    timerDisplay.style.display = 'block';
}

// 开始长按结束按钮
window.startLongPress = function(event) {
    event.preventDefault(); // 阻止默认行为（如文本选择）
    
    const wrapper = document.querySelector('.stop-btn-wrapper');
    if (!wrapper) return;
    
    // 添加按压状态类，触发CSS动画
    wrapper.classList.add('pressing');
    
    // 重置进度环动画
    const progressFill = wrapper.querySelector('.progress-ring-fill');
    if (progressFill) {
        progressFill.style.animation = 'none';
        progressFill.offsetHeight; // 触发重排
        progressFill.style.animation = 'fillProgress 3s linear forwards';
    }
    
    // 设置3秒后触发结束
    longPressTimer = setTimeout(() => {
        wrapper.classList.remove('pressing');
        // 添加完成动画效果
        wrapper.classList.add('completed');
        setTimeout(() => {
            wrapper.classList.remove('completed');
        }, 300);
        
        // 触发结束摸鱼
        endMoYoo();
    }, LONG_PRESS_DURATION);
    
    console.log('开始长按结束按钮');
}

// 取消长按
window.cancelLongPress = function() {
    if (longPressTimer) {
        clearTimeout(longPressTimer);
        longPressTimer = null;
        console.log('取消长按');
    }
    
    const wrapper = document.querySelector('.stop-btn-wrapper');
    if (wrapper) {
        wrapper.classList.remove('pressing');
        
        // 重置进度环
        const progressFill = wrapper.querySelector('.progress-ring-fill');
        if (progressFill) {
            progressFill.style.animation = 'none';
            progressFill.style.strokeDashoffset = '226.2';
        }
    }
}

// 结束摸鱼（确保在全局作用域）
window.endMoYoo = async function() {
    // 验证用户信息
    if (!currentUser || !currentUser.userId) {
        showMessage('用户信息异常，请重新登录', 'error');
        logout();
        return;
    }
    
    try {
        // 先获取当前摸鱼记录，计算时长
        const currentResponse = await fetch(`${API_BASE}/current?userId=${currentUser.userId}`);
        const currentData = await currentResponse.json();
        
        if (!currentData.success || !currentData.data) {
            showMessage('没有进行中的摸鱼记录', 'error');
            return;
        }
        
        const record = currentData.data;
        const startTime = new Date(record.startTime);
        const now = new Date();
        
        // 计算总时长 = (当前时间 - 开始时间) - 累计暂停时长
        let diffMs = now - startTime;
        
        // 减去累计暂停时长
        diffMs -= totalPausedMs;
        
        // 如果当前是暂停状态，还要减去当前暂停的时间
        if (isPaused && pausedStartTime) {
            const currentPausedMs = now - pausedStartTime;
            diffMs -= currentPausedMs;
        }
        
        // 确保时长不为负
        if (diffMs < 0) diffMs = 0;
        
        // 使用浮点数计算时长（保留2位小数）
        const durationMinutes = Math.round((diffMs / 60000) * 100) / 100;
        console.log('结束摸鱼 - 开始时间:', startTime, '当前时间:', now, '累计暂停(ms):', totalPausedMs, '实际时长(分钟):', durationMinutes);
        
        // 提交结束摸鱼请求，包含前端计算的时长
        const response = await fetch(`${API_BASE}/end?userId=${currentUser.userId}&durationMinutes=${durationMinutes}`, {
            method: 'POST'
        });
        
        const data = await response.json();
        
        if (data.success) {
            // 停止计时器
            if (moyooTimer) {
                clearInterval(moyooTimer);
                moyooTimer = null;
            }
            
            // 重置所有状态
            isPaused = false;
            pausedStartTime = null;
            totalPausedMs = 0;
            moyooStartTime = null;
            
            // 1. 先更新用户状态为在线（确保状态同步）
            updateUserStatus(1);
            if (currentUser) {
                currentUser.status = 1;
            }
            
            // 2. 更新UI状态为初始状态
            updateMoYooStatusInitial();
            
            // 3. 显示摸鱼报告
            showMoYooReport(data.data);
            
            showMessage('摸鱼结束！', 'success');
        } else {
            // 如果是用户不存在错误，提示重新登录并强制退出
            if (data.message && data.message.includes('用户不存在')) {
                showMessage('用户信息已过期，请重新登录', 'error');
                setTimeout(() => {
                    forceLogout();
                }, 2000);
            } else {
                showMessage(data.message || '结束摸鱼失败', 'error');
            }
        }
    } catch (error) {
        showMessage('网络错误：' + error.message, 'error');
    }
}

// 检查当前摸鱼状态
async function checkCurrentMoYoo() {
    // 验证用户信息
    if (!currentUser || !currentUser.userId) {
        console.warn('用户信息异常，无法检查摸鱼状态');
        updateMoYooStatus(false, null);
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/current?userId=${currentUser.userId}`);
        const data = await response.json();
        
        if (data.success && data.data) {
            // 重置/恢复状态
            isPaused = false;
            pausedStartTime = null;
            totalPausedMs = 0; // 页面刷新后，暂停时间无法恢复，重置为0
            moyooStartTime = new Date(data.data.startTime);
            updateMoYooStatus(true, data.data);
            startTimer();
        } else {
            updateMoYooStatus(false, null);
        }
    } catch (error) {
        console.error('检查摸鱼状态失败:', error);
        updateMoYooStatus(false, null);
    }
}

// 更新摸鱼状态显示
function updateMoYooStatus(isActive, record) {
    if (isActive && record) {
        // 摸鱼中状态：显示时间计时和暂停按钮
        updateRunningStatus();
        // 更新摸鱼时长显示
        updateDurationDisplay(record);
    } else {
        // 初始状态：显示开始按钮
        updateMoYooStatusInitial();
    }
}

// 更新初始状态UI
function updateMoYooStatusInitial() {
    const timerDisplay = document.getElementById('moyoo-timer');
    const startBtn = document.getElementById('start-btn');
    const pauseBtn = document.getElementById('pause-btn');
    const pausedControls = document.getElementById('paused-controls');
    
    // 隐藏时间显示和所有控制按钮
    timerDisplay.style.display = 'none';
    pauseBtn.style.display = 'none';
    pausedControls.style.display = 'none';
    
    // 显示开始按钮
    startBtn.style.display = 'inline-block';
}

// 更新摸鱼时长显示
function updateDurationDisplay(record) {
    const timerDisplay = document.getElementById('timer-display');
    if (record && record.startTime) {
        const startTime = new Date(record.startTime);
        const now = new Date();
        // 计算实际时长 = (当前时间 - 开始时间) - 累计暂停时长
        let diffMs = now - startTime - totalPausedMs;
        
        // 如果当前是暂停状态，还要减去当前暂停的时间
        if (isPaused && pausedStartTime) {
            diffMs -= (now - pausedStartTime);
        }
        
        if (diffMs < 0) diffMs = 0;
        
        const diffMinutes = Math.floor(diffMs / 60000);
        const diffSeconds = Math.floor((diffMs % 60000) / 1000);
        
        // 格式化显示：MM:SS
        const minutesStr = diffMinutes.toString().padStart(2, '0');
        const secondsStr = diffSeconds.toString().padStart(2, '0');
        timerDisplay.textContent = `${minutesStr}:${secondsStr}`;
    }
}

// 开始计时器（首次开始，从服务器获取开始时间）
function startTimer() {
    // 如果已经存在计时器，不重复创建
    if (moyooTimer) return;
    
    // 验证用户信息
    if (!currentUser || !currentUser.userId) {
        console.warn('用户信息异常，无法启动计时器');
        return;
    }
    
    // 获取开始时间
    fetch(`${API_BASE}/current?userId=${currentUser.userId}`)
        .then(response => response.json())
        .then(data => {
            if (data.success && data.data && data.data.startTime) {
                moyooStartTime = new Date(data.data.startTime);
                console.log('计时器启动，开始时间:', moyooStartTime);
                
                // 开始计时
                runTimer();
            }
        })
        .catch(error => {
            console.error('获取摸鱼记录失败:', error);
        });
}

// 继续计时器（暂停后恢复，使用已保存的开始时间和累计暂停时间）
function startTimerWithOffset() {
    // 清除之前的计时器（如果有）
    if (moyooTimer) {
        clearInterval(moyooTimer);
        moyooTimer = null;
    }
    
    if (!moyooStartTime) {
        console.warn('开始时间异常，重新获取');
        startTimer();
        return;
    }
    
    console.log('继续计时器，开始时间:', moyooStartTime, '累计暂停(ms):', totalPausedMs);
    
    // 开始计时
    runTimer();
}

// 运行计时器（实际的计时逻辑）
function runTimer() {
    // 清除之前的计时器（如果有）
    if (moyooTimer) {
        clearInterval(moyooTimer);
        moyooTimer = null;
    }
    
    moyooTimer = setInterval(() => {
        if (!isPaused && moyooStartTime) {
            const now = new Date();
            // 计算实际摸鱼时长 = (当前时间 - 开始时间) - 累计暂停时长
            let diffMs = now - moyooStartTime - totalPausedMs;
            if (diffMs < 0) diffMs = 0;
            
            const diffMinutes = Math.floor(diffMs / 60000);
            const diffSeconds = Math.floor((diffMs % 60000) / 1000);
            
            // 格式化显示：MM:SS
            const minutesStr = diffMinutes.toString().padStart(2, '0');
            const secondsStr = diffSeconds.toString().padStart(2, '0');
            const timerDisplay = document.getElementById('timer-display');
            if (timerDisplay) {
                timerDisplay.textContent = `${minutesStr}:${secondsStr}`;
            }
        }
    }, 1000); // 每秒更新一次
}

// 更新摸鱼状态为已完成（显示报告提示）- 已废弃，使用updateMoYooStatusInitial替代
function updateMoYooStatusCompleted() {
    updateMoYooStatusInitial();
}

// 显示摸鱼报告
function showMoYooReport(report) {
    // 时长显示保留2位小数
    document.getElementById('report-duration').textContent = report.durationMinutes.toFixed(2);
    document.getElementById('report-carbon').textContent = report.carbonReductionGrams.toFixed(2);
    document.getElementById('report-life').textContent = report.lifeExtensionMinutes.toFixed(2);
    document.getElementById('report-ai').textContent = report.aiReport || '暂无AI报告';
    document.getElementById('moyoo-report').style.display = 'block';
}

// 切换标签页
function showTab(tab, clickedElement) {
    const moyooSection = document.querySelector('.moyoo-card').parentElement;
    const rankingSection = document.getElementById('ranking-section');
    const tabs = document.querySelectorAll('.tab-nav-btn');
    
    tabs.forEach(t => t.classList.remove('active'));
    if (clickedElement) {
        clickedElement.classList.add('active');
    } else {
        // 如果没有传递元素，根据tab参数设置
        tabs.forEach(t => {
            if ((tab === 'moyoo' && t.textContent.includes('摸鱼')) || 
                (tab === 'ranking' && t.textContent.includes('排行'))) {
                t.classList.add('active');
            }
        });
    }
    
    if (tab === 'moyoo') {
        moyooSection.style.display = 'block';
        rankingSection.style.display = 'none';
    } else {
        moyooSection.style.display = 'none';
        rankingSection.style.display = 'block';
        loadRanking('duration', 'week', null);
    }
}

// 加载排行榜
async function loadRanking(type, period, clickedElement) {
    try {
        const response = await fetch(`${API_BASE}/ranking/${type}?period=${period}`);
        const data = await response.json();
        
        if (data.success) {
            displayRanking(data.data, type);
            // 更新按钮状态
            if (clickedElement) {
                document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
                clickedElement.classList.add('active');
            }
        } else {
            showMessage(data.message || '加载排行榜失败', 'error');
        }
    } catch (error) {
        showMessage('网络错误：' + error.message, 'error');
    }
}

// 显示排行榜
function displayRanking(ranking, type) {
    const listDiv = document.getElementById('ranking-list');
    listDiv.innerHTML = '';
    
    if (!ranking || ranking.length === 0) {
        listDiv.innerHTML = '<p style="text-align: center; color: #999; padding: 40px;">暂无数据</p>';
        return;
    }
    
    ranking.forEach((item, index) => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'ranking-item';
        
        const rankClass = index === 0 ? 'top1' : index === 1 ? 'top2' : index === 2 ? 'top3' : '';
        
        let valueText = '';
        if (type === 'duration') {
            // 时长支持小数，保留2位
            valueText = `${item.value.toFixed(2)} 分钟`;
        } else if (type === 'count') {
            // 次数显示为整数
            valueText = `${Math.round(item.value)} 次`;
        } else if (type === 'carbon') {
            valueText = `${item.carbonReduction.toFixed(2)} 克`;
        }
        
        itemDiv.innerHTML = `
            <div class="ranking-rank ${rankClass}">${index + 1}</div>
            <div class="ranking-info">
                <div class="ranking-username">${item.username}</div>
                <div class="ranking-profession">${item.profession || '未知职业'}</div>
            </div>
            <div class="ranking-value">${valueText}</div>
        `;
        
        listDiv.appendChild(itemDiv);
    });
}

// 显示消息提示
function showMessage(message, type) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;
    messageDiv.textContent = message;
    document.body.appendChild(messageDiv);
    
    setTimeout(() => {
        messageDiv.remove();
    }, 3000);
}


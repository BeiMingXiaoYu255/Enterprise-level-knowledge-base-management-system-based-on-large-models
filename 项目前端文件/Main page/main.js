// 添加axios请求拦截器，自动附加认证信息
axios.interceptors.request.use(config => {
    const cookies = {};
    if (document.cookie) {
        document.cookie.split(';').forEach(cookie => {
            const [name, value] = cookie.trim().split('=');
            if (name && value) {
                cookies[name] = decodeURIComponent(value);
                // 打印所有获取到的Cookie，方便调试
                console.log(`发现Cookie - 名称: ${name}, 值: ${value.substring(0, 10)}...`);
            }
        });
    } else {
        console.log("未获取到任何Cookie");
    }
    
    // 为所有请求添加Authorization头
    // 注意：这里的cookie名称必须与后端设置的一致，建议统一使用"jwtToken"
    const tokenCookieName = "TheLifeIsGone"; // 统一Cookie名称
    if (cookies[tokenCookieName]) {
        config.headers.Authorization = `Bearer ${cookies[tokenCookieName]}`;
        console.log("已添加Authorization头");
    } else {
        console.log(`未找到名为${tokenCookieName}的Cookie`);
    }
    
    // 启用跨域请求携带凭据(cookie)
    config.withCredentials = true;
        
    return config;
}, error => {
    return Promise.reject(error);
});

// 初始化设置面板相关功能
document.addEventListener('DOMContentLoaded', function () {
    // 初始化所有设置功能
    initAllSettings();
});

// 统一初始化函数
function initAllSettings() {
    initSettingsPanel();
    initSliders();
    initToggles();
    initFontFamilySelector();
    initAdvancedSettings();
    initSettingsActions();
    loadSavedSettings();
}

// 初始化设置面板显示/隐藏
function initSettingsPanel() {
    const settingsPanel = document.getElementById('settingsPanel');
    const settingsBtn = document.querySelector('.base-control[data-action="settings"]');
    const closeSettingsBtn = document.getElementById('closeSettingsBtn');
    const cancelSettingsBtn = document.getElementById('cancelSettingsBtn');
    const overlay = document.createElement('div');

    // 创建遮罩层
    overlay.className = 'settings-overlay';
    document.body.appendChild(overlay);

    // 打开设置面板
    if (settingsBtn) {
        settingsBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            e.preventDefault();
            settingsPanel.classList.remove('hidden');
            setTimeout(() => {
                settingsPanel.classList.add('active');
                overlay.classList.add('active');
            }, 10);
            document.body.style.overflow = 'hidden';
        });
    }

    // 关闭设置面板的通用函数
    function closePanel() {
        settingsPanel.classList.remove('active');
        overlay.classList.remove('active');
        setTimeout(() => {
            settingsPanel.classList.add('hidden');
            document.body.style.overflow = '';
        }, 300);
    }

    // 关闭按钮
    if (closeSettingsBtn) {
        closeSettingsBtn.addEventListener('click', closePanel);
    }

    // 取消按钮
    if (cancelSettingsBtn) {
        cancelSettingsBtn.addEventListener('click', closePanel);
    }

    // 点击遮罩层关闭
    overlay.addEventListener('click', closePanel);

    // 初始化折叠面板
    document.querySelectorAll('.section-header').forEach(header => {
        header.addEventListener('click', function () {
            const content = this.nextElementSibling;
            const icon = this.querySelector('.section-toggle');
            content.classList.toggle('hidden');
            icon.classList.toggle('rotate');
        });
    });
}

// 初始化滑块控件
function initSliders() {
    const sliders = [
        { id: 'fontSize', unit: 'px', displayId: 'fontSizeValue' },
        { id: 'lineHeight', unit: '', displayId: 'lineHeightValue' },
        { id: 'paragraphSpacing', unit: 'px', displayId: 'paragraphSpacingValue' },
        { id: 'marginSize', unit: 'px', displayId: 'marginSizeValue' }
    ];

    sliders.forEach(slider => {
        const element = document.getElementById(slider.id);
        const display = document.getElementById(slider.displayId);

        if (element && display) {
            // 初始显示值
            display.textContent = `${element.value}${slider.unit}`;

            // 滑动时更新显示
            element.addEventListener('input', function () {
                display.textContent = `${this.value}${slider.unit}`;
            });

            // 滑动结束后应用设置
            element.addEventListener('change', applySettings);
        }
    });
}

// 初始化开关控件
function initToggles() {
    const toggles = [
        'darkMode',
        'showSidebar',
        'enableCache',
        'debugMode',
        'autoHideToolbar',
        'enableAnimations'
    ];

    toggles.forEach(toggleId => {
        const toggle = document.getElementById(toggleId);
        if (toggle) {
            toggle.addEventListener('change', applySettings);
        }
    });
}

// 初始化字体选择器
function initFontFamilySelector() {
    const selector = document.getElementById('fontFamily');
    const display = document.getElementById('fontFamilyValue');

    if (selector && display) {
        // 初始显示
        display.textContent = selector.options[selector.selectedIndex].text;

        // 选择变化时更新
        selector.addEventListener('change', function () {
            display.textContent = this.options[this.selectedIndex].text;
            applySettings();
        });
    }
}

// 初始化高级设置功能
function initAdvancedSettings() {
    // 清除缓存按钮
    const clearCacheBtn = document.getElementById('clearCacheBtn');
    if (clearCacheBtn) {
        clearCacheBtn.addEventListener('click', function () {
            if (confirm('确定要清除所有缓存数据吗？这将无法恢复。')) {
                localStorage.clear();
                sessionStorage.clear();

                // 显示清除成功消息
                const originalText = this.innerHTML;
                this.innerHTML = '<i class="fas fa-check"></i> 已清除';
                this.disabled = true;

                setTimeout(() => {
                    this.innerHTML = originalText;
                    this.disabled = false;
                }, 2000);
            }
        });
    }
}

// 初始化设置操作按钮
function initSettingsActions() {
    // 应用设置按钮
    const applyBtn = document.getElementById('applySettingsBtn');
    if (applyBtn) {
        applyBtn.addEventListener('click', function () {
            applySettings();
            showSettingsToast('设置已应用');

            // 关闭面板
            document.getElementById('closeSettingsBtn').click();
        });
    }

    // 重置默认按钮
    const resetBtn = document.getElementById('resetSettingsBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', function () {
            if (confirm('确定要恢复默认设置吗？所有自定义设置将丢失。')) {
                resetDefaultSettings();
                showSettingsToast('已恢复默认设置');
            }
        });
    }

    // 导入/导出设置已在原有代码基础上完善
}

// 收集当前设置
function collectSettings() {
    return {
        fontSize: document.getElementById('fontSize').value,
        lineHeight: document.getElementById('lineHeight').value,
        paragraphSpacing: document.getElementById('paragraphSpacing').value,
        marginSize: document.getElementById('marginSize').value,
        fontFamily: document.getElementById('fontFamily').value,
        darkMode: document.getElementById('darkMode').checked,
        showSidebar: document.getElementById('showSidebar').checked,
        enableCache: document.getElementById('enableCache').checked,
        debugMode: document.getElementById('debugMode').checked,
        autoHideToolbar: document.getElementById('autoHideToolbar').checked,
        enableAnimations: document.getElementById('enableAnimations').checked
    };
}

// 应用设置到页面
function applySettings() {
    const settings = collectSettings();
    const root = document.documentElement;

    // 应用文本样式
    root.style.setProperty('--font-size', `${settings.fontSize}px`);
    root.style.setProperty('--line-height', settings.lineHeight);
    root.style.setProperty('--paragraph-spacing', `${settings.paragraphSpacing}px`);
    root.style.setProperty('--margin-size', `${settings.marginSize}px`);
    root.style.setProperty('--font-family', settings.fontFamily);

    // 应用深色模式
    document.body.classList.toggle('dark-mode', settings.darkMode);

    // 控制侧边栏显示
    const leftMain = document.querySelector('.left-main');
    if (leftMain) {
        leftMain.style.display = settings.showSidebar ? 'block' : 'none';
    }

    // 自动隐藏工具栏
    const header = document.querySelector('.header');
    if (header) {
        if (settings.autoHideToolbar) {
            let lastScrollTop = 0;
            window.addEventListener('scroll', function () {
                const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                if (scrollTop > lastScrollTop && scrollTop > 100) {
                    header.style.transform = 'translateY(-100%)';
                } else {
                    header.style.transform = 'translateY(0)';
                }
                lastScrollTop = scrollTop;
            });
        } else {
            header.style.transform = 'translateY(0)';
        }
    }

    // 启用/禁用动画效果
    if (settings.enableAnimations) {
        document.documentElement.classList.remove('no-animations');
    } else {
        document.documentElement.classList.add('no-animations');
    }

    // 调试模式
    if (settings.debugMode) {
        console.log('调试模式已启用');
        console.log('当前设置:', settings);
    }

    saveSettings(settings);
}

// 保存设置到本地存储
function saveSettings(settings) {
    localStorage.setItem('appSettings', JSON.stringify(settings));
}

// 加载保存的设置
function loadSavedSettings() {
    const saved = localStorage.getItem('appSettings');
    if (saved) {
        try {
            const settings = JSON.parse(saved);
            applySettingsFromObject(settings);
        } catch (e) {
            console.error('加载设置失败:', e);
            // 加载失败时使用默认设置
            resetDefaultSettings();
        }
    }
}

// 从对象应用设置
function applySettingsFromObject(settings) {
    // 设置滑块值
    document.getElementById('fontSize').value = settings.fontSize || 16;
    document.getElementById('lineHeight').value = settings.lineHeight || 1.5;
    document.getElementById('paragraphSpacing').value = settings.paragraphSpacing || 16;
    document.getElementById('marginSize').value = settings.marginSize || 20;

    // 设置字体选择
    if (settings.fontFamily) {
        document.getElementById('fontFamily').value = settings.fontFamily;
        document.getElementById('fontFamilyValue').textContent =
            document.getElementById('fontFamily').options[document.getElementById('fontFamily').selectedIndex].text;
    }

    // 设置开关状态
    document.getElementById('darkMode').checked = settings.darkMode || false;
    document.getElementById('showSidebar').checked = settings.showSidebar !== undefined ? settings.showSidebar : true;
    document.getElementById('enableCache').checked = settings.enableCache !== undefined ? settings.enableCache : true;
    document.getElementById('debugMode').checked = settings.debugMode || false;
    document.getElementById('autoHideToolbar').checked = settings.autoHideToolbar || false;
    document.getElementById('enableAnimations').checked = settings.enableAnimations !== undefined ? settings.enableAnimations : true;

    // 更新滑块显示值
    document.getElementById('fontSizeValue').textContent = `${settings.fontSize || 16}px`;
    document.getElementById('lineHeightValue').textContent = settings.lineHeight || 1.5;
    document.getElementById('paragraphSpacingValue').textContent = `${settings.paragraphSpacing || 16}px`;
    document.getElementById('marginSizeValue').textContent = `${settings.marginSize || 20}px`;

    // 应用设置
    applySettings();
}

// 重置默认设置
function resetDefaultSettings() {
    const defaultSettings = {
        fontSize: 16,
        lineHeight: 1.5,
        paragraphSpacing: 16,
        marginSize: 20,
        fontFamily: 'system-ui',
        darkMode: false,
        showSidebar: true,
        enableCache: true,
        debugMode: false,
        autoHideToolbar: false,
        enableAnimations: true
    };

    applySettingsFromObject(defaultSettings);
}

// 显示设置提示消息
function showSettingsToast(message) {
    // 创建提示元素
    let toast = document.querySelector('.settings-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.className = 'settings-toast';
        toast.style.position = 'fixed';
        toast.style.bottom = '20px';
        toast.style.left = '50%';
        toast.style.transform = 'translateX(-50%)';
        toast.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
        toast.style.color = 'white';
        toast.style.padding = '10px 20px';
        toast.style.borderRadius = '4px';
        toast.style.zIndex = '9999';
        toast.style.transition = 'opacity 0.3s ease';
        document.body.appendChild(toast);
    }

    // 显示消息
    toast.textContent = message;
    toast.style.opacity = '1';

    // 3秒后隐藏
    setTimeout(() => {
        toast.style.opacity = '0';
    }, 3000);
}

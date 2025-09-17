const documentContent = document.getElementById('documentContent');
const documentList = document.querySelector('.document-list');

// 解析MD文件内容，提取标题并构建层级结构
function parseMarkdown(content, fileName) {
    const lines = content.split('\n');
    const titles = [];
    const stack = [];
    let maxLevel = 6; // 默认最大标题级别
    let idCounter = 0;

    // 首先扫描确定最大标题级别
    lines.forEach(line => {
    const match = line.match(/^(#{1,6})\s/);
    if (match) {
        const level = match[1].length;
        if (level < maxLevel) {
        maxLevel = level;
        }
    }
    });

    // 构建标题层级结构
    lines.forEach(line => {
    const match = line.match(/^(#{1,6})\s(.*)/);
    if (match) {
        const level = match[1].length;
        const text = match[2].trim();
        const id = `heading-${idCounter++}`;
        const node = { text, level, children: [], expanded: false, id };

        // 根据级别调整stack
        while (stack.length > 0 && stack[stack.length - 1].level >= level) {
        stack.pop();
        }

        if (stack.length === 0) {
        titles.push(node);
        } else {
        stack[stack.length - 1].children.push(node);
        }

        stack.push(node);
    }
    });

    // 如果没有标题，使用文件名作为一级标题
    if (titles.length === 0) {
    titles.push({
        text: fileName.replace('.md', ''),
        level: 1,
        children: [],
        expanded: false
    });
    }

    return { titles, maxLevel };
}

// 渲染标题列表
function renderTitles(data) {
    documentList.innerHTML = '';
    const { titles, maxLevel } = data;

    const renderNode = (node, parentElement, levelDiff) => {
    const element = document.createElement('div');
    element.className = levelDiff === 0 ? 'main-title' : 'sub-title';

    if (node.children.length > 0) {
        element.innerHTML = `
        <span class="toggle-icon">${node.expanded ? '▼' : '▶'}</span>
        <a href="#${node.id}" class="title-link">${node.text}</a>
        `;

        element.addEventListener('click', (e) => {
        // 防止点击图标时触发父元素点击事件
        if (!e.target.classList.contains('toggle-icon')) {
            node.expanded = !node.expanded;
            renderTitles(data);
        }
        });

        parentElement.appendChild(element);

        if (node.children.length > 0) {
        const container = document.createElement('div');
        container.className = 'sub-title-container';
        container.style.display = node.expanded ? 'block' : 'none';

        node.children.forEach(child => {
            renderNode(child, container, levelDiff + (child.level - node.level));
        });

        parentElement.appendChild(container);
        }
    } else {
        const link = document.createElement('a');
        link.href = `#${node.id}`;
        link.className = 'title-link';
        link.textContent = node.text;
        element.appendChild(link);
        parentElement.appendChild(element);
    }
    };

    titles.forEach(title => {
    renderNode(title, documentList, title.level - maxLevel);
    });
}

// 渲染MD内容
function renderMarkdown(content, titles) {
    let idCounter = 0;
    // 增强MD内容渲染
    let html = content
    // 标题
    .replace(/^# (.*$)/gm, (match, p1) => {
        const id = titles[idCounter]?.id || `heading-${idCounter++}`;
        return `<h1 id="${id}">${p1}</h1>`;
    })
    .replace(/^## (.*$)/gm, (match, p1) => {
        const id = titles[idCounter]?.id || `heading-${idCounter++}`;
        return `<h2 id="${id}">${p1}</h2>`;
    })
    .replace(/^### (.*$)/gm, (match, p1) => {
        const id = titles[idCounter]?.id || `heading-${idCounter++}`;
        return `<h3 id="${id}">${p1}</h3>`;
    })
    .replace(/^#### (.*$)/gm, (match, p1) => {
        const id = titles[idCounter]?.id || `heading-${idCounter++}`;
        return `<h4 id="${id}">${p1}</h4>`;
    })
    .replace(/^##### (.*$)/gm, (match, p1) => {
        const id = titles[idCounter]?.id || `heading-${idCounter++}`;
        return `<h5 id="${id}">${p1}</h5>`;
    })
    .replace(/^###### (.*$)/gm, (match, p1) => {
        const id = titles[idCounter]?.id || `heading-${idCounter++}`;
        return `<h6 id="${id}">${p1}</h6>`;
    })

    // 段落和换行
    .replace(/([^\n])\n([^\n])/g, '$1<br>$2')

    // 代码块
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')

    // 强调
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\_\_(.*?)\_\_/g, '<strong>$1</strong>')
    .replace(/\_(.*?)\_/g, '<em>$1</em>')

    // 列表
    .replace(/^\s*\*\s(.*$)/gm, '<li>$1</li>')
    .replace(/^\s*-\s(.*$)/gm, '<li>$1</li>')
    .replace(/^\s*\+\s(.*$)/gm, '<li>$1</li>')
    .replace(/<li>.*<\/li>/g, function (match) {
        return '<ul>' + match + '</ul>';
    })

    // 引用
    .replace(/^\>\s(.*$)/gm, '<blockquote>$1</blockquote>')

    // 分割线
    .replace(/^\-\-\-$/gm, '<hr>');

    // 将连续的相同列表项合并
    html = html.replace(/<\/ul>\s*<ul>/g, '');

    documentContent.innerHTML = html;
}

// 更新顶部栏显示
function updateTopBar(title) {
    const topBarText = document.querySelector('.top-bar .text');
    if (topBarText && title) {
    topBarText.textContent = title;
    }
}

// 文件读取和处理
const fileInput = document.getElementById('fileInput');

function readAndDisplayFile() {
    fileInput.click();

    fileInput.addEventListener('change', function (event) {
    const file = event.target.files[0];
    if (file && file.name.endsWith('.md')) {
        const reader = new FileReader();
        reader.onload = function (e) {
        const content = e.target.result;
        const { titles, maxLevel } = parseMarkdown(content, file.name);
        updateTopBar(titles[0].text); // 使用第一个标题更新顶部栏
        renderTitles({ titles, maxLevel });
        renderMarkdown(content, titles);

        // 保存当前文件信息
        currentFile = {
            name: file.name,
            content: content,
            titles: titles
        };

        // 添加平滑滚动效果
        document.querySelectorAll('.title-link').forEach(link => {
            link.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
                });
            }
            });
        });
        };
        reader.readAsText(file);
    } else {
        alert('请选择.md文件');
    }
    });
}

// 编辑状态变量
let currentFile = null;
let originalContent = '';
let isEditMode = false;

// 切换编辑模式
function toggleEditMode() {
    isEditMode = !isEditMode;

    if (isEditMode) {
    // 进入编辑模式
    document.getElementById('editBtn').style.display = 'none';
    document.getElementById('saveBtn').style.display = 'inline-block';

    // 保存原始内容并显示
    originalContent = currentFile ? currentFile.content : documentContent.textContent;
    const textarea = document.createElement('textarea');
    textarea.id = 'mdEditor';
    textarea.value = originalContent;
    textarea.style.width = '100%';
    textarea.style.height = '100%';
    textarea.style.minHeight = '500px';
    documentContent.innerHTML = '';
    documentContent.appendChild(textarea);
    } else {
    // 退出编辑模式
    document.getElementById('editBtn').style.display = 'inline-block';
    document.getElementById('saveBtn').style.display = 'none';

    // 恢复渲染内容
    const editedContent = document.getElementById('mdEditor').value;
    if (currentFile) {
        currentFile.content = editedContent;
        const { titles, maxLevel } = parseMarkdown(editedContent, currentFile.name);
        currentFile.titles = titles;
        renderTitles({ titles, maxLevel });
        renderMarkdown(editedContent, titles);
    } else {
        const { titles, maxLevel } = parseMarkdown(editedContent, "未命名文档");
        renderTitles({ titles, maxLevel });
        renderMarkdown(editedContent, titles);
    }
    }
}

// 保存文件
function saveFile() {
    if (!currentFile) {
    alert('没有文件可保存，请先加载文件');
    return;
    }

    const editedContent = isEditMode ? document.getElementById('mdEditor').value : currentFile.content;
    
    // 创建下载链接
    const blob = new Blob([editedContent], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = currentFile.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    alert('文件已保存');

    // 更新当前文件内容
    currentFile.content = editedContent;

    // 如果在编辑模式，退出编辑模式
    if (isEditMode) {
    toggleEditMode();
    }
}

// 侧边栏显示/隐藏控制
const toggleSidebarBtn = document.getElementById('toggleSidebarBtn');
const sidebarContainer = document.querySelector('.sidebar-container');

toggleSidebarBtn.addEventListener('click', function () {
    sidebarContainer.classList.toggle('hidden');
    this.innerHTML = sidebarContainer.classList.contains('hidden') ? 
    '<i class="fas fa-bars"></i>显示侧边栏' : 
    '<i class="fas fa-bars"></i>隐藏侧边栏';
});

// ======================================
// 设置面板核心功能 - 完整代码
// ======================================

// 1. 全局元素获取
const settingsPanel = document.getElementById('settingsPanel');
const closeSettingsBtn = document.getElementById('closeSettingsBtn');
const applySettingsBtn = document.getElementById('applySettingsBtn');
const resetSettingsBtn = document.getElementById('resetSettingsBtn');
const settingsBtn = document.getElementById('settingsBtn');

// 2. 分组折叠/展开功能（控制设置面板分组显示）
function toggleGroup(header) {
    const content = header.nextElementSibling;
    const toggleIcon = header.querySelector('.group-toggle');
    content.classList.toggle('collapsed');
    toggleIcon.classList.toggle('closed');
}

// 3. 字体选择显示更新（同步下拉框选择与显示文本）
function updateFontFamilyDisplay() {
    const select = document.getElementById('fontFamily');
    const valueSpan = document.getElementById('fontFamilyValue');
    const options = select.options;
    valueSpan.textContent = options[options.selectedIndex].text;
}

// 4. 实时更新滑块值显示（所有滑块通用逻辑）
function initSliderRealTimeUpdate() {
    // 字体大小滑块
    document.getElementById('fontSize').addEventListener('input', (e) => {
        document.getElementById('fontSizeValue').textContent = `${e.target.value}px`;
    });

    // 行间距滑块
    document.getElementById('lineHeight').addEventListener('input', (e) => {
        document.getElementById('lineHeightValue').textContent = e.target.value;
    });

    // 段间距滑块
    document.getElementById('paragraphSpacing').addEventListener('input', (e) => {
        document.getElementById('paragraphSpacingValue').textContent = `${e.target.value}px`;
    });

    // 页边距滑块
    document.getElementById('marginSize').addEventListener('input', (e) => {
        document.getElementById('marginSizeValue').textContent = `${e.target.value}px`;
    });

    // 粒子数量滑块
    document.getElementById('particleCount').addEventListener('input', (e) => {
        document.getElementById('particleCountValue').textContent = e.target.value;
    });

    // Tab缩进大小滑块
    document.getElementById('tabSize').addEventListener('input', (e) => {
        document.getElementById('tabSizeValue').textContent = e.target.value;
    });
}

// 5. 弹窗控制（导入/导出弹窗的显示隐藏）
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('active');
}
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
}

// 6. 导出设置功能（将当前设置转为JSON并提供复制）
function initExportSettings() {
    const exportBtn = document.getElementById('exportSettingsBtn');
    exportBtn.addEventListener('click', () => {
        // 收集当前所有设置项
        const currentSettings = {
            fontSize: document.getElementById('fontSize').value,
            lineHeight: document.getElementById('lineHeight').value,
            paragraphSpacing: document.getElementById('paragraphSpacing').value,
            marginSize: document.getElementById('marginSize').value,
            darkMode: document.getElementById('darkMode').checked,
            fontFamily: document.getElementById('fontFamily').value,
            autoSave: document.getElementById('autoSave').checked,
            wordWrap: document.getElementById('wordWrap').checked,
            showLineNumbers: document.getElementById('showLineNumbers').checked,
            codeHighlight: document.getElementById('codeHighlight').checked,
            tabSize: document.getElementById('tabSize').value,
            sidebarAutoExpand: document.getElementById('sidebarAutoExpand').checked,
            searchHighlight: document.getElementById('searchHighlight').checked,
            caseSensitiveSearch: document.getElementById('caseSensitiveSearch').checked,
            smoothScroll: document.getElementById('smoothScroll').checked,
            enableParticles: document.getElementById('enableParticles').checked,
            particleCount: document.getElementById('particleCount').value,
            enableCache: document.getElementById('enableCache').checked,
            debugMode: document.getElementById('debugMode').checked
        };

        // 填充JSON到文本框（格式化显示）
        const exportText = document.getElementById('exportText');
        exportText.value = JSON.stringify(currentSettings, null, 2);
        openModal('exportModal');
    });
}

// 7. 复制导出代码（一键复制JSON设置）
function copyExportText() {
    const textarea = document.getElementById('exportText');
    textarea.select();
    document.execCommand('copy'); // 复制到剪贴板
    alert('设置代码已复制到剪贴板，可在其他设备导入使用！');
}

// 8. 导入设置功能（解析JSON并应用到设置面板）
function initImportSettings() {
    const importBtn = document.getElementById('importSettingsBtn');
    importBtn.addEventListener('click', () => {
        openModal('importModal');
    });
}
function importSettings() {
    const importText = document.getElementById('importText').value.trim();
    if (!importText) {
        alert('请先粘贴从其他设备导出的设置JSON代码！');
        return;
    }

    try {
        // 解析JSON（容错处理）
        const importedSettings = JSON.parse(importText);

        // 应用导入的设置到控件
        document.getElementById('fontSize').value = importedSettings.fontSize || 16;
        document.getElementById('fontSizeValue').textContent = `${importedSettings.fontSize || 16}px`;
        
        document.getElementById('lineHeight').value = importedSettings.lineHeight || 1.5;
        document.getElementById('lineHeightValue').textContent = importedSettings.lineHeight || 1.5;
        
        document.getElementById('paragraphSpacing').value = importedSettings.paragraphSpacing || 15;
        document.getElementById('paragraphSpacingValue').textContent = `${importedSettings.paragraphSpacing || 15}px`;
        
        document.getElementById('marginSize').value = importedSettings.marginSize || 20;
        document.getElementById('marginSizeValue').textContent = `${importedSettings.marginSize || 20}px`;
        
        document.getElementById('darkMode').checked = importedSettings.darkMode || false;
        document.getElementById('fontFamily').value = importedSettings.fontFamily || "Arial, sans-serif, 'Segoe UI', 'Microsoft YaHei', sans-serif";
        updateFontFamilyDisplay(); // 同步字体显示
        
        document.getElementById('autoSave').checked = importedSettings.autoSave !== undefined ? importedSettings.autoSave : true;
        document.getElementById('wordWrap').checked = importedSettings.wordWrap !== undefined ? importedSettings.wordWrap : true;
        document.getElementById('showLineNumbers').checked = importedSettings.showLineNumbers || false;
        document.getElementById('codeHighlight').checked = importedSettings.codeHighlight !== undefined ? importedSettings.codeHighlight : true;
        
        document.getElementById('tabSize').value = importedSettings.tabSize || 4;
        document.getElementById('tabSizeValue').textContent = importedSettings.tabSize || 4;
        
        document.getElementById('sidebarAutoExpand').checked = importedSettings.sidebarAutoExpand || false;
        document.getElementById('searchHighlight').checked = importedSettings.searchHighlight !== undefined ? importedSettings.searchHighlight : true;
        document.getElementById('caseSensitiveSearch').checked = importedSettings.caseSensitiveSearch || false;
        document.getElementById('smoothScroll').checked = importedSettings.smoothScroll !== undefined ? importedSettings.smoothScroll : true;
        
        document.getElementById('enableParticles').checked = importedSettings.enableParticles !== undefined ? importedSettings.enableParticles : true;
        document.getElementById('particleCount').value = importedSettings.particleCount || 50;
        document.getElementById('particleCountValue').textContent = importedSettings.particleCount || 50;
        
        document.getElementById('enableCache').checked = importedSettings.enableCache !== undefined ? importedSettings.enableCache : true;
        document.getElementById('debugMode').checked = importedSettings.debugMode || false;

        // 关闭弹窗并提示
        closeModal('importModal');
        alert('设置导入成功！请点击「应用设置」使修改生效');
    } catch (e) {
        alert('导入失败：JSON格式错误，请检查复制的代码是否完整！');
        console.error('导入设置解析错误：', e);
    }
}

// 9. 清除缓存功能（一键删除本地存储的设置与历史）
function initClearCache() {
    const clearBtn = document.getElementById('clearCacheBtn');
    clearBtn.addEventListener('click', () => {
        if (confirm('确定要清除所有本地缓存吗？（包括设置、历史记录等，不可恢复）')) {
            localStorage.clear(); // 清除本地存储
            sessionStorage.clear(); // 清除会话存储
            alert('缓存已清除！请刷新页面使修改生效');
        }
    });
}

// 10. 应用设置（将面板设置应用到页面并保存到本地）
function initApplySettings() {
    applySettingsBtn.addEventListener('click', () => {
        // 1. 读取所有设置项的值
        const fontSize = document.getElementById('fontSize').value;
        const lineHeight = document.getElementById('lineHeight').value;
        const paragraphSpacing = document.getElementById('paragraphSpacing').value;
        const marginSize = document.getElementById('marginSize').value;
        const darkMode = document.getElementById('darkMode').checked;
        const fontFamily = document.getElementById('fontFamily').value;
        const tabSize = document.getElementById('tabSize').value;
        const enableParticles = document.getElementById('enableParticles').checked;
        const particleCount = document.getElementById('particleCount').value;
        const debugMode = document.getElementById('debugMode').checked;

        // 2. 应用「显示设置」到页面样式
        document.documentElement.style.setProperty('--font-size', `${fontSize}px`);
        document.documentElement.style.setProperty('--line-height', lineHeight);
        document.documentElement.style.setProperty('--paragraph-spacing', `${paragraphSpacing}px`);
        document.documentElement.style.setProperty('--margin-size', `${marginSize}px`);
        document.body.style.fontFamily = fontFamily; // 应用字体
        darkMode ? document.body.classList.add('dark-mode') : document.body.classList.remove('dark-mode');

        // 3. 应用「编辑器设置」（若处于编辑模式）
        const editor = document.getElementById('mdEditor');
        if (editor) {
            editor.style.tabSize = tabSize; // 缩进大小
            editor.style.wordWrap = document.getElementById('wordWrap').checked ? 'break-word' : 'normal'; // 自动换行
        }

        // 4. 应用「粒子效果设置」（控制背景粒子显示）
        if (window.config) {
            window.config.particleCount = particleCount; // 更新粒子数量
            const particlesContainer = document.querySelector('.particles-container');
            if (particlesContainer) {
                particlesContainer.style.display = enableParticles ? 'block' : 'none';
            }
        }

        // 5. 应用「调试模式」（打印设置到控制台）
        if (debugMode) {
            console.log('=== 调试模式 - 当前设置 ===');
            console.log({
                fontSize, lineHeight, paragraphSpacing, marginSize, darkMode, fontFamily
            });
        }

        // 6. 保存所有设置到本地存储（持久化）
        localStorage.setItem('appSettings', JSON.stringify({
            fontSize,
            lineHeight,
            paragraphSpacing,
            marginSize,
            darkMode,
            fontFamily,
            autoSave: document.getElementById('autoSave').checked,
            wordWrap: document.getElementById('wordWrap').checked,
            showLineNumbers: document.getElementById('showLineNumbers').checked,
            codeHighlight: document.getElementById('codeHighlight').checked,
            tabSize,
            sidebarAutoExpand: document.getElementById('sidebarAutoExpand').checked,
            searchHighlight: document.getElementById('searchHighlight').checked,
            caseSensitiveSearch: document.getElementById('caseSensitiveSearch').checked,
            smoothScroll: document.getElementById('smoothScroll').checked,
            enableParticles,
            particleCount,
            enableCache: document.getElementById('enableCache').checked,
            debugMode
        }));

        // 7. 关闭面板并提示
        settingsPanel.style.display = 'none';
        alert('设置已应用！\n- 主题、粒子效果等设置需刷新页面生效\n- 字体、行间距等设置即时生效');
    });
}

// 11. 重置设置（恢复所有默认值并清除本地存储）
function initResetSettings() {
    resetSettingsBtn.addEventListener('click', () => {
        if (!confirm('确定要将所有设置恢复为默认值吗？当前设置将被覆盖！')) {
            return;
        }

        // 1. 重置所有控件到默认值
        document.getElementById('fontSize').value = 16;
        document.getElementById('fontSizeValue').textContent = '16px';
        
        document.getElementById('lineHeight').value = 1.5;
        document.getElementById('lineHeightValue').textContent = '1.5';
        
        document.getElementById('paragraphSpacing').value = 15;
        document.getElementById('paragraphSpacingValue').textContent = '15px';
        
        document.getElementById('marginSize').value = 20;
        document.getElementById('marginSizeValue').textContent = '20px';
        
        document.getElementById('darkMode').checked = false;
        document.getElementById('fontFamily').value = "Arial, sans-serif, 'Segoe UI', 'Microsoft YaHei', sans-serif";
        updateFontFamilyDisplay();
        
        document.getElementById('autoSave').checked = true;
        document.getElementById('wordWrap').checked = true;
        document.getElementById('showLineNumbers').checked = false;
        document.getElementById('codeHighlight').checked = true;
        
        document.getElementById('tabSize').value = 4;
        document.getElementById('tabSizeValue').textContent = '4';
        
        document.getElementById('sidebarAutoExpand').checked = false;
        document.getElementById('searchHighlight').checked = true;
        document.getElementById('caseSensitiveSearch').checked = false;
        document.getElementById('smoothScroll').checked = true;
        
        document.getElementById('enableParticles').checked = true;
        document.getElementById('particleCount').value = 50;
        document.getElementById('particleCountValue').textContent = '50';
        
        document.getElementById('enableCache').checked = true;
        document.getElementById('debugMode').checked = false;

        // 2. 重置页面样式到默认
        document.documentElement.style.removeProperty('--font-size');
        document.documentElement.style.removeProperty('--line-height');
        document.documentElement.style.removeProperty('--paragraph-spacing');
        document.documentElement.style.removeProperty('--margin-size');
        document.body.style.fontFamily = '';
        document.body.classList.remove('dark-mode');

        // 3. 重置粒子效果
        if (window.config) {
            window.config.particleCount = 50;
            const particlesContainer = document.querySelector('.particles-container');
            if (particlesContainer) particlesContainer.style.display = 'block';
        }

        // 4. 清除本地存储的设置
        localStorage.removeItem('appSettings');
        alert('所有设置已恢复为默认值！');
    });
}

// 12. 加载本地保存的设置（页面初始化时执行）
function loadSavedSettings() {
    const savedSettings = localStorage.getItem('appSettings');
    if (!savedSettings) return; // 无保存设置则跳过

    try {
        const settings = JSON.parse(savedSettings);

        // 1. 恢复「显示设置」控件
        document.getElementById('fontSize').value = settings.fontSize;
        document.getElementById('fontSizeValue').textContent = `${settings.fontSize}px`;
        
        document.getElementById('lineHeight').value = settings.lineHeight;
        document.getElementById('lineHeightValue').textContent = settings.lineHeight;
        
        document.getElementById('paragraphSpacing').value = settings.paragraphSpacing;
        document.getElementById('paragraphSpacingValue').textContent = `${settings.paragraphSpacing}px`;
        
        document.getElementById('marginSize').value = settings.marginSize;
        document.getElementById('marginSizeValue').textContent = `${settings.marginSize}px`;
        
        document.getElementById('darkMode').checked = settings.darkMode;
        document.getElementById('fontFamily').value = settings.fontFamily || "Arial, sans-serif, 'Segoe UI', 'Microsoft YaHei', sans-serif";
        updateFontFamilyDisplay();

        // 2. 恢复「编辑器设置」控件
        document.getElementById('autoSave').checked = settings.autoSave !== undefined ? settings.autoSave : true;
        document.getElementById('wordWrap').checked = settings.wordWrap !== undefined ? settings.wordWrap : true;
        document.getElementById('showLineNumbers').checked = settings.showLineNumbers || false;
        document.getElementById('codeHighlight').checked = settings.codeHighlight !== undefined ? settings.codeHighlight : true;
        
        document.getElementById('tabSize').value = settings.tabSize || 4;
        document.getElementById('tabSizeValue').textContent = settings.tabSize || 4;

        // 3. 恢复「导航与搜索」控件
        document.getElementById('sidebarAutoExpand').checked = settings.sidebarAutoExpand || false;
        document.getElementById('searchHighlight').checked = settings.searchHighlight !== undefined ? settings.searchHighlight : true;
        document.getElementById('caseSensitiveSearch').checked = settings.caseSensitiveSearch || false;
        document.getElementById('smoothScroll').checked = settings.smoothScroll !== undefined ? settings.smoothScroll : true;

        // 4. 恢复「高级设置」控件
        document.getElementById('enableParticles').checked = settings.enableParticles !== undefined ? settings.enableParticles : true;
        document.getElementById('particleCount').value = settings.particleCount || 50;
        document.getElementById('particleCountValue').textContent = settings.particleCount || 50;
        
        document.getElementById('enableCache').checked = settings.enableCache !== undefined ? settings.enableCache : true;
        document.getElementById('debugMode').checked = settings.debugMode || false;

        // 5. 应用恢复的样式到页面
        document.documentElement.style.setProperty('--font-size', `${settings.fontSize}px`);
        document.documentElement.style.setProperty('--line-height', settings.lineHeight);
        document.documentElement.style.setProperty('--paragraph-spacing', `${settings.paragraphSpacing}px`);
        document.documentElement.style.setProperty('--margin-size', `${settings.marginSize}px`);
        document.body.style.fontFamily = settings.fontFamily || "";
        
        if (settings.darkMode) {
            document.body.classList.add('dark-mode');
        }

        // 6. 应用粒子效果设置
        if (window.config) {
            window.config.particleCount = settings.particleCount || 50;
            const particlesContainer = document.querySelector('.particles-container');
            if (particlesContainer && !settings.enableParticles) {
                particlesContainer.style.display = 'none';
            }
        }

        // 7. 应用编辑器样式
        const editor = document.getElementById('mdEditor');
        if (editor) {
            editor.style.tabSize = settings.tabSize || 4;
            editor.style.wordWrap = settings.wordWrap !== undefined ? (settings.wordWrap ? 'break-word' : 'normal') : 'break-word';
        }
    } catch (e) {
        console.error('加载保存的设置失败，已自动清除错误数据：', e);
        localStorage.removeItem('appSettings');
    }
}

// 13. 初始化设置面板所有功能（入口函数）
function initSettingsPanel() {
    // 基础交互：显示/隐藏设置面板
    settingsBtn.addEventListener('click', () => {
        settingsPanel.style.display = 'block';
    });
    closeSettingsBtn.addEventListener('click', () => {
        settingsPanel.style.display = 'none';
    });

    // 初始化子功能模块
    initSliderRealTimeUpdate(); // 滑块实时更新
    initExportSettings(); // 导出设置
    initImportSettings(); // 导入设置
    initClearCache(); // 清除缓存
    initApplySettings(); // 应用设置
    initResetSettings(); // 重置设置

    // 页面加载时恢复保存的设置
    loadSavedSettings();

    // 绑定弹窗关闭按钮事件
    document.querySelectorAll('.modal-close').forEach(btn => {
        btn.addEventListener('click', function () {
            const modalId = this.closest('.settings-modal').id;
            closeModal(modalId);
        });
    });

    // 绑定导入确认按钮事件
    document.querySelector('.modal-confirm[onclick="importSettings()"]').addEventListener('click', importSettings);
    // 绑定复制导出代码按钮事件
    document.querySelector('.modal-confirm[onclick="copyExportText()"]').addEventListener('click', copyExportText);
    // 绑定弹窗取消按钮事件
    document.querySelectorAll('.modal-cancel').forEach(btn => {
        btn.addEventListener('click', function () {
            const modalId = this.closest('.settings-modal').id;
            closeModal(modalId);
        });
    });
}

// 14. 页面加载完成后初始化设置面板
document.addEventListener('DOMContentLoaded', function () {
    initSettingsPanel();
});

// 专注模式控制
const focusModeBtn = document.getElementById('focusModeBtn');
const focusModeContainer = document.getElementById('focusModeContainer');
const focusContent = document.getElementById('focusContent');
const exitFocusModeBtn = document.getElementById('exitFocusModeBtn');

// 进入专注模式
focusModeBtn.addEventListener('click', () => {
    // 复制当前内容到专注模式区域
    focusContent.innerHTML = documentContent.innerHTML;

    // 应用当前字体设置
    const savedSettings = localStorage.getItem('appSettings');
    if (savedSettings) {
    const settings = JSON.parse(savedSettings);
    focusContent.style.fontSize = `${settings.fontSize}px`;
    focusContent.style.lineHeight = settings.lineHeight;
    }

    // 显示专注模式
    focusModeContainer.classList.add('active');
});

// 退出专注模式
exitFocusModeBtn.addEventListener('click', () => {
    // 隐藏专注模式
    focusModeContainer.classList.remove('active');
});

// 退出专注模式
function exitFocusMode() {
    const focusModeContainer = document.getElementById('focusModeContainer');
    focusModeContainer.classList.remove('active');
}

// 在页面加载完成后绑定退出专注模式按钮事件
document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('exitFocusModeBtn').addEventListener('click', exitFocusMode);
});

const loadFileBtn = document.getElementById('loadFileBtn');
const editBtn = document.getElementById('editBtn');
const saveBtn = document.getElementById('saveBtn');

loadFileBtn.addEventListener('click', readAndDisplayFile);
editBtn.addEventListener('click', toggleEditMode);
saveBtn.addEventListener('click', saveFile);

// 文本聊天框控制
// 搜索相关元素
const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');
const searchResults = document.getElementById('searchResults');
const textChatPanel = document.getElementById('textChatPanel');
const closeChatBtn = document.getElementById('closeChatBtn');
const selectedTextArea = document.getElementById('selectedTextArea');
const chatResponseArea = document.getElementById('chatResponseArea');
const chatInput = document.getElementById('chatInput');
const submitChatBtn = document.getElementById('submitChatBtn');

// 执行搜索
function performSearch() {
    const keyword = searchInput.value.trim();
    if (!keyword) return;

    const content = documentContent.textContent;
    const lines = content.split('\n');
    const results = [];

    lines.forEach((line, index) => {
    if (line.includes(keyword)) {
        results.push({
        lineNumber: index + 1,
        text: line,
        position: getLinePosition(index)
        });
    }
    });

    displaySearchResults(results, keyword);
}

// 获取行的位置
function getLinePosition(lineIndex) {
    const content = documentContent.textContent;
    const lines = content.split('\n');
    let position = 0;

    for (let i = 0; i < lineIndex; i++) {
    position += lines[i].length + 1; // +1 for newline character
    }

    return position;
}

// 显示搜索结果
function displaySearchResults(results, keyword) {
    if (results.length === 0) {
    searchResults.innerHTML = '<div class="no-results">未找到匹配结果</div>';
    return;
    }

    let html = '<div class="results-count">找到 ' + results.length + ' 个结果</div>';

    results.forEach(result => {
    const highlightedText = result.text.replace(
        new RegExp(keyword, 'gi'),
        match => `<span class="highlight">${match}</span>`
    );

    html += `
        <div class="result-item" data-position="${result.position}">
        <div class="line-number">第 ${result.lineNumber} 行</div>
        <div class="result-text">${highlightedText}</div>
        </div>
    `;
    });

    searchResults.innerHTML = html;

    // 添加点击事件
    document.querySelectorAll('.result-item').forEach(item => {
    item.addEventListener('click', () => {
        const position = parseInt(item.getAttribute('data-position'));
        highlightAndScroll(position, keyword);
    });
    });
}

// 高亮并滚动到指定位置
function highlightAndScroll(position, keyword) {
    const content = documentContent.textContent;
    const regex = new RegExp(keyword, 'gi');
    const match = regex.exec(content.substr(position));

    if (match) {
    const range = document.createRange();
    const startNode = documentContent.firstChild;

    range.setStart(startNode, position);
    range.setEnd(startNode, position + match[0].length);

    // 移除之前的高亮
    const highlights = document.querySelectorAll('.search-highlight');
    highlights.forEach(hl => hl.outerHTML = hl.innerHTML);

    // 添加新高亮
    const span = document.createElement('span');
    span.className = 'search-highlight';
    range.surroundContents(span);

    // 滚动到高亮位置
    span.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
}

// 搜索功能初始化
function initSearch() {
    const searchBtn = document.getElementById('searchBtn');
    const searchBox = document.querySelector('.search-box');
    const searchInput = document.getElementById('searchInput');
    const searchResults = document.getElementById('searchResults');

    // 确保只绑定一次事件
    searchBtn.onclick = null;
    searchBtn.addEventListener('click', function (e) {
    e.stopPropagation();

    // 切换搜索框状态
    const isActive = searchBox.classList.toggle('active');

    if (isActive) {
        // 激活状态
        searchInput.style.display = 'block';
        searchInput.focus();
    } else {
        // 非激活状态
        searchInput.style.display = 'none';
        searchResults.classList.remove('active');
    }
    });

    // 输入框回车搜索
    searchInput.addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
        performSearch();
    }
    });
}
document.addEventListener('DOMContentLoaded', function () {
    initSearch();
});

// 执行搜索
function performSearch() {
    const keyword = searchInput.value.trim();
    if (!keyword) return;

    const content = documentContent.textContent;
    const lines = content.split('\n');
    const results = [];

    lines.forEach((line, index) => {
    if (line.includes(keyword)) {
        results.push({
        lineNumber: index + 1,
        text: line,
        position: getLinePosition(index)
        });
    }
    });

    // 显示搜索结果
    searchResults.classList.add('active');
    displaySearchResults(results, keyword);
}

// 监听文本选中事件
document.addEventListener('selectionchange', function () {
    const selection = window.getSelection();
    const selectedText = selection.toString().trim();

    if (selectedText.length > 0) {
    // 获取选中文本的位置
    const range = selection.getRangeAt(0);
    const rect = range.getBoundingClientRect();

    // 设置聊天框位置
    textChatPanel.style.top = `${rect.bottom + window.scrollY}px`;
    textChatPanel.style.left = `${rect.left + window.scrollX}px`;

    selectedTextArea.textContent = selectedText;
    textChatPanel.classList.add('active');
    } else {
    // 没有选中文本时关闭聊天框
    textChatPanel.classList.remove('active');
    }
});

// 关闭聊天框
closeChatBtn.addEventListener('click', function () {
    textChatPanel.classList.remove('active');
});

// 处理窗口滚动和大小变化
window.addEventListener('scroll', updateChatPanelPosition);
window.addEventListener('resize', updateChatPanelPosition);

function updateChatPanelPosition() {
    const selection = window.getSelection();
    const selectedText = selection.toString().trim();

    if (selectedText.length > 0 && textChatPanel.classList.contains('active')) {
    const range = selection.getRangeAt(0);
    const rect = range.getBoundingClientRect();

    textChatPanel.style.top = `${rect.bottom + window.scrollY}px`;
    textChatPanel.style.left = `${rect.left + window.scrollX}px`;
    }
}

// 提交问题
submitChatBtn.addEventListener('click', function () {
    if (chatInput.value.trim()) {
    chatResponseArea.textContent = "模型未连接";
    chatInput.value = '';
    }
});

// 回车键提交
chatInput.addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
    submitChatBtn.click();
    }
});

// AI聊天功能实现
const aiChatMessages = document.querySelector('.ai-chat-messages');
const aiChatInput = document.querySelector('.ai-chat-input textarea');
const aiSendBtn = document.querySelector('.ai-chat-input .send-btn');

// 添加消息到聊天区域
function addMessage(content, isUser = false) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user-message' : 'ai-message'}`;
    
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = content;
    
    const timeDiv = document.createElement('div');
    timeDiv.className = 'message-time';
    timeDiv.textContent = '刚刚';
    
    messageDiv.appendChild(contentDiv);
    messageDiv.appendChild(timeDiv);
    aiChatMessages.appendChild(messageDiv);
    
    // 滚动到底部
    aiChatMessages.scrollTop = aiChatMessages.scrollHeight;
}

// 发送消息到AI
async function sendMessageToAI(message) {
    try {
        addMessage(message, true);
        aiChatInput.value = '';
        
        // 使用fetch API实现SSE客户端
        const response = await fetch(`http://localhost:8080/ai/chat?message=${encodeURIComponent(message)}`, {
            headers: {
                'Accept': 'text/html;charset=utf-8'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        if (!response.body) {
            throw new Error('ReadableStream not supported in this browser');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let fullResponse = '';
        
        const processStream = async () => {
            try {
                while (true) {
                    const { done, value } = await reader.read();
                    if (done) {
                        // 流结束
                        return;
                    }
                    
                    const text = decoder.decode(value, { stream: true });
                    if (text) {
                        fullResponse += text;
                        // 更新最后一条消息内容
                        const messages = aiChatMessages.querySelectorAll('.message');
                        const lastMessage = messages[messages.length - 1];
                        if (lastMessage && !lastMessage.classList.contains('user-message')) {
                            lastMessage.querySelector('.message-content').textContent = fullResponse;
                        } else {
                            addMessage(text);
                        }
                        aiChatMessages.scrollTop = aiChatMessages.scrollHeight;
                    }
                }
            } catch (error) {
                console.error('Stream reading error:', error);
                if (!fullResponse) {
                    addMessage('抱歉，暂时无法连接到AI服务');
                }
            } finally {
                reader.releaseLock();
            }
        };

        processStream();
    } catch (error) {
        addMessage('抱歉，聊天服务出现错误');
        console.error('AI聊天错误:', error);
    }
}

// 初始化AI聊天事件
aiSendBtn.addEventListener('click', () => {
    const message = aiChatInput.value.trim();
    if (message) {
        sendMessageToAI(message);
    }
});

aiChatInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        const message = aiChatInput.value.trim();
        if (message) {
            sendMessageToAI(message);
        }
    }
});

// 初始加载示例文档
document.addEventListener('DOMContentLoaded', function() {
    const exampleContent = `# 欢迎使用知识库管理系统
    
## 系统介绍
这是一个现代化的知识库管理系统，支持Markdown文档的编辑、预览和管理。

### 主要功能
- 文档加载与预览
- 侧边栏导航
- 专注模式阅读
- 个性化显示设置

### 使用说明
1. 点击"加载文件"按钮导入Markdown文档
2. 使用侧边栏导航快速跳转到不同章节
3. 点击"专注模式"获得无干扰阅读体验
4. 使用"设置"调整阅读偏好

\`\`\`javascript
// 示例代码
function welcome() {
console.log("欢迎使用知识库管理系统！");
}
\`\`\`

> 提示：系统支持黑夜模式，可在设置中开启
`;

    const { titles, maxLevel } = parseMarkdown(exampleContent, "示例文档");
    renderTitles({ titles, maxLevel });
    renderMarkdown(exampleContent, titles);
});

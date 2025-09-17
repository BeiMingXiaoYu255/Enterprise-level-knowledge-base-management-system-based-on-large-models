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


new Vue({
            el: '#app',
            data() {
                return {
                    sidebarCollapsed: false,
                    recognition: null,
                    isRecording: false,
                    // 当前活动模块
                    currentModule: 'chat',
                    // 子菜单展开状态
                    submenus: {
                        chat: false,
                        knowledge: false,
                        visit: false,
                        profile: false,
                        permission: false,
                        manual: false
                    },
                    dirOpen: {
                        api: false,
                        dataStructure: false,
                        devEnv: false
                    },
                    // 对话状态
                    isInConversation: false,
                    // 消息列表
                    messages: [],
                    userInput: '',
                    isTyping: false,
                    currentDate: this.getCurrentDate(),
                    isCreateKnowledgeModalVisible: false,
                    newKnowledge: {
                        name: '',
                        creater: '',
                        description: '',
                        documentCount: 0,
                        collaboratorCount: 0,
                        updateTime: '刚刚',
                        tags: [],
                        accessCount: 0,
                        categoryLevel1: '',
                        categoryLevel2: '',
                        fileTypes: '',
                        keywords: ''
                    },
                    createKnowledgeModule: {
                        id: 'create',
                        isCreate: true,
                        name: '创建新知识库',
                        description: '点击创建一个全新的知识库，开始知识管理',
                        tags: ['知识库', '创建'],
                        accessCount: '基础功能'
                    },
                    myKnowledgeList: [],
                    newKnowledgeCount: 0,
                    knowledgeCreated: false,
                    searchKeyword: '',
                    tocExpanded: {
                        intro: true,
                        quickstart: false,
                        knowledge: false,
                        advanced: false
                    },

                    currentDoc: 'intro-overview',
                    devTocExpanded: {
                        environment: true,
                        api: false,
                        'best-practices': false,
                        integration: false,
                        changelog: false
                    },
                    currentDevDoc: 'env-requirements', // 默认显示的开发文档
                    //查找知识库    
                    searchQuery: '',
                    frequentKnowledge: [], // 常用知识库数据
                    allKnowledge: [], // 所有知识库数据
                    categories: [], // 知识库分类数据
                    selectedCategory: 'all',
                    categoryKnowledge: [],
                    // 我的收藏相关属性
                    collectionSearchQuery: '',       // 收藏搜索框的绑定值
                    collectedKnowledge: [],          // 收藏的知识库列表
                    filteredCollections: [],         // 筛选后的收藏列表
                    activeCollectionFilter: 'all',   // 当前激活的收藏筛选条件
                    activeSortType: 'default',       // 当前激活的排序方式

                    // 个人资料数据
                    isEditing: false,
                    emailChanged: false,
                    user: {
                        id: 1001,
                        realName: '果粒橙',
                        userName: 'zhangming123',
                        nickName: '果粒橙',
                        avatar: 'https://randomuser.me/api/portraits/men/32.jpg',
                        gender: '男',
                        email: 'zhang.ming@hss.com',
                        phone: '13800138000',
                        department: '技术研发部',
                        position: '高级前端工程师',
                        role: '技术专家',
                        bio: '专注于前端架构和用户体验设计',
                    },
                    editUser: {},
                    // 新增邮件模块显示状态
                    isMailModuleVisible: false,
                    // 原有的数据内容
                    isFloatingVisible: true,
                    mails: [],
                    isEditKnowledgeModalVisible: false,
                    editKnowledge: {
                        id: '',
                        name: '',
                        creater: '',
                        description: '',
                        categoryLevel1: '',
                        fileTypes: '',
                        keywords: ''
                    },
                    isAddFilesModalVisible: false,
                    currentKnowledge: {},
                    newFileTypes: '',
                    selectedFiles: [],
                    isDragover: false,
                    //用户手册相关数据
                    tocItems: [],
                    tocExpanded: {},
                    currentDoc: '',
                    loading: true,
                    error: null,

                    tocItems: [
                        {
                            id: 'intro',
                            title: '系统介绍',
                            subitems: [
                                // 子项关联MD文件路径（后端可访问的路径标识）
                                { id: 'intro1', docId: 'overview', title: '系统概述', mdPath: 'intro/overview.md' },
                                { id: 'intro2', docId: 'features', title: '功能特点', mdPath: 'intro/features.md' }
                            ]
                        },
                        {
                            id: 'user',
                            title: '用户指南',
                            subitems: [
                                { id: 'user1', docId: 'register', title: '注册与登录', mdPath: 'user/register.md' },
                                { id: 'user2', docId: 'profile', title: '个人资料设置', mdPath: 'user/profile.md' }
                            ]
                        }
                        // 更多目录项...
                    ],
                    tocItems: [],           // 目录结构
                    tocExpanded: {},        // 一级目录展开状态
                    currentDoc: null,       // 当前选中的文档ID
                    currentDocPath: '',     // 当前文档的路径（用于请求）
                    currentDocTitle: '',    // 当前文档标题
                    renderedContent: '',    // 渲染后的HTML内容
                    loading: false,         // 加载状态
                    error: null,            // 错误信息
                    showBackToTop: false,   // 回到顶部按钮显示状态
                    markdownScrollPositions: {}, // 记录各文档的滚动位置
                    practices: {},
                    isMailModuleVisible: false,  // 邮件模块显示状态
                    isHistoryModuleVisible: false,  // 历史记录模块显示状态
                    mails: [],  // 邮件数据
                    histories: []  // 历史记录数据
                }
            },
            computed: {
                formattedMessages() {
                    return this.messages.map(msg => {
                        if (msg.sender === 'ai') {
                            return {
                                ...msg,
                                content: this.parseMarkdown(msg.content)
                            };
                        }
                        return msg;
                    })
                },
                // 过滤后的知识库列表
                filteredKnowledge() {
                    let result = [...this.allKnowledge];

                    // 应用搜索过滤
                    if (this.searchQuery) {
                        const query = this.searchQuery.toLowerCase();
                        result = result.filter(kb =>
                            kb.name.toLowerCase().includes(query) ||
                            kb.description.toLowerCase().includes(query)
                        );
                    }

                    // 应用分类过滤
                    if (this.activeCategory !== 'all') {
                        result = result.filter(kb => kb.category === this.activeCategory);
                    }

                    // 应用状态过滤
                    if (this.statusFilter !== 'all') {
                        result = result.filter(kb => kb.status === this.statusFilter);
                    }

                    // 应用排序
                    if (this.sortOption === 'access') {
                        result.sort((a, b) => b.accessCount - a.accessCount);
                    } else if (this.sortOption === 'date') {
                        result.sort((a, b) => new Date(b.lastUpdated) - new Date(a.lastUpdated));
                    } else if (this.sortOption === 'docs') {
                        result.sort((a, b) => b.documentCount - a.documentCount);
                    }

                    return result;
                }
            },
            mounted() {
                // 页面加载时获取常用知识库、所有知识库和分类数据
                const savedSidebarState = this.getCookie('sidebarCollapsed');
                if (savedSidebarState !== null) {
                    this.sidebarCollapsed = savedSidebarState === 'true';
                }
                this.getFrequentKnowledge();
                this.getAllKnowledge();
                if (this.currentModule === 'knowledge-mine') {
                    this.getMyKnowledgeList();
                }
                if (this.currentModule === 'manual') {
                    this.fetchTOCData();
                }
                if (this.$refs.markdownContent) {
                    this.$refs.markdownContent.addEventListener('scroll', this.handleScroll);
                }

            },
            methods: {
                // ======================================
                // Cookie操作核心方法（新增getAllCookies）
                // ======================================
                setCookie(name, value, days = 1, path = '/', domain = '') {
                    let expires = '';
                    if (days) {
                        const date = new Date();
                        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
                        expires = `; expires=${date.toUTCString()}`;
                    }
                    const domainStr = domain ? `; domain=${domain}` : '';
                    const pathStr = `; path=${path}`;
                    document.cookie = `${name}=${encodeURIComponent(value)}${expires}${domainStr}${pathStr}`;
                },
                getCookie(name) {
                    const nameEQ = `${name}=`;
                    const ca = document.cookie.split(';').map(c => c.trim());
                    for (let i = 0; i < ca.length; i++) {
                        const c = ca[i];
                        if (c.indexOf(nameEQ) === 0) {
                            return decodeURIComponent(c.substring(nameEQ.length, c.length));
                        }
                    }
                    return null;
                },
                // 新增：获取所有当前Cookie（解析为键值对对象）
                getAllCookies() {
                    const cookies = {};
                    if (!document.cookie) return cookies;
                    document.cookie.split(';').forEach(cookie => {
                        const [name, value] = cookie.trim().split('=');
                        if (name && value) {
                            cookies[name] = decodeURIComponent(value);
                        }
                    });
                    return cookies;
                },
                deleteCookie(name, path = '/', domain = '') {
                    this.setCookie(name, '', -1, path, domain);
                },

                // ======================================
                // 原有方法（已添加“返回当前Cookie”逻辑）
                // ======================================
                toggleSidebar() {
                    this.sidebarCollapsed = !this.sidebarCollapsed;
                    this.setCookie('sidebarCollapsed', this.sidebarCollapsed, 30);
                },
                toggleDir(dir) {
                    this.dirOpen[dir] = !this.dirOpen[dir];
                },
                getCurrentDate() {
                    const now = new Date();
                    const year = now.getFullYear();
                    const month = (now.getMonth() + 1).toString().padStart(2, '0');
                    const day = now.getDate().toString().padStart(2, '0');
                    return `${year}年${month}月${day}日`;
                },

                //切换父模块激活，不影响子模块
                switchParentModule(module) {
                    this.submenus[module] = true;
                },
                // 切换模块
                switchModule(module) {
                    this.currentModule = module;
                    if (module.includes('-')) {
                        const parentModule = module.split('-')[0];
                        this.submenus[parentModule] = true;
                    }
                    if (module === 'chat' && this.messages.length === 0) {
                        this.isInConversation = false;
                    }
                    if (module === 'knowledge-mine') {
                        this.getMyKnowledgeList();
                    }
                },

                // 切换子菜单展开/折叠
                toggleSubmenu(module) {
                    this.submenus[module] = !this.submenus[module];
                },
                // 切换开发文档目录展开/折叠
                toggleDir(dir) {
                    this.dirOpen[dir] = !this.dirOpen[dir];
                },

                // 调整文本框高度
                adjustTextareaHeight() {
                    const textarea = document.querySelector('.chat-input');
                    textarea.style.height = 'auto';
                    textarea.style.height = (textarea.scrollHeight > 120 ? 120 : textarea.scrollHeight) + 'px';
                },

                // 添加换行
                addNewline() {
                    this.userInput += '\n';
                    this.$nextTick(() => this.adjustTextareaHeight());
                },

                // 发送消息
                async sendMessage() {
                    if (!this.userInput.trim()) return;

                    // 添加用户消息到对话列表
                    this.messages.push({
                        sender: 'user',
                        content: this.userInput.trim(),
                        time: this.getCurrentTime()
                    });

                    this.isInConversation = true;
                    const userMessage = this.userInput.trim();
                    this.userInput = '';
                    this.adjustTextareaHeight();

                    try {
                        this.isTyping = true;
                        
                        // 创建AI消息占位符
                        const aiMessageIndex = this.messages.push({
                            sender: 'ai',
                            content: '',
                            time: this.getCurrentTime(),
                            isStreaming: true
                        }) - 1;

                        // 使用fetch API处理流式响应
                        const response = await fetch(`http://localhost:8080/ai/chat?message=${encodeURIComponent(userMessage)}`, {
                            headers: {
                                'Accept': 'text/html;charset=utf-8',
                                'X-Requested-With': 'XMLHttpRequest'
                            }
                        });

                        if (!response.ok) {
                            const errorData = await response.json().catch(() => ({}));
                            console.error('API错误详情:', errorData);
                            throw new Error(`HTTP error! status: ${response.status}`);
                        }

                        const reader = response.body.getReader();
                        const decoder = new TextDecoder();
                        let fullResponse = '';
                        let hasReceivedTokens = false;

                        const processStream = async () => {
                            try {
                                while (true) {
                                    const { done, value } = await reader.read();
                                    if (done) {
                                        this.messages[aiMessageIndex].isStreaming = false;
                                        this.isTyping = false;
                                        return;
                                    }

                                    hasReceivedTokens = true;
                                    const text = decoder.decode(value);
                                    fullResponse += text;
                                    
                                    // 更新AI消息内容
                                    this.messages[aiMessageIndex].content = this.parseMarkdown(fullResponse);
                                    this.scrollToBottom();
                                    this.highlightCodeBlocks();
                                }
                            } catch (error) {
                                console.error('流处理错误:', error);
                                this.messages[aiMessageIndex].content = hasReceivedTokens ? 
                                    fullResponse + '\n\n⚠️ 流处理中断' : 
                                    '请求失败，请稍后再试';
                                this.messages[aiMessageIndex].isStreaming = false;
                                this.isTyping = false;
                                this.scrollToBottom();
                            }
                        };

                        processStream();
                    } catch (error) {
                        console.error('请求出错:', error);
                        this.isTyping = false;
                        
                        let errorMessage = '请求处理失败，请稍后再试';
                        if (error.response) {
                            try {
                                const errorData = error.response.data;
                                errorMessage = errorData.error || errorData.message || errorMessage;
                            } catch (e) {
                                console.error('解析错误响应失败:', e);
                            }
                        } else if (error.message.includes('timeout')) {
                            errorMessage = '请求超时，请检查网络连接';
                        }

                        this.messages.push({
                            sender: 'ai',
                            content: errorMessage,
                            time: this.getCurrentTime()
                        });
                        
                        this.scrollToBottom();
                    }
                },
                getCurrentTime() {
                    const now = new Date();
                    const hours = now.getHours().toString().padStart(2, '0');
                    const minutes = now.getMinutes().toString().padStart(2, '0');
                    return `${hours}:${minutes}`;
                },

                // 解析Markdown内容
                parseMarkdown(content) {
                    return marked.parse(content, {
                        gfm: true, // 支持表格、删除线等GFM语法
                        breaks: true, // 支持换行符
                        sanitize: false // 允许HTML标签（如果后端返回的内容可信）
                    });
                },

                // 代码块高亮处理
                highlightCodeBlocks() {
                    // 查找页面中所有AI消息里的代码块
                    document.querySelectorAll('.message.ai pre code').forEach(block => {
                        hljs.highlightElement(block);
                    });
                },

                scrollToBottom() {
                    this.$nextTick(() => {
                        const chatMessages = document.querySelector('.chat-messages');

                        if (chatMessages) {
                            chatMessages.scrollTop = chatMessages.scrollHeight;
                        }

                    });
                },

                clearConversation() {
                    this.messages = [];
                    this.isInConversation = false;
                    this.userInput = '';
                },
                switchToCreateKnowledge() {
                    this.currentModule = 'create-knowledge';
                    this.knowledgeCreated = false;
                },
                switchToKnowledgeMine() {
                    this.currentModule = 'knowledge-mine';
                    this.getMyKnowledgeList();
                },
                saveKnowledge() {
                    if (!this.newKnowledge.name.trim()) {
                        alert('请填写知识库名称');
                        return;
                    }
                    if (!this.newKnowledge.creater.trim()) {
                        alert('请填写知识库创建人');
                        return;
                    }
                    if (!this.newKnowledge.description.trim()) {
                        alert('请填写知识库描述');
                        return;
                    }
                    // 发送 POST 请求到后端 API
                    axios.post('http://localhost:8080/KLB/creat', {
                        KLBName: this.newKnowledge.name,
                        KLBCreator: this.newKnowledge.creater,
                        description: this.newKnowledge.description,
                        primaryClassification: this.newKnowledge.categoryLevel1,
                        secondaryClassification: this.newKnowledge.categoryLevel2,
                        supportedDataFormats: this.newKnowledge.fileTypes,
                        KLBSearchStrategy: this.newKnowledge.keywords,
                        KLBStatus: "启用"
                    }, {
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    })
                        .then(response => {
                            console.log('知识库保存成功:', response.data);
                            // 新增：获取并输出当前所有Cookie
                            const currentCookies = this.getAllCookies();
                            console.log('当前Cookie（保存知识库后）:', currentCookies);

                            if (response.data.code === 1 && response.data.msg === 'success') {
                                this.knowledgeCreated = true;
                                this.newKnowledgeCount = 1;
                                // 切换到管理我的知识库页面
                                this.switchToKnowledgeMine();
                                // 关闭模态框
                                this.closeCreateKnowledgeModal();
                            } else {
                                console.error('知识库保存失败:', response.data.msg);
                                alert('创建知识库失败: ' + response.data.msg);
                            }
                        })
                        .catch(error => {
                            console.error('保存知识库失败:', error);
                            alert('创建知识库失败，请稍后重试');
                        });
                },
                speakMessage(content) {
                    const synth = window.speechSynthesis;
                    const utterance = new SpeechSynthesisUtterance(content);
                    utterance.lang = 'zh-CN'; // 设置语言为中文
                    synth.speak(utterance);
                },
                getMyKnowledgeList() {
                    // 发送 POST请求到后端 API 获取所有知识库信息
                    axios.post('http://localhost:8080/KLB/selectKLBByCreatorName', {//api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口
                    })
                        .then(response => {
                            console.log('获取知识库列表成功:', response.data);

                            if (response.data.code === 1 && response.data.msg === 'success') {
                                // 处理后端返回的数据，将其转换为前端期望的格式
                                this.myKnowledgeList = response.data.data.map(item => {
                                    return {
                                        id: item.id,
                                        name: item.klbname,
                                        creater: item.klbcreator,
                                        description: item.description,
                                        status: item.klbstatus,
                                        documentCount: 0,
                                        collaboratorCount: 0,
                                        updateTime: item.klbreviseTime,
                                        tags: [],
                                        accessCount: 0,
                                        categoryLevel1: item.primaryClassification,
                                        categoryLevel2: item.secondaryClassification,
                                        fileTypes: item.supportedDataFormats,
                                        keywords: item.klbsearchStrategy
                                    };
                                });
                                // 如果是新建知识库后第一次加载，更新新增数量
                                if (this.knowledgeCreated) {
                                    this.newKnowledgeCount = this.myKnowledgeList.length;
                                    this.knowledgeCreated = false;
                                }
                            } else {
                                console.error('获取知识库列表失败:', response.data.msg);
                                alert('获取知识库列表失败: ' + response.data.msg);
                            }
                        })
                        .catch(error => {
                            console.error('获取知识库列表失败:', error);
                            alert('获取知识库列表失败，请稍后重试');
                        });
                },
                // 4. 搜索知识库（向后端请求）
                searchKnowledge() {
                    const keyword = this.searchQuery.trim();
                    if (!keyword) return;

                    axios.post('localhost:8080/Knowledge_Base/KLB/KLB/SEARCH_KLBS_BY_KEY_WORDS', {
                        keyword: keyword
                    })
                        .then(response => {
                            console.log('搜索知识库列表成功:', response.data);

                            if (response.data.code === 1 && response.data.msg === 'success') {
                                this.myKnowledgeList = response.data.data.map(item => ({
                                    id: item.id,
                                    name: item.klbname,
                                    creater: item.klbcreator,
                                    description: item.description,
                                    status: 'draft',
                                    documentCount: 0,
                                    collaboratorCount: 0,
                                    updateTime: '刚刚',
                                    tags: [],
                                    accessCount: 0
                                }));
                            } else {
                                console.error('搜索知识库列表失败:', response.data.msg);
                                alert(response.data.msg);
                                this.myKnowledgeList = [];
                            }
                        })
                        .catch(error => {
                            console.error('搜索知识库列表失败:', error);
                            alert('搜索知识库列表失败，请稍后重试');
                        });
                },
                toggleRecording() {
                    this.isRecording ? this.stopRecording() : this.startRecording();
                },
                startRecording() {
                    if (!('webkitSpeechRecognition' in window)) {
                        alert('您的浏览器不支持语音输入功能，请使用支持 Web Speech API 的浏览器。');
                        return;
                    }

                    this.recognition = new webkitSpeechRecognition();
                    this.recognition.lang = 'zh-CN';
                    this.recognition.interimResults = false;

                    this.recognition.onresult = (event) => {
                        const transcript = event.results[0][0].transcript;
                        this.userInput = transcript;
                    };

                    this.recognition.onerror = (event) => {
                        console.error('语音识别出错:', event.error);
                        if (event.error === 'not-allowed') {
                            alert('您已拒绝麦克风权限，请在浏览器设置中重新授予权限。');
                        }
                    };

                    this.recognition.onend = () => {
                        this.isRecording = false;
                    };

                    navigator.mediaDevices.getUserMedia({ audio: true })
                        .then(() => {
                            this.recognition.start();
                            this.isRecording = true;
                        })
                        .catch((error) => {
                            console.error('无法获取麦克风权限:', error);
                            alert('无法获取麦克风权限，请检查设置。');
                        });
                },
                stopRecording() {
                    if (this.isRecording && this.recognition) {
                        this.recognition.stop();
                        this.isRecording = false;
                    }
                },
                copyMessage(content) {
                    const textArea = document.createElement('textarea');
                    textArea.value = content;
                    document.body.appendChild(textArea);
                    textArea.select();
                    document.execCommand('copy');
                    document.body.removeChild(textArea);
                    alert('复制成功');
                },
                setCategory(categoryId) {
                    this.activeCategory = categoryId;
                },
                openAvatarUpload() {
                    this.$refs.fileInput?.click();
                },
                handleAvatarUpload(event) {
                    const file = event.target.files[0];
                    if (!file) return;

                    if (!file.type.match('image.*')) {
                        alert('请选择有效的图片文件');
                        return;
                    }
                    if (file.size > 2 * 1024 * 1024) {
                        alert('图片大小不能超过2MB');
                        return;
                    }

                    const reader = new FileReader();
                    reader.onload = (e) => {
                        this.user.avatar = e.target.result;
                        alert('头像更新成功');
                    };
                    reader.readAsDataURL(file);
                },
                toggleEditMode() {
                    this.isEditing = !this.isEditing;
                    if (this.isEditing) {
                        this.editUser = { ...this.user };
                        this.emailChanged = false;
                    }
                },
                cancelEdit() {
                    this.isEditing = false;
                    this.emailChanged = false;
                },
                saveProfile() {
                    if (!this.editUser.phone) {
                        alert('联系电话不能为空');
                        return false;
                    }
                    if (!this.editUser.email) {
                        alert('邮箱不能为空');
                        return false;
                    }

                    if (this.editUser.email !== this.user.email) {
                        this.emailChanged = true;
                        alert('请查收验证邮件完成验证');
                    }
                    if (!this.editUser.nickName) {
                        this.editUser.nickName = this.editUser.userName;
                    }

                    this.user = { ...this.editUser };
                    this.isEditing = false;
                    alert('个人资料保存成功！');
                },
                toggleTocItem(item) {
                    this.tocExpanded[item] = !this.tocExpanded[item];
                },
                switchDoc(docId) {
                    this.currentDoc = docId;
                    const contentEl = document.querySelector('.manual-content');
                    if (contentEl) {
                        contentEl.scrollTop = 0;
                    }
                },
                toggleDevToc(item) {
                    this.devTocExpanded[item] = !this.devTocExpanded[item];
                },
                switchDevDoc(docId) {
                    this.currentDevDoc = docId;
                    const contentEl = document.querySelector('.manual-content');
                    if (contentEl) {
                        contentEl.scrollTop = 0;
                    }
                },
                // 5. 获取邮件列表（向后端请求）
                async getMails() {
                    try {
                        const response = await axios.post('');
                        this.mails = response.data;
                        // 新增：获取并输出当前所有Cookie
                        const currentCookies = this.getAllCookies();
                        console.log('当前Cookie（获取邮件列表后）:', currentCookies);
                    } catch (error) {
                        console.error('获取邮件信息出错:', error);
                    }
                },
                openMailModule() {
                    this.isMailModuleVisible = true;
                    this.getMails();
                },
                closeMailModule() {
                    this.isMailModuleVisible = false;
                },
                handleAction(type) {
                    switch (type) {
                        case 'home':
                            this.switchModule('chat');
                            break;
                        case 'profile-photo':
                            this.switchModule('profile');
                            break;
                        case 'email-message':
                            this.openMailModule();
                            break;
                    }
                },
                openCreateKnowledgeModal() {
                    this.isCreateKnowledgeModalVisible = true;
                },
                closeCreateKnowledgeModal() {
                    this.isCreateKnowledgeModalVisible = false;
                },
                openEditKnowledgeModal(knowledge) {
                    this.editKnowledge = { ...knowledge };
                    this.isEditKnowledgeModalVisible = true;
                },
                closeEditKnowledgeModal() {
                    this.isEditKnowledgeModalVisible = false;
                },
                // 6. 保存编辑后的知识库（向后端请求）
                saveEditedKnowledge() {
                    if (!this.editKnowledge.name.trim()) {
                        alert('请填写知识库名称');
                        return;
                    }
                    if (!this.editKnowledge.creater.trim()) {
                        alert('请填写知识库创建人');
                        return;
                    }
                    if (!this.editKnowledge.description.trim()) {
                        alert('请填写知识库描述');
                        return;
                    }

                    axios.post('localhost:8080/Knowledge_Base/KLB/UPDATE_KLB', {
                        KLBCreator: this.editKnowledge.creater,
                        description: this.editKnowledge.description,
                        primaryClassification: this.editKnowledge.categoryLevel1,
                        supportedDataFormats: this.editKnowledge.fileTypes,
                        KLBSearchStrategy: this.editKnowledge.keywords,
                        KLBStatus: "active"
                    }, {
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    })
                        .then(response => {
                            console.log('知识库更新成功:', response.data);

                            if (response.data.code === 1 && response.data.msg === 'success') {
                                // 关闭模态框
                                this.closeEditKnowledgeModal();
                                // 刷新知识库列表
                                this.getMyKnowledgeList();
                            } else {
                                console.error('知识库更新失败:', response.data.msg);
                                alert('更新知识库失败: ' + response.data.msg);
                            }
                        })
                        .catch(error => {
                            console.error('更新知识库失败:', error);
                            alert('更新知识库失败，请稍后重试');
                        });
                },
                openAddFilesModal(knowledge) {
                    this.currentKnowledge = knowledge;
                    this.isAddFilesModalVisible = true;
                },
                closeAddFilesModal() {
                    this.isAddFilesModalVisible = false;
                },
                removeFile(index) {
                    this.currentKnowledge.files?.splice(index, 1);
                },
                onFileChange(event) {
                    this.selectedFiles = Array.from(event.target.files);
                },
                onDragOver(event) {
                    event.preventDefault();
                    this.isDragover = true;
                },
                onDragLeave(event) {
                    this.isDragover = false;
                },
                onDrop(event) {
                    event.preventDefault();
                    this.isDragover = false;
                    this.selectedFiles = Array.from(event.dataTransfer.files);
                },
                // 7. 保存上传文件（向后端请求）
                saveFiles() {
                    if (this.selectedFiles.length === 0) {
                        alert('请选择要上传的文件');
                        return;
                    }
                    const formData = new FormData();
                    formData.append('knowledgeId', this.currentKnowledge.id);
                    formData.append('fileTypes', this.newFileTypes);
                    this.selectedFiles.forEach((file) => {
                        formData.append('files', file);
                    });
                    axios.post('', formData, {//添加api添加api添加api添加api添加api添加api添加api添加api添加api添加api添加api添加api添加api添加api添加api添加api添加api
                        headers: {
                            'Content-Type': 'multipart/form-data'
                        }
                    })
                        .then(response => {
                            console.log('文件上传成功:', response.data);
                            // 新增：获取并输出当前所有Cookie
                            const currentCookies = this.getAllCookies();
                            console.log('当前Cookie（文件上传后）:', currentCookies);
                            if (response.data.code === 1 && response.data.msg === 'success') {
                                this.closeAddFilesModal();
                                this.getMyKnowledgeList();
                                alert('文件上传成功！');
                            } else {
                                console.error('文件上传失败:', response.data.msg);
                                alert('文件上传失败: ' + response.data.msg);
                            }
                        })
                        .catch(error => {
                            console.error('文件上传失败:', error);
                            alert('文件上传失败，请稍后重试');
                        });
                },
                searchKnowledge() {
                    const keyword = this.searchQuery.trim();
                    if (keyword) {
                        axios.post('', {//api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口
                            caseName: keyword
                        })
                            .then(response => {
                                console.log('搜索知识库列表成功:', response.data);
                                if (response.data.code === 1 && response.data.msg === 'success') {
                                    this.allKnowledge = response.data.data.map(item => {
                                        return {
                                            id: item.id,
                                            name: item.caseName,
                                            creater: item.caseCreater,
                                            description: item.caseDescription,
                                            status: 'draft',
                                            documentCount: 0,
                                            collaboratorCount: 0,
                                            updateTime: '刚刚',
                                            tags: [],
                                            accessCount: 0
                                        };
                                    });
                                } else {
                                    console.error('搜索知识库列表失败:', response.data.msg);
                                    alert(response.data.msg);
                                    this.allKnowledge = [];
                                }
                            })
                            .catch(error => {
                                console.error('搜索知识库列表失败:', error);
                                alert('搜索知识库列表失败，请稍后重试');
                            });
                    }
                },
                getFrequentKnowledge() {
                    // 调用 API 获取常用知识库数据
                    axios.post('')//api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口
                        .then(response => {
                            if (response.data.code === 1 && response.data.msg === 'success') {
                                this.frequentKnowledge = response.data.data;
                                console.log('获取常用知识库数据成功:', response.data);
                                // 新增：获取并输出当前所有Cookie
                                const currentCookies = this.getAllCookies();
                                console.log('当前Cookie（获取常用知识库后）:', currentCookies);
                            } else {
                                console.error('获取常用知识库数据失败:', response.data.msg);
                                alert('获取常用知识库数据失败: ' + response.data.msg);
                            }
                        })
                        .catch(error => {
                            console.error('获取常用知识库数据失败:', error);
                            alert('获取常用知识库数据失败，请稍后重试');
                        });
                },
                getAllKnowledge() {
                    // 调用 API 获取所有知识库数据
                    axios.post('http://localhost:8080/KLB/selectAllKLB')//api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口
                        .then(response => {
                            if (response.data.code === 1 && response.data.msg === 'success') {
                                this.allKnowledge = response.data.data;
                                console.log('获取所有知识库数据成功:', response.data);
                                // 新增：获取并输出当前所有Cookie
                                const currentCookies = this.getAllCookies();
                                console.log('当前Cookie（获取所有知识库后）:', currentCookies);
                            } else {
                                console.error('获取所有知识库数据失败:', response.data.msg);
                                alert('获取所有知识库数据失败: ' + response.data.msg);
                            }
                        })
                        .catch(error => {
                            console.error('获取所有知识库数据失败:', error);
                            alert('获取所有知识库数据失败，请稍后重试');
                        });
                },

                filterByCategory(category) {
                    this.selectedCategory = category;
                    if (this.selectedCategory === 'all') {
                        // 如果选择全部，重新获取所有知识库数据
                        this.getAllKnowledge();
                    } else {
                        // 发送 API 请求获取指定分类中访问量最高的知识库信息
                        axios.post('', {//api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口api接口
                            primaryClassification: this.selectedCategory,
                            sortBy: 'accessCount',
                            order: 'desc',
                            limit: 1 // 只获取访问量最高的一个
                        })
                            .then(response => {
                                if (response.data.code === 1 && response.data.msg === 'success') {
                                    this.categoryKnowledge = response.data.data;
                                    console.log('获取指定分类知识库数据成功:', response.data);
                                    // 新增：获取并输出当前所有Cookie
                                    const currentCookies = this.getAllCookies();
                                    console.log('当前Cookie（筛选分类知识库后）:', currentCookies);
                                } else {
                                    console.error('获取指定分类知识库数据失败:', response.data.msg);
                                    alert('获取指定分类知识库数据失败: ' + response.data.msg);
                                }
                            })
                            .catch(error => {
                                console.error('获取指定分类知识库数据失败:', error);
                                alert('获取指定分类知识库数据失败，请稍后重试');
                            });
                    }
                },
                refreshPage() {
                    location.reload();
                },
                speakMessage(content) {
                    const synth = window.speechSynthesis;
                    const utterance = new SpeechSynthesisUtterance(content);
                    utterance.lang = 'zh-CN'; // 设置语言为中文
                    synth.speak(utterance);
                },
                // 11. 获取TOC文档列表（向后端请求）
                async fetchTOCData() {
                    try {
                        const response = await axios.get('http://localhost:8080/KLBS/getmd');
                        const data = response.data;

                        if (!data) {
                            this.error = '响应数据为空';
                            console.error('响应数据为空');
                            return;
                        }

                        if (data.code === 1) {
                            if (!data.data) {
                                this.error = '返回的数据中 data 字段为空';
                                console.error('返回的数据中 data 字段为空');
                                return;
                            }

                            try {
                                const parsedData = JSON.parse(data.data);
                                this.processTOCData({ data: parsedData });
                                console.log('获取TOC数据成功:', data);
                                // 新增：获取并输出当前所有Cookie
                                const currentCookies = this.getAllCookies();
                                console.log('当前Cookie（获取TOC数据后）:', currentCookies);
                            } catch (parseError) {
                                this.error = '解析返回的数据失败';
                                console.error('解析返回的数据失败:', parseError);
                            }
                        } else {
                            this.error = data.msg || '获取数据失败';
                            console.error('获取数据失败:', this.error);
                        }
                    } catch (error) {
                        this.error = '网络错误，请稍后重试';
                        console.error('获取TOC数据失败:', error);
                        if (error.response) {
                            console.error('响应错误:', error.response.status, error.response.data);
                        } else if (error.request) {
                            console.error('请求错误:', error.request);
                        } else {
                            console.error('错误信息:', error.message);
                        }
                    } finally {
                        this.loading = false;
                    }
                },
                processTOCData(data) {
                    if (!data || !data.data) {
                        console.error('传入 processTOCData 的数据不完整');
                        return;
                    }

                    const categorizedItems = data.data;
                    const tocItems = [];
                    let idCounter = 1;

                    for (const [category, files] of Object.entries(categorizedItems)) {
                        const subitems = files.map(file => {
                            const docId = `doc-${idCounter++}`;
                            const title = file.replace('.md', '');
                            return {
                                id: `sub-${docId}`,
                                title,
                                docId
                            };
                        });

                        tocItems.push({
                            id: `cat-${category.toLowerCase()}`,
                            title: category,
                            subitems
                        });
                    }

                    this.tocItems = tocItems;
                    tocItems.forEach(item => {
                        this.$set(this.tocExpanded, item.id, false);
                    });

                    if (tocItems.length > 0 && tocItems[0].subitems.length > 0) {
                        this.currentDoc = tocItems[0].subitems[0].docId;
                    }
                },
                switchDocByCategory(category) {
                    Object.keys(this.tocExpanded).forEach(key => {
                        this.tocExpanded[key] = key === category.id;
                    });

                    if (category.subitems && category.subitems.length > 0) {
                        const firstSubitem = category.subitems[0];
                        this.switchDoc(firstSubitem.docId);
                    }
                },
                /**
                * 切换文档（处理二级目录点击）
                */
                async switchDoc(docId) {
                    // 如果点击的是当前已选中的文档，不重复请求
                    if (this.currentDoc === docId) return;

                    // 记录当前文档的滚动位置
                    if (this.currentDoc && this.$refs.markdownContent) {
                        this.markdownScrollPositions[this.currentDoc] = this.$refs.markdownContent.scrollTop;
                    }

                    // 更新选中状态
                    this.currentDoc = docId;
                    this.error = null;
                    this.loading = true;

                    // 查找对应的文档路径
                    const docInfo = this.findDocInfoById(docId);
                    if (!docInfo) {
                        this.loading = false;
                        this.error = '文档不存在';
                        return;
                    }

                    // 保存文档路径和标题
                    this.currentDocPath = docInfo.path;
                    this.currentDocTitle = docInfo.title;

                    try {
                        // 请求文档内容：参数名从filename改为filePath，匹配后端接口
                        const response = await axios.get('https://frp-hub.com:39340/KLBS/getmdcontent', {
                            params: { filePath: docInfo.path }  // 关键修改：参数名调整为filePath
                        });

                        // 适配后端Result结构（无论成功失败都有code和msg）
                        if (response.data.code === 1) {  // 后端成功状态码
                            // 解析Markdown内容并渲染（后端success直接返回content，对应response.data.data）
                            this.renderedContent = this.parseMarkdown(response.data.data);
                            console.log('获取文档内容成功:', response.data);
                            // 新增：获取并输出当前所有Cookie
                            const currentCookies = this.getAllCookies();
                            console.log('当前Cookie（获取文档内容后）:', currentCookies);


                            // 如果有hash值（锚点），滚动到对应位置
                            if (window.location.hash) {
                                this.scrollToHeading(window.location.hash.substring(1));
                            } else {
                                // 恢复之前保存的滚动位置
                                this.$nextTick(() => {
                                    if (this.markdownScrollPositions[docId]) {
                                        this.$refs.markdownContent.scrollTop = this.markdownScrollPositions[docId];
                                    }
                                });
                            }
                        } else {
                            // 后端返回错误信息（如文件不存在），直接使用response.data.msg
                            this.error = response.data.msg || '获取文档内容失败';
                        }
                    } catch (error) {
                        console.error('获取文档内容失败:', error);
                        // 区分网络错误和后端返回的业务错误
                        if (error.response) {
                            // 后端返回4xx/5xx状态码时的错误信息
                            this.error = error.response.data?.msg || `请求失败（${error.response.status}）`;
                        } else {
                            // 无响应的网络错误
                            this.error = '网络错误，请检查连接或稍后重试';
                        }
                    } finally {
                        this.loading = false;
                    }
                },
                /**
                 * 根据ID查找文档信息
                 */
                findDocInfoById(docId) {
                    for (const category of this.tocItems) {
                        for (const subitem of category.subitems) {
                            if (subitem.docId === docId) {
                                // 处理特殊字符：对标题中的特殊字符进行编码，避免路径解析错误
                                const encodedCategory = encodeURIComponent(category.title.trim()); // 一级目录标题即文件名（不含后缀）
                                const fullFileName = `${encodedCategory}`; // 拼接完整文件名（含.md后缀）

                                // 构建完整路径：根目录 + 一级目录标题对应的文件名
                                const docPath = `D:/KLB# MyBatis éç½®/产品使用手册知识库/${fullFileName}`;

                                return {
                                    path: docPath,
                                    title: subitem.title
                                };
                            }
                        }
                    }
                    return null;
                },
                parseMarkdown(markdownContent) {
                    // 无需 require，直接使用全局变量 marked（CDN 引入后自动暴露）
                    // 配置 marked 选项
                    marked.setOptions({
                        gfm: true,                // 启用 GitHub 风格的 Markdown
                        breaks: true,             // 转换换行符为<br>
                        highlight: function (code, lang) {
                            // 代码高亮支持（需要先通过 CDN 引入 highlight.js）
                            if (window.hljs) { // 先判断 highlight.js 是否加载
                                if (lang && window.hljs.getLanguage(lang)) {
                                    return window.hljs.highlight(code, { language: lang }).value;
                                }
                                return window.hljs.highlightAuto(code).value;
                            }
                            // 如果没有 highlight.js，直接返回代码（不高亮）
                            return code;
                        }
                    });

                    // 解析 Markdown 并返回 HTML
                    return marked.parse(markdownContent);
                },
                /**
                 * 滚动到指定标题位置
                 */
                scrollToHeading(headingId) {
                    this.$nextTick(() => {
                        const element = document.getElementById(headingId);
                        if (element) {
                            // 添加平滑滚动效果
                            element.scrollIntoView({ behavior: 'smooth', block: 'start' });

                            // 高亮显示标题（添加动画效果）
                            element.classList.add('highlight');
                            setTimeout(() => {
                                element.classList.remove('highlight');
                            }, 2000);
                        }
                    });
                },
                /**
                * 处理内容区域点击（用于拦截内部链接）
                */
                handleContentClick(event) {
                    if (event.target.tagName === 'A') {
                        const href = event.target.getAttribute('href');
                        if (href && href.startsWith('#')) {
                            // 阻止默认行为
                            event.preventDefault();

                            // 滚动到指定锚点
                            const headingId = href.substring(1);
                            this.scrollToHeading(headingId);

                            // 更新URL但不刷新页面
                            history.pushState(null, null, href);
                        }
                    }
                },
                /**
                 * 滚动事件处理
                 */
                handleScroll() {
                    if (!this.$refs.markdownContent) return;

                    // 显示/隐藏回到顶部按钮
                    const scrollTop = this.$refs.markdownContent.scrollTop;
                    this.showBackToTop = scrollTop > 300;

                    // 自动更新导航高亮（根据滚动位置）
                    this.updateTOCActiveState(scrollTop);
                },
                /**
                 * 更新目录高亮状态
                 */
                updateTOCActiveState(scrollTop) {
                    // 获取所有标题元素
                    const headings = this.$refs.markdownContent.querySelectorAll('h1, h2, h3');
                    if (headings.length === 0) return;

                    let closestHeading = null;
                    let closestOffset = Infinity;

                    // 找到当前滚动位置最接近的标题
                    headings.forEach(heading => {
                        const headingTop = heading.offsetTop - 80; // 减去导航栏高度
                        if (headingTop <= scrollTop && scrollTop - headingTop < closestOffset) {
                            closestOffset = scrollTop - headingTop;
                            closestHeading = heading;
                        }
                    });

                    // 如果找到匹配的标题，更新URL和导航高亮
                    if (closestHeading && closestHeading.id) {
                        // 更新URL但不刷新页面
                        history.replaceState(null, null, `#${closestHeading.id}`);

                        // 找到对应的二级目录项并高亮
                        const headingText = closestHeading.textContent.trim();
                        const subitem = this.findSubitemByTitle(headingText);
                        if (subitem) {
                            this.currentDoc = subitem.docId;
                        }
                    }
                },
                /**
                 * 根据标题查找子项
                 */
                findSubitemByTitle(title) {
                    for (const category of this.tocItems) {
                        for (const subitem of category.subitems) {
                            if (subitem.title === title) {
                                return subitem;
                            }
                        }
                    }
                    return null;
                },
                /**
                * 回到顶部
                */
                scrollToTop() {
                    if (this.$refs.markdownContent) {
                        this.$refs.markdownContent.scrollTo({
                            top: 0,
                            behavior: 'smooth'
                        });
                    }
                },
                // 打开默认尺寸的新网页
                openNewPage() {
                    // 第一个参数是URL，第二个参数是窗口名称
                    window.open('file:///C:/Users/ROG/Desktop/%E9%A1%B9%E7%9B%AE14%EF%BC%88%E6%B1%87%E6%80%BB%EF%BC%89/base.html', '_blank')
                },
                removeFromCollection(id) {
                    // 实现取消收藏逻辑
                },
                searchCollections() {
                    // 实现收藏搜索逻辑
                },
                // 1. 搜索收藏的知识库
                searchCollections() {
                    // 获取搜索关键词并去空格
                    const keyword = this.collectionSearchQuery.trim().toLowerCase();
                    if (!keyword) {
                        // 若搜索为空，显示全部收藏
                        this.filteredCollections = [...this.collectedKnowledge];
                        return;
                    }
                    // 根据关键词筛选收藏（匹配名称或描述）
                    this.filteredCollections = this.collectedKnowledge.filter(kb =>
                        kb.name.toLowerCase().includes(keyword) ||
                        (kb.description && kb.description.toLowerCase().includes(keyword))
                    );
                },

                // 2. 从收藏中移除知识库
                removeFromCollection(id) {
                    // 二次确认防止误操作
                    if (!confirm('确定要取消收藏该知识库吗？')) return;

                    // 调用接口取消收藏（实际项目中替换为真实API）
                    axios.post('/api/collections/remove', { knowledgeId: id })
                        .then(response => {
                            if (response.data.code === 1) {
                                // 接口成功后更新本地收藏列表
                                this.collectedKnowledge = this.collectedKnowledge.filter(kb => kb.id !== id);
                                // 同步更新筛选后的列表
                                this.searchCollections();
                                alert('取消收藏成功');
                            } else {
                                alert('取消收藏失败：' + response.data.msg);
                            }
                        })
                        .catch(error => {
                            console.error('取消收藏失败', error);
                            alert('网络错误，取消收藏失败');
                        });
                },

                // 3. 切换收藏筛选条件（全部/分类/最近添加）
                switchCollectionFilter(type) {
                    // 更新激活的筛选按钮状态
                    this.activeCollectionFilter = type;

                    // 根据筛选类型处理
                    switch (type) {
                        case 'all':
                            // 显示全部收藏
                            this.filteredCollections = [...this.collectedKnowledge];
                            break;
                        case 'category':
                            // 按分类筛选（需提前获取分类数据）
                            this.filteredCollections = this.collectedKnowledge.filter(kb =>
                                this.activeCategory ? kb.category === this.activeCategory : true
                            );
                            break;
                        case 'recent':
                            // 按最近添加排序（假设collectTime为时间字符串）
                            this.filteredCollections = [...this.collectedKnowledge]
                                .sort((a, b) => new Date(b.collectTime) - new Date(a.collectTime));
                            break;
                    }
                },

                // 4. 切换收藏排序方式（默认/名称/收藏时间）
                sortCollections(sortType) {
                    this.activeSortType = sortType;
                    const sorted = [...this.filteredCollections];

                    switch (sortType) {
                        case 'default':
                            // 默认排序（按收藏时的原始顺序）
                            sorted.sort((a, b) => this.collectedKnowledge.indexOf(a) - this.collectedKnowledge.indexOf(b));
                            break;
                        case 'name':
                            // 按名称字母排序
                            sorted.sort((a, b) => a.name.localeCompare(b.name));
                            break;
                        case 'time':
                            // 按收藏时间排序（新到旧）
                            sorted.sort((a, b) => new Date(b.collectTime) - new Date(a.collectTime));
                            break;
                    }

                    this.filteredCollections = sorted;
                },

                // 5. 查看收藏的知识库详情
                viewKnowledge(id) {
                    // 跳转到知识库详情页（根据实际路由逻辑调整）
                    this.currentModule = 'knowledge-detail';
                    this.selectedKnowledgeId = id;
                    // 加载知识库详情数据
                    this.loadKnowledgeDetail(id);
                },

                // 6. 初始化加载收藏列表
                loadCollectedKnowledge() {
                    this.loading = true;
                    // 调用接口获取收藏列表（实际项目中替换为真实API）
                    axios.get('/api/collections')
                        .then(response => {
                            if (response.data.code === 1) {
                                this.collectedKnowledge = response.data.data;
                                this.filteredCollections = [...this.collectedKnowledge];
                            } else {
                                this.collectedKnowledge = [];
                                this.filteredCollections = [];
                                alert('获取收藏列表失败：' + response.data.msg);
                            }
                        })
                        .catch(error => {
                            console.error('加载收藏列表失败', error);
                            alert('网络错误，无法加载收藏列表');
                            this.collectedKnowledge = [];
                            this.filteredCollections = [];
                        })
                        .finally(() => {
                            this.loading = false;
                        });
                },
                openMailModule() {
                    this.isMailModuleVisible = true;
                    this.isHistoryModuleVisible = false;  // 关闭其他模块
                    // 可以在这里加载邮件数据
                },
                closeMailModule() {
                    this.isMailModuleVisible = false;
                },
                openHistoryModule() {
                    this.isHistoryModuleVisible = true;
                    this.isMailModuleVisible = false;  // 关闭其他模块
                    // 可以在这里加载历史记录数据
                    this.loadHistories();
                },
                closeHistoryModule() {
                    this.isHistoryModuleVisible = false;
                },
                // 新增头像点击跳转方法
                goToProfile() {
                    window.location.href = 'https://www.doubao.com/chat/';

                },


            },

            mounted() {
                if (this.currentModule === 'knowledge-mine') {
                    this.getMyKnowledgeList();
                }
                this.getFrequentKnowledge();
                this.getAllKnowledge();
                if (this.currentModule === 'manual') {
                    this.fetchTOCData;
                }
                // 监听滚动事件
                if (this.$refs.markdownContent) {
                    this.$refs.markdownContent.addEventListener('scroll', this.handleScroll);
                }
                if (this.currentModule === 'visit-collect') {
                    this.loadCollectedKnowledge();
                }
            },
            beforeDestroy() {
                // 移除滚动事件监听
                if (this.$refs.markdownContent) {
                    this.$refs.markdownContent.removeEventListener('scroll', this.handleScroll);
                }
            },
            watch: {
                // 监听路由变化（如果使用Vue Router）
                '$route.hash': {
                    handler(hash) {
                        if (hash && this.renderedContent) {
                            this.scrollToHeading(hash.substring(1));
                        }
                    },
                    immediate: true
                }
            }



        });
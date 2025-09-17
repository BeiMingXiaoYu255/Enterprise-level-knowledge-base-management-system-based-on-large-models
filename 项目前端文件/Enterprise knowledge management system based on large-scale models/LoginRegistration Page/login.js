// 创建科技感背景线条和点
function createTechElements() {
    const container = document.getElementById('techBackground');
    const lineCount = 15;
    const dotCount = 30;

    // 创建线条
    for (let i = 0; i < lineCount; i++) {
        const line = document.createElement('div');
        line.classList.add('tech-line');

        // 随机位置和动画延迟
        const top = Math.random() * 100;
        const delay = Math.random() * 15;
        const duration = 10 + Math.random() * 20;

        line.style.top = `${top}%`;
        line.style.width = `${20 + Math.random() * 60}%`;
        line.style.animationDelay = `-${delay}s`;
        line.style.animationDuration = `${duration}s`;
        line.style.opacity = `${0.1 + Math.random() * 0.3}`;

        container.appendChild(line);
    }

    // 创建点
    for (let i = 0; i < dotCount; i++) {
        const dot = document.createElement('div');
        dot.classList.add('tech-dot');

        // 随机位置和动画延迟
        const top = Math.random() * 100;
        const left = Math.random() * 100;
        const delay = Math.random() * 8;
        const duration = 6 + Math.random() * 6;

        dot.style.top = `${top}%`;
        dot.style.left = `${left}%`;
        dot.style.animationDelay = `-${delay}s`;
        dot.style.animationDuration = `${duration}s`;

        container.appendChild(dot);
    }
}

// 在页面加载完成后创建背景元素
document.addEventListener('DOMContentLoaded', createTechElements);

// 确保Axios已加载
if (typeof axios === 'undefined') {
    console.error('Axios未加载！请检查网络连接或引入路径。');
    alert('系统加载失败，请刷新页面重试');
} else {
    console.log('Axios已成功加载');
}

// 创建Vue实例
const login = new Vue({
    el: '.loginn',
    data: {
        currentTab: 'login',
        showSuccessModal: false,
        modalMessage: '操作成功！',
        loginLoading: false,
        registerLoading: false,
        resetLoading: false,
        agreementAccepted: false,

        loginform: {
            username: '',
            password: ''
        },
        registerform: {
            username: '',
            password: '',
            confirmPassword: '',
            email: ''
        },
        refreshform: {
            username: '',
            email: '',
            password: '',
            confirmPassword: ''
        }
    },

    methods: {
        switchTab(tab) {
            this.currentTab = tab;
        },

        denglu() {
            // 验证用户是否全部填写
            if (!this.loginform.username) {
                this.showModal('请输入用户名');
                return;
            }
            if (!this.loginform.password) {
                this.showModal('请输入密码');
                return;
            }
            if (!this.agreementAccepted) {
                this.showModal('请阅读并同意用户协议和隐私政策');
                return;
            }
            //添加登录api调用
            console.log('登录数据', this.loginform);
            const loginData = {
                username: this.loginform.username,
                password: this.loginform.password
            };

            this.loginLoading = true;
            this.loginError = '';

            axios.post('http://localhost:8080/LoginRegistration_Page/login', loginData, {
                withCredentials: true
            })
                .then(response => {
                    if (response.data.code === 1 || response.data.msg === 'success') {
                        console.log('登录成功', response.data);

                        // 从后端响应中获取token
                        const token = response.data.data;
                        if (token) {
                            // 设置会话Cookie（关闭浏览器后失效）
                            this.setSessionCookie(undefined, token); // 使用默认名称
                            console.log(`已设置Cookie: TheLifeIsGone=${token}`);
                        } else {
                            console.warn('后端未返回token数据');
                        }

                        this.showModal('登录成功');
                        // 登录成功后跳转到 main 页面
                        setTimeout(() => {
                            window.location.href = '../Main page/main.html';
                        }, 1500);
                    } else {
                        this.loginError = response.data.msg || '登录失败，请检查账号密码';
                        this.showModal(this.loginError);
                    }
                })
                .catch(error => {
                    console.error('登录错误', error);
                    if (error.response) {
                        this.loginError = `登录失败 (${error.response.status})`;
                    } else if (error.request) {
                        this.loginError = '服务器无响应，请检查API地址';
                    } else {
                        this.loginError = '登录失败，请稍后再试';
                    }
                    this.showModal(this.loginError);
                })
                .finally(() => {
                    this.loginLoading = false;
                });
        },

        reset() {
            if (this.currentTab === 'login') {
                this.loginform = {
                    username: '',
                    password: ''
                };
            }
            else if (this.currentTab === 'register') {
                this.registerform = {
                    username: '',
                    password: '',
                    confirmPassword: '',
                    email: ''
                };
            }
            else if (this.currentTab === 'refresh') {
                this.refreshform = {
                    username: '',
                    email: '',
                    password: '',
                    confirmPassword: ''
                };

            }
        },

        zhuce() {
            if (!this.registerform.username) {
                this.showModal('请输入用户名');
                return;
            }
            if (!this.registerform.password) {
                this.showModal('请输入密码');
                return;
            }
            if (!this.registerform.confirmPassword) {
                this.showModal('请确认密码');
                return;
            }
            if (this.registerform.password !== this.registerform.confirmPassword) {
                this.showModal('两次输入的密码不一致');
                return;
            }
            if (!this.registerform.email) {
                this.showModal('请输入邮箱');
                return;
            }
            console.log('注册数据', this.registerform);
            //添加注册api调用

            const registerData = {
                username: this.registerform.username,
                password: this.registerform.password,
                confirmPassword: this.registerform.confirmPassword,
                email: this.registerform.email
            };
            this.registerLoading = true;
            this.registerError = '';

            axios.post('http://localhost:8080/LoginRegistration_Page/register', registerData, {
                withCredentials: true
            })
                .then(response => {
                    if (response.data.code === 1 && response.data.msg === 'success') {
                        console.log('注册成功', response.data);
                        this.showModal('注册成功');
                        this.registerSuccess = true;
                        this.registerform = {
                            username: '',
                            password: '',
                            confirmPassword: '',
                            email: ''
                        };
                        setTimeout(() => {
                            this.switchTab('login');
                        }, 2000);
                    } else {
                        this.registerError = response.data.msg || '注册失败,未知错误';
                        this.showModal(this.registerError);

                    }
                })
                .catch(error => {
                    console.error('注册错误', error);
                    if (error.response) {
                        this.registerError = `注册失败 (${error.response.status})`;
                    } else if (error.request) {
                        this.registerError = '服务器无响应，请检查API地址';
                    } else {
                        this.registerError = '注册失败，请稍后再试';
                    }
                    this.showModal(this.registerError);

                })
                .finally(() => {
                    this.registerLoading = false;
                });
        },

        updatepassword() {
            console.log('重置密码数据', this.refreshform);
            if (!this.refreshform.username) {
                this.showModal('请输入用户名');
                return;
            }
            if (!this.refreshform.email) {
                this.showModal('请输入邮箱');
                return;
            }
            if (!this.refreshform.password) {
                this.showModal('请输入新密码');
                return;
            }
            if (!this.refreshform.confirmPassword) {
                this.showModal('请确认新密码');
                return;
            }
            if (this.refreshform.password !== this.refreshform.confirmPassword) {
                this.showModal('两次输入的密码不一致');
                return;
            }
            // 添加找回密码API调用
            console.log('重置密码数据', this.refreshform);
            const resetData = {
                username: this.refreshform.username,
                email: this.refreshform.email,
                newPassword: this.refreshform.password
            };

            this.resetLoading = true;
            this.resetError = '';

            axios.post('http://localhost:8080/user/recover-password', resetData, {
                withCredentials: true
            })
                .then(response => {
                    if (response.data.code === 1 && response.data.msg === 'success') {
                        console.log('密码重置成功', response.data);
                        this.showModal('密码重置成功，将自动返回登录界面');

                        setTimeout(() => {
                            this.refreshform = {
                                username: '',
                                email: '',
                                password: '',
                                confirmPassword: ''
                            };
                            this.switchTab('login');
                        }, 2000);
                    } else {
                        this.resetError = response.data.msg || '用户信息不存在，请检查用户名和邮箱';
                        this.showModal(this.resetError);

                    }
                })
                .catch(error => {
                    console.error('密码重置错误', error);
                    if (error.response) {
                        this.resetError = `密码重置失败 (${error.response.status})`;
                    } else if (error.request) {
                        this.resetError = '服务器无响应，请检查API地址';
                    } else {
                        this.resetError = '密码重置失败，请稍后再试';
                    }
                    this.showModal(this.resetError);

                })
                .finally(() => {
                    this.resetLoading = false;
                });
        },

        // 显示模态框
        showModal(message) {
            this.modalMessage = message;
            this.showSuccessModal = true;
        },

        closeModal() {
            this.showSuccessModal = false;
            if (this.modalMessage.includes('登录') || this.modalMessage.includes('修改')) {
                setTimeout(() => {
                    this.switchTab('login');
                    this.refreshform = {
                        username: '',
                        email: '',
                        password: '',
                        confirmPassword: ''
                    };
                }, 300);
            }
        },
         // 设置会话Cookie（7天有效期）
        setSessionCookie(name = "TheLifeIsGone", value) {
            const expires = new Date();
            expires.setTime(expires.getTime() + 7 * 24 * 60 * 60 * 1000); // 7天后过期
            let cookieString = `${name}=${value}; path=/; expires=${expires.toUTCString()}; SameSite=Lax`;
            if (window.location.protocol === 'https:') {
                cookieString += '; Secure';
            }
            document.cookie = cookieString;
            console.log('Cookie设置:', cookieString, '过期时间:', expires.toUTCString());
        },
    },

    // 添加创建钩子，用于初始化
    created() {
        console.log('Vue实例已创建');
        // 页面加载时获取验证码

    },
    
    // 添加mounted钩子，用于初始化
    mounted() {
        console.log('Vue实例已挂载');
        // 为验证码图片添加点击事件，点击可刷新验证码
        this.$nextTick(() => {
            // 等待DOM更新完成
            console.log('DOM已更新，验证码准备完成');
        });
    }
});
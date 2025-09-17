// 带线条动画和中空五边形掉落效果的正二十面体动画
function initTechLinesAnimation() {
    const canvas = document.getElementById('techLinesCanvas');
    if (!canvas) return;
    
    // 设置canvas尺寸
    const container = canvas.parentElement;
    const width = container.clientWidth;
    const height = container.clientHeight;
    canvas.width = width;
    canvas.height = height;
    
    // 初始化Three.js
    const scene = new THREE.Scene();
    
    const camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 10000);
    const renderer = new THREE.WebGLRenderer({ 
        canvas, 
        antialias: true,
        alpha: true 
    });
    renderer.setClearColor(0x000000, 0); // 完全透明背景
    
    // 定义蓝色调色板 - 新增深蓝色
    const colorPalette = {
        lightBlue: 0x87CEFA,    // 淡蓝色
        mediumBlue: 0x1E90FF,   // 中蓝色
        darkBlue: 0x0000CD,     // 深蓝色
        white: 0xffffff
    };
    
    // 创建正二十面体 - 中空线条结构
    const icosahedronGeometry = new THREE.IcosahedronGeometry(1400, 1);
    const edges = new THREE.EdgesGeometry(icosahedronGeometry);
    
    // 为每条边创建单独的线段
    const lineSegments = [];
    const lineData = [];
    const edgeCount = edges.attributes.position.count / 2;
    
    // 创建每条边的材质和线段
    for (let i = 0; i < edgeCount; i++) {
        // 获取边的两个顶点
        const startIdx = i * 6;
        const endIdx = startIdx + 3;
        
        const start = new THREE.Vector3(
            edges.attributes.position.array[startIdx],
            edges.attributes.position.array[startIdx + 1],
            edges.attributes.position.array[startIdx + 2]
        );
        
        const end = new THREE.Vector3(
            edges.attributes.position.array[endIdx],
            edges.attributes.position.array[endIdx + 1],
            edges.attributes.position.array[endIdx + 2]
        );
        
        // 创建单条线段的几何体
        const lineGeo = new THREE.BufferGeometry().setFromPoints([start, end]);
        
        // 创建材质 - 使用标准线条宽度
        const lineMat = new THREE.LineBasicMaterial({ 
            color: colorPalette.lightBlue, 
            transparent: true, 
            opacity: 1,
            linewidth: 2 // 标准线条宽度，不加粗
        });
        
        // 创建线段
        const line = new THREE.Line(lineGeo, lineMat);
        scene.add(line);
        
        // 存储线段数据用于动画
        lineSegments.push(line);
        lineData.push({
            flickerRate: 0.5 + Math.random() * 3, // 闪烁频率
            flickerOffset: Math.random() * Math.PI * 2, // 闪烁偏移
            originalPosition: new THREE.Vector3().copy(line.position) // 原始位置
        });
    }
    
    // 创建中空五边形面（使用标准线条样式）
    const faceLines = []; // 存储组成面的线条
    const faceData = [];  // 存储面的动画数据
    
    // 正二十面体的顶点索引
    const icosahedronIndices = [
        0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
        1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
        3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
        4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1
    ];
    
    // 获取几何体顶点
    const vertices = icosahedronGeometry.attributes.position.array;
    
    // 正二十面体每个面有5个顶点
    for (let i = 0; i < icosahedronIndices.length; i += 3) {
        // 检查是否已经处理过这个面
        const faceIndex = i / 3;
        if (faceIndex % 5 === 0) { 
            const faceVertices = [];
            // 收集五边形的5个顶点
            for (let j = 0; j < 5; j++) {
                const idx = icosahedronIndices[i + j * 3] * 3;
                faceVertices.push(new THREE.Vector3(
                    vertices[idx],
                    vertices[idx + 1],
                    vertices[idx + 2]
                ));
            }
            
            // 创建组成五边形的线条（中空效果，不加粗）
            const linesInFace = [];
            for (let j = 0; j < 5; j++) {
                const start = faceVertices[j];
                const end = faceVertices[(j + 1) % 5]; // 连接到下一个顶点，最后一个连接回第一个
                
                // 创建线条几何体
                const lineGeo = new THREE.BufferGeometry().setFromPoints([start, end]);
                
                // 创建线条材质（标准宽度，与主体线条颜色一致）
                const lineMat = new THREE.LineBasicMaterial({ 
                    color: colorPalette.lightBlue, 
                    transparent: true, 
                    opacity: 0.8,
                    linewidth: 2 // 标准线条宽度，不加粗
                });
                
                // 创建线条
                const line = new THREE.Line(lineGeo, lineMat);
                scene.add(line);
                linesInFace.push(line);
            }
            
            // 存储面数据用于动画
            faceLines.push(linesInFace);
            faceData.push({
                isFalling: false, // 是否正在掉落
                fallSpeed: new THREE.Vector3(0, 0, 0), // 掉落速度
                fallDistance: 0,  // 已掉落距离
                maxFallDistance: 1500 + Math.random() * 1000, // 最大掉落距离
                originalPositions: linesInFace.map(line => new THREE.Vector3().copy(line.position)) // 原始位置
            });
        }
    }
    
    // 添加白色散点
    const createParticles = () => {
        const particleCount = 200; // 散点数量
        const particlesGeometry = new THREE.BufferGeometry();
        
        // 存储粒子位置和初始大小
        const positions = new Float32Array(particleCount * 3);
        const sizes = new Float32Array(particleCount);
        
        // 随机生成粒子属性
        for (let i = 0; i < particleCount; i++) {
            const i3 = i * 3;
            
            // 在正二十面体范围内随机位置
            const radius = 1400 * (0.5 + Math.random() * 0.8);
            const theta = Math.random() * Math.PI * 2;
            const phi = Math.acos(Math.random() * 2 - 1);
            
            positions[i3] = radius * Math.sin(phi) * Math.cos(theta);
            positions[i3 + 1] = radius * Math.sin(phi) * Math.sin(theta);
            positions[i3 + 2] = radius * Math.cos(phi);
            
            // 随机大小
            sizes[i] = 1 + Math.random() * 3;
        }
        
        particlesGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
        
        // 创建粒子材质
        const particlesMaterial = new THREE.PointsMaterial({
            color: 0xffffff,
            size: 1,
            transparent: true,
            opacity: 0.8
        });
        
        // 创建粒子系统
        const particles = new THREE.Points(particlesGeometry, particlesMaterial);
        
        // 存储每个粒子的运动参数
        particles.userData = {
            speeds: Array(particleCount).fill().map(() => ({
                x: (Math.random() - 0.5) * 0.2,
                y: (Math.random() - 0.5) * 0.2,
                z: (Math.random() - 0.5) * 0.2,
                pulseSpeed: 0.5 + Math.random() * 2,
                pulseOffset: Math.random() * Math.PI * 2
            })),
            sizes: sizes
        };
        
        return particles;
    };
    
    // 创建并添加粒子到场景
    const particles = createParticles();
    scene.add(particles);
    
    // 调整相机位置
    camera.position.z = 2240;
    let cameraZOffset = 0;
    
    // 随机选择一个五边形面开始掉落
    function startRandomFaceFall() {
        // 随机概率触发
        if (Math.random() < 0.003) {
            // 找到还没掉落的面
            const availableFaces = faceData.findIndex((data) => !data.isFalling);
            if (availableFaces !== -1) {
                // 设置掉落状态
                faceData[availableFaces].isFalling = true;
                
                // 设置主要向下的掉落方向，带有一些随机偏移
                const horizontalSpeed = (Math.random() - 0.5) * 2;
                faceData[availableFaces].fallSpeed.set(
                    horizontalSpeed,  // x方向轻微随机
                    -5 - Math.random() * 3,  // y方向主要向下
                    (Math.random() - 0.5) * 2   // z方向轻微随机
                );
                faceData[availableFaces].fallDistance = 0;
            }
        }
    }
    
    // 动画循环
    function animate() {
        requestAnimationFrame(animate);
        
        // 时间变量用于同步动画
        const time = Date.now() * 0.001;
        
        // 不规则旋转 - 各轴速度不同且随时间轻微变化
        const rotationSpeedX = 0.005 + Math.sin(time * 0.3) * 0.002;
        const rotationSpeedY = 0.007 + Math.cos(time * 0.4) * 0.003;
        const rotationSpeedZ = 0.003 + Math.sin(time * 0.5) * 0.001;
        
        // 更新每条线段的动画
        lineSegments.forEach((line, idx) => {
            const data = lineData[idx];
            
            // 应用旋转
            line.rotation.x += rotationSpeedX;
            line.rotation.y += rotationSpeedY;
            line.rotation.z += rotationSpeedZ;
            
            // 单独闪烁效果
            const flicker = Math.sin(time * data.flickerRate + data.flickerOffset) * 0.5 + 0.5;
            const opacity = 0.3 + flicker * 0.7; // 闪烁范围：0.3-1.0
            line.material.opacity = opacity;
            
            // 颜色闪烁效果（淡蓝到深蓝色，而非白色）
            line.material.color.lerpColors(
                new THREE.Color(colorPalette.lightBlue),
                new THREE.Color(colorPalette.darkBlue), // 改为深蓝色
                flicker
            );
        });
        
        // 更新五边形面的动画
        faceLines.forEach((linesInFace, faceIdx) => {
            const data = faceData[faceIdx];
            
            // 如果面没有掉落，应用整体旋转和颜色渐变
            if (!data.isFalling) {
                linesInFace.forEach((line, lineIdx) => {
                    line.rotation.x += rotationSpeedX;
                    line.rotation.y += rotationSpeedY;
                    line.rotation.z += rotationSpeedZ;
                    
                    // 同步主体线条的颜色渐变效果
                    const flicker = Math.sin(time * lineData[lineIdx % lineData.length].flickerRate + lineData[lineIdx % lineData.length].flickerOffset) * 0.5 + 0.5;
                    line.material.opacity = 0.3 + flicker * 0.7;
                    line.material.color.lerpColors(
                        new THREE.Color(colorPalette.lightBlue),
                        new THREE.Color(colorPalette.darkBlue),
                        flicker
                    );
                });
            } else {
                // 应用向下掉落动画（不旋转）
                const speedFactor = 1 + (data.fallDistance / data.maxFallDistance) * 2; // 逐渐加速
                data.fallDistance += Math.abs(data.fallSpeed.y) * speedFactor;
                
                linesInFace.forEach(line => {
                    // 只移动，不旋转
                    line.position.x += data.fallSpeed.x * speedFactor;
                    line.position.y += data.fallSpeed.y * speedFactor;
                    line.position.z += data.fallSpeed.z * speedFactor;
                    
                    // 根据掉落距离计算透明度（最后阶段快速消失）
                    const remainingRatio = 1 - (data.fallDistance / data.maxFallDistance);
                    line.material.opacity = Math.max(0, remainingRatio * 2);
                });
                
                // 当掉落达到最大距离后重置
                if (data.fallDistance >= data.maxFallDistance) {
                    data.isFalling = false;
                    // 重置位置
                    linesInFace.forEach((line, idx) => {
                        line.position.copy(data.originalPositions[idx]);
                    });
                }
            }
        });
        
        // 随机触发五边形面的掉落
        startRandomFaceFall();
        
        // 更新粒子动画
        const positions = particles.geometry.attributes.position.array;
        const particleCount = positions.length / 3;
        const particleData = particles.userData;
        
        for (let i = 0; i < particleCount; i++) {
            const i3 = i * 3;
            const speed = particleData.speeds[i];
            
            // 移动粒子
            positions[i3] += speed.x;
            positions[i3 + 1] += speed.y;
            positions[i3 + 2] += speed.z;
            
            // 边界检查 - 超出范围后反弹
            const distance = Math.sqrt(
                positions[i3] **2 + 
                positions[i3 + 1]** 2 + 
                positions[i3 + 2] **2
            );
            
            if (distance > 1400 * 1.2) {
                // 反弹效果
                const nx = positions[i3] / distance;
                const ny = positions[i3 + 1] / distance;
                const nz = positions[i3 + 2] / distance;
                
                // 计算反射向量
                const dot = speed.x * nx + speed.y * ny + speed.z * nz;
                speed.x = speed.x - 2 * dot * nx;
                speed.y = speed.y - 2 * dot * ny;
                speed.z = speed.z - 2 * dot * nz;
            }
            
            // 忽明忽暗效果
            if (i === 0) {
                const opacity = 0.3 + Math.sin(time * speed.pulseSpeed + speed.pulseOffset) * 0.5;
                particles.material.opacity = opacity;
                particles.material.size = particleData.sizes[i] * (0.5 + Math.sin(time * speed.pulseSpeed + speed.pulseOffset) * 0.5);
            }
        }
        
        particles.geometry.attributes.position.needsUpdate = true;
        
        // 相机轻微浮动效果
        cameraZOffset = Math.sin(time * 0.5) * 128;
        camera.position.z = 2240 + cameraZOffset;
        
        renderer.render(scene, camera);
    }
    
    animate();
    
    // 响应窗口大小变化
    window.addEventListener('resize', () => {
        const newWidth = container.clientWidth;
        const newHeight = container.clientHeight;
        
        camera.aspect = newWidth / newHeight;
        camera.updateProjectionMatrix();
        
        renderer.setSize(newWidth, newHeight);
    });
}




// 等待DOM加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 初始化3D线条动画
    initTechLinesAnimation();
    
    // 移动端菜单切换
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const mobileMenu = document.getElementById('mobileMenu');
    
    if (mobileMenuBtn && mobileMenu) {
        mobileMenuBtn.addEventListener('click', () => {
            mobileMenu.classList.toggle('hidden');
            const icon = mobileMenuBtn.querySelector('i');
            
            if (mobileMenu.classList.contains('hidden')) {
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            } else {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-times');
            }
        });
    }
    
    // 轮播功能
    const carouselTrack = document.getElementById('carouselTrack');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const indicators = document.querySelectorAll('#carouselIndicators button');
    let currentSlide = 0;
    const totalSlides = 3;
    
    function updateCarousel() {
        if (!carouselTrack) return;
        
        let slideWidth = 100;
        if (window.innerWidth >= 768 && window.innerWidth < 1024) {
            slideWidth = 50;
        } else if (window.innerWidth >= 1024) {
            slideWidth = 33.333;
        }
        
        carouselTrack.style.transform = `translateX(-${currentSlide * slideWidth}%)`;
        
        // 更新指示器
        indicators.forEach((indicator, index) => {
            if (index === currentSlide) {
                indicator.classList.add('bg-microsoftGreen');
                indicator.classList.remove('bg-gray-300');
            } else {
                indicator.classList.remove('bg-microsoftGreen');
                indicator.classList.add('bg-gray-300');
            }
        });
    }
    
    if (prevBtn) {
        prevBtn.addEventListener('click', () => {
            currentSlide = (currentSlide - 1 + totalSlides) % totalSlides;
            updateCarousel();
        });
    }
    
    if (nextBtn) {
        nextBtn.addEventListener('click', () => {
            currentSlide = (currentSlide + 1) % totalSlides;
            updateCarousel();
        });
    }
    
    indicators.forEach((indicator) => {
        indicator.addEventListener('click', () => {
            currentSlide = parseInt(indicator.dataset.index);
            updateCarousel();
        });
    });
    
    // 窗口大小改变时更新轮播
    window.addEventListener('resize', updateCarousel);
    
    // 初始化轮播
    updateCarousel();
    
    // 滚动效果
    const backToTopBtn = document.getElementById('backToTop');
    const mainNav = document.getElementById('mainNav');
    
    function handleScrollEffects() {
        // 显示/隐藏返回顶部按钮
        if (backToTopBtn) {
            if (window.scrollY > 300) {
                backToTopBtn.classList.remove('opacity-0', 'invisible');
                backToTopBtn.classList.add('opacity-100', 'visible');
            } else {
                backToTopBtn.classList.add('opacity-0', 'invisible');
                backToTopBtn.classList.remove('opacity-100', 'visible');
            }
        }
        
        // 导航栏滚动效果
        if (mainNav) {
            if (window.scrollY > 100) {
                mainNav.classList.add('shadow-md', 'bg-white/95', 'backdrop-blur-sm');
                mainNav.classList.remove('shadow-sm', 'bg-white');
            } else {
                mainNav.classList.remove('shadow-md', 'bg-white/95', 'backdrop-blur-sm');
                mainNav.classList.add('shadow-sm', 'bg-white');
            }
        }
    }
    
    // 初始检查滚动位置
    handleScrollEffects();
    
    // 滚动时检查
    window.addEventListener('scroll', handleScrollEffects);
    
    // 返回顶部功能
    if (backToTopBtn) {
        backToTopBtn.addEventListener('click', () => {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }
    
    // 图片懒加载与动画
    const lazyImages = document.querySelectorAll('img');
    
    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries, observer) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const image = entry.target;
                    image.classList.add('fade-in');
                    imageObserver.unobserve(image);
                }
            });
        });
        
        lazyImages.forEach(image => {
            imageObserver.observe(image);
        });
    }
});
    
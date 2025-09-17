// 粒子效果配置 - 改为全局变量，允许设置面板修改
window.config = {
  particleCount: 50,
  particleColor: 'rgba(0, 123, 255, 0.5)',
  lineColor: 'rgba(0, 123, 255, 0.2)',
  lineDistance: 150,
  particleSize: 3,
  particleSpeed: 1
};

// 初始化粒子系统
function initParticles() {

  
  const container = document.createElement('div');
  container.className = 'particles-container';
  // 设置粒子容器层级（避免遮挡内容）
  container.style.position = 'fixed';
  container.style.top = '0';
  container.style.left = '0';
  container.style.width = '100%';
  container.style.height = '100%';
  container.style.pointerEvents = 'none';
  container.style.zIndex = '0';
  document.body.appendChild(container);
  container.className = 'particles-container';
  document.body.appendChild(container);

  const canvas = document.createElement('canvas');
  canvas.width = container.offsetWidth;
  canvas.height = container.offsetHeight;
  container.appendChild(canvas);

  const ctx = canvas.getContext('2d');
  const particles = [];

  // 创建粒子
  for (let i = 0; i < config.particleCount; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * config.particleSpeed,
      vy: (Math.random() - 0.5) * config.particleSpeed,
      size: Math.random() * config.particleSize + 1
    });
  }

  // 动画循环
  function animate() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // 更新和绘制粒子
    particles.forEach(p => {
      p.x += p.vx;
      p.y += p.vy;

      // 边界检查
      if (p.x < 0 || p.x > canvas.width) p.vx *= -1;
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1;

      // 绘制粒子
      ctx.fillStyle = config.particleColor;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
      ctx.fill();
    });

    // 绘制连线
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const p1 = particles[i];
        const p2 = particles[j];
        const dx = p1.x - p2.x;
        const dy = p1.y - p2.y;
        const distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < config.lineDistance) {
          ctx.strokeStyle = config.lineColor;
          ctx.lineWidth = 1 - distance / config.lineDistance;
          ctx.beginPath();
          ctx.moveTo(p1.x, p1.y);
          ctx.lineTo(p2.x, p2.y);
          ctx.stroke();
        }
      }
    }

    requestAnimationFrame(animate);
  }

  // 处理窗口大小变化
  window.addEventListener('resize', () => {
    canvas.width = container.offsetWidth;
    canvas.height = container.offsetHeight;
  });

  animate();
}

// 初始化
document.addEventListener('DOMContentLoaded', initParticles);

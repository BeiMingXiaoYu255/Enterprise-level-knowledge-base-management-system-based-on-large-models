class LargeFileUploader {
  constructor({
    fileInputId,        // 文件输入框ID
    uploadUrl,          // 分片上传接口地址
    checkUrl,           // 断点校验接口地址
    chunkSize = 5 * 1024 * 1024,  // 分片大小（默认5MB）
    onProgress          // 进度回调函数
  }) {
    this.fileInput = document.getElementById(fileInputId);
    this.uploadUrl = uploadUrl;
    this.checkUrl = checkUrl;
    this.chunkSize = chunkSize;
    this.onProgress = onProgress;
    this.abortController = null;  // 用于中断上传
    this.initEvent();
  }

  // 初始化事件监听
  initEvent() {
    this.fileInput.addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (file) {
        await this.handleUpload(file);
      }
    });
  }

  // 计算文件唯一标识（基于文件名+大小+最后修改时间）
  async getFileId(file) {
    // 简单方案：使用文件名+文件大小+最后修改时间生成唯一标识
    const fileInfo = `${file.name}-${file.size}-${file.lastModified}`;
    
    // 更可靠方案：计算文件MD5（大文件可只计算前1MB+文件信息）
    // return await this.calculateFileMD5(file);
    
    return btoa(fileInfo);  // 简单编码为Base64
  }

  // 计算文件MD5（可选，用于更可靠的唯一标识）
  async calculateFileMD5(file) {
    return new Promise((resolve) => {
      const fileReader = new FileReader();
      const spark = new SparkMD5.ArrayBuffer(); // 需要引入spark-md5库
      
      // 大文件优化：只读取前1MB和最后1MB计算MD5
      const chunkSize = 1024 * 1024;
      const chunks = [
        file.slice(0, chunkSize),
        file.slice(Math.max(0, file.size - chunkSize))
      ];
      
      let loadedChunks = 0;
      
      const loadNextChunk = () => {
        if (loadedChunks < chunks.length) {
          fileReader.readAsArrayBuffer(chunks[loadedChunks]);
          loadedChunks++;
        } else {
          spark.append(`${file.size}-${file.lastModified}`); // 补充文件元信息
          resolve(spark.end());
        }
      };
      
      fileReader.onload = (e) => {
        spark.append(e.target.result);
        loadNextChunk();
      };
      
      loadNextChunk();
    });
  }

  // 断点校验：查询已上传的分片
  async checkUploadedChunks(fileId) {
    try {
      const response = await fetch(`${this.checkUrl}?fileId=${fileId}`);
      if (!response.ok) throw new Error('校验失败');
      return await response.json();
    } catch (error) {
      console.error('校验已上传分片失败:', error);
      return []; // 校验失败默认从头上传
    }
  }

  // 上传单个分片
  async uploadChunk(file, fileId, chunkIndex, start, end) {
    const chunk = file.slice(start, end);
    
    // 创建FormData
    const formData = new FormData();
    formData.append('fileId', fileId);
    formData.append('fileName', file.name);
    formData.append('fileSize', file.size);
    formData.append('chunkIndex', chunkIndex);
    formData.append('totalChunks', Math.ceil(file.size / this.chunkSize));
    formData.append('chunkSize', this.chunkSize);
    formData.append('chunkFile', new File([chunk], `chunk-${chunkIndex}`));
    
    // 创建可中断的请求
    this.abortController = new AbortController();
    
    try {
      const response = await fetch(this.uploadUrl, {
        method: 'POST',
        body: formData,
        signal: this.abortController.signal
      });
      
      const result = await response.json();
      if (!result.success) throw new Error(result.message || '分片上传失败');
      return result;
    } catch (error) {
      if (error.name !== 'AbortError') {
        console.error(`分片${chunkIndex}上传失败:`, error);
      }
      throw error;
    }
  }

  // 处理整个文件上传流程
  async handleUpload(file) {
    console.log('开始上传文件:', file.name, '大小:', this.formatSize(file.size));
    
    // 1. 获取文件唯一标识
    const fileId = await this.getFileId(file);
    console.log('文件唯一标识:', fileId);
    
    // 2. 断点校验，获取已上传的分片
    const uploadedChunks = await this.checkUploadedChunks(fileId);
    console.log('已上传的分片:', uploadedChunks);
    
    // 3. 计算总分片数和需要上传的分片
    const totalChunks = Math.ceil(file.size / this.chunkSize);
    let uploadedSize = uploadedChunks.length * this.chunkSize;
    if (uploadedChunks.length > 0 && uploadedChunks.length === totalChunks) {
      console.log('文件已完全上传');
      this.onProgress?.(100);
      return;
    }
    
    // 4. 按顺序上传未完成的分片
    console.log(`开始上传，共${totalChunks}个分片，已完成${uploadedChunks.length}个`);
    
    for (let i = 0; i < totalChunks; i++) {
      // 跳过已上传的分片
      if (uploadedChunks.includes(i)) {
        continue;
      }
      
      // 计算分片的起始和结束位置
      const start = i * this.chunkSize;
      const end = Math.min(start + this.chunkSize, file.size);
      
      try {
        // 上传当前分片
        console.log(`上传分片${i}/${totalChunks} (${this.formatSize(start)} - ${this.formatSize(end)})`);
        const result = await this.uploadChunk(file, fileId, i, start, end);
        
        // 更新已上传大小并触发进度回调
        uploadedSize += (end - start);
        const progress = Math.floor((uploadedSize / file.size) * 100);
        this.onProgress?.(progress);
        
        // 如果所有分片都已上传完成
        if (result.isAllMerged) {
          console.log('所有分片上传完成，已合并为完整文件');
          this.onProgress?.(100);
          break;
        }
      } catch (error) {
        if (error.name === 'AbortError') {
          console.log('上传已中断');
          return;
        }
        console.error(`分片${i}上传失败，将重试`);
        i--; // 重试当前分片
        await new Promise(resolve => setTimeout(resolve, 1000)); // 等待1秒后重试
      }
    }
  }

  // 中断上传
  abortUpload() {
    if (this.abortController) {
      this.abortController.abort();
      console.log('已中断上传');
    }
  }

  // 格式化文件大小显示
  formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  }
}


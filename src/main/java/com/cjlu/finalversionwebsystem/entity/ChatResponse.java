package com.cjlu.finalversionwebsystem.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 聊天响应实体类
 * 包含AI回答内容和参考的文件信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    /**
     * AI回答的内容
     */
    private String content;
    
    /**
     * 参考的文件信息列表
     */
    private List<FileReference> references;
    
    /**
     * 是否为流式响应的最后一部分
     */
    private boolean isLast;
    
    /**
     * 文件引用信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileReference {
        /**
         * 文件名
         */
        private String fileName;
        
        /**
         * 文件路径
         */
        private String filePath;
        
        /**
         * 相关性得分
         */
        private Double relevanceScore;
        
        /**
         * 引用的内容片段
         */
        private String snippet;
    }
    
    /**
     * 构造函数 - 仅内容
     */
    public ChatResponse(String content) {
        this.content = content;
        this.isLast = false;
    }
    
    /**
     * 构造函数 - 内容和是否最后
     */
    public ChatResponse(String content, boolean isLast) {
        this.content = content;
        this.isLast = isLast;
    }
}

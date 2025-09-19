package com.cjlu.finalversionwebsystem.service.Interface;

import com.cjlu.finalversionwebsystem.entity.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 增强的聊天服务接口
 * 支持返回带有文件引用信息的聊天响应
 */
public interface EnhancedChatService {
    
    /**
     * 增强的聊天方法，返回包含文件引用的响应
     * @param message 用户消息
     * @return 包含内容和文件引用的流式响应
     */
    Flux<ChatResponse> chatWithReferences(String message);
    
    /**
     * 基于特定文件的增强聊天
     * @param message 用户消息
     * @param fileName 指定的文件名
     * @return 包含内容和文件引用的流式响应
     */
    Flux<ChatResponse> chatWithFileReferences(String message, String fileName);

    /**
     * 基于多个文件的增强聊天
     * @param message 用户消息
     * @param fileNames 指定的文件名列表
     * @return 包含内容和文件引用的流式响应
     */
    Flux<ChatResponse> chatWithMultipleFileReferences(String message, List<String> fileNames);
}

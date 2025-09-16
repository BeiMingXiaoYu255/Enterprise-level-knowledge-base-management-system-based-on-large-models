package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.entity.ChatResponse;
import com.cjlu.finalversionwebsystem.service.Interface.ChatServiceInterface;
import com.cjlu.finalversionwebsystem.service.Interface.DocumentService;
import com.cjlu.finalversionwebsystem.service.Interface.EnhancedChatService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 聊天服务实现类
 * 用于处理基于文件的聊天功能
 */
@Slf4j
@Service
public class ChatServiceImpl {

    @Autowired
    @Qualifier("openAiStreamingChatModel")
    private OpenAiStreamingChatModel streamingChatModel;

    @Autowired
    private ChatMemoryProvider chatMemoryProvider;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ChatServiceInterface chatService;

    @Autowired
    private EnhancedChatService enhancedChatService;

    /**
     * 基于特定文件的聊天（包含文件引用信息）
     */
    public Flux<String> chatWithFile(String message, String fileName) {
        try {
            log.info("开始基于文件 {} 的聊天", fileName);

            // 使用增强聊天服务获取文件引用，然后转换为纯文本格式
            return enhancedChatService.chatWithFileReferences(message, fileName)
                .map(chatResponse -> {
                    if (chatResponse.isLast() && chatResponse.getReferences() != null && !chatResponse.getReferences().isEmpty()) {
                        // 最后一个响应包含文件引用信息，转换为纯文本格式
                        StringBuilder result = new StringBuilder();
                        result.append("\n\n📚 参考文件：\n");
                        for (int i = 0; i < chatResponse.getReferences().size(); i++) {
                            ChatResponse.FileReference ref = chatResponse.getReferences().get(i);
                            result.append(String.format("%d. 文件名：%s\n", i + 1, ref.getFileName()));
                            result.append(String.format("   路径：%s\n", ref.getFilePath()));
                            if (ref.getRelevanceScore() != null) {
                                result.append(String.format("   相关性：%.2f\n", ref.getRelevanceScore()));
                            }
                            if (ref.getSnippet() != null && !ref.getSnippet().trim().isEmpty()) {
                                result.append(String.format("   内容片段：%s\n", ref.getSnippet()));
                            }
                            result.append("\n");
                        }
                        return result.toString();
                    } else {
                        // 普通的流式内容
                        return chatResponse.getContent();
                    }
                })
                .onErrorResume(error -> {
                    log.error("基于文件的聊天失败: {}", error.getMessage(), error);
                    String errorMsg = error.getMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "AI服务暂时不可用，请稍后重试";
                    }
                    return Flux.just("抱歉，处理基于文件 " + fileName + " 的AI响应时出现错误: " + errorMsg);
                });

        } catch (Exception e) {
            log.error("基于文件的聊天失败: {}", e.getMessage(), e);
            return Flux.just("抱歉，处理基于文件 " + fileName + " 的聊天时出现错误: " + e.getMessage());
        }
    }
}

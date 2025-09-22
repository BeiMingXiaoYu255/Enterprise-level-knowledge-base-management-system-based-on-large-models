package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.entity.ChatResponse;
import com.cjlu.finalversionwebsystem.service.Interface.*;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 增强的聊天服务实现类
 * 支持返回带有文件引用信息的聊天响应
 */
@Slf4j
@Service
public class EnhancedChatServiceImpl implements EnhancedChatService {

    @Autowired
    private ChatServiceInterface chatService;

    @Autowired
    @Qualifier("openAiStreamingChatModel")
    private OpenAiStreamingChatModel streamingChatModel;

    @Autowired
    private OpenAiChatModel chatModel; // 非流式模型，用于RAG功能

    @Autowired
    private ChatMemoryProvider chatMemoryProvider;

    @Autowired
    @Qualifier("contentRetriever")
    private ContentRetriever contentRetriever;



    @Autowired
    private DocumentService documentService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileReferenceDetectionService fileReferenceDetectionService;

    @Override
    public Flux<ChatResponse> chatWithReferences(String message) {
        try {
            log.info("开始增强聊天 - 消息: {}", message);

            // 使用和基础聊天相同的高效TokenStream处理方式
            return Flux.create(sink -> {
                try {
                    final StringBuilder fullResponse = new StringBuilder();

                    chatService.chat(message)
                        .onNext(token -> {
                            fullResponse.append(token);
                            // 直接发送流式内容，转换为ChatResponse格式
                            ChatResponse streamResponse = new ChatResponse(token, false);
                            sink.next(streamResponse);
                        })
                        .onComplete(response -> {
                            // AI回答完成后，检测文件引用
                            try {
                                System.out.println("开始检测文件引用 - AI回答: " + fullResponse.toString());
                                List<String> referencedFiles = fileReferenceDetectionService.detectReferencedFiles(fullResponse.toString());
                                System.out.println("检测到的引用文件数量: " + referencedFiles.size());

                                if (!referencedFiles.isEmpty()) {
                                    // 有参考文件，只显示被引用的文件
                                    StringBuilder fileInfo = new StringBuilder("\n\n📚 参考文件：\n");
                                    for (String fileName : referencedFiles) {
                                        String filePath = fileReferenceDetectionService.getFilePath(fileName);
                                        fileInfo.append("文件名：").append(fileName).append("\n");
                                        fileInfo.append("路径：").append(filePath).append("\n");
                                    }

                                    ChatResponse fileRefResponse = new ChatResponse(fileInfo.toString(), true);
                                    sink.next(fileRefResponse);
                                } else {
                                    // 没有参考任何文件
                                    ChatResponse noRefResponse = new ChatResponse("\n\n📚 没参考任何文件", true);
                                    sink.next(noRefResponse);
                                }
                            } catch (Exception e) {
                                log.warn("检测文件引用时出错: {}", e.getMessage());
                                ChatResponse noRefResponse = new ChatResponse("\n\n📚 没参考任何文件", true);
                                sink.next(noRefResponse);
                            }

                            sink.complete();
                        })
                        .onError(error -> {
                            // 检查是否已经接收到token，如果是，说明AI响应成功，只是内部处理有问题
                            if (fullResponse.length() > 0) {
                                log.warn("AI响应完成后出现内部错误（可能是token统计问题）: {}", error.getMessage());
                                // 执行文件引用检测
                                try {
                                    System.out.println("在onError中开始检测文件引用 - AI回答: " + fullResponse.toString());
                                    List<String> referencedFiles = fileReferenceDetectionService.detectReferencedFiles(fullResponse.toString());
                                    System.out.println("在onError中检测到的引用文件数量: " + referencedFiles.size());

                                    if (!referencedFiles.isEmpty()) {
                                        // 有参考文件，只显示被引用的文件
                                        StringBuilder fileInfo = new StringBuilder("\n\n📚 参考文件：\n");
                                        for (String fileName : referencedFiles) {
                                            String filePath = fileReferenceDetectionService.getFilePath(fileName);
                                            fileInfo.append("文件名：").append(fileName).append("\n");
                                            fileInfo.append("路径：").append(filePath).append("\n");
                                        }

                                        ChatResponse fileRefResponse = new ChatResponse(fileInfo.toString(), true);
                                        sink.next(fileRefResponse);
                                    } else {
                                        // 没有参考任何文件
                                        ChatResponse noRefResponse = new ChatResponse("\n\n📚 没参考任何文件", true);
                                        sink.next(noRefResponse);
                                    }
                                } catch (Exception e) {
                                    log.warn("在onError中检测文件引用时出错: {}", e.getMessage());
                                    ChatResponse noRefResponse = new ChatResponse("\n\n📚 没参考任何文件", true);
                                    sink.next(noRefResponse);
                                }
                            } else {
                                // 如果没有接收到任何token，说明是真正的AI服务错误
                                log.error("增强聊天AI响应失败: {}", error.getMessage(), error);
                                String errorMsg = error.getMessage();
                                if (errorMsg == null || errorMsg.trim().isEmpty()) {
                                    errorMsg = "AI服务暂时不可用，请稍后重试";
                                }
                                ChatResponse errorResponse = new ChatResponse("抱歉，AI响应失败: " + errorMsg, true);
                                sink.next(errorResponse);
                            }
                            sink.complete();
                        })
                        .start();
                } catch (Exception e) {
                    log.error("增强聊天启动失败: {}", e.getMessage(), e);
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "AI服务启动失败，请稍后重试";
                    }
                    ChatResponse errorResponse = new ChatResponse("抱歉，增强聊天启动失败: " + errorMsg, true);
                    sink.next(errorResponse);
                    sink.complete();
                }
            });

        } catch (Exception e) {
            log.error("增强聊天失败: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "系统暂时不可用，请稍后重试";
            }
            return Flux.just(new ChatResponse("抱歉，处理您的请求时出现了错误: " + errorMsg, true));
        }
    }

    @Override
    public Flux<ChatResponse> chatWithFileReferences(String message, String fileName) {
        try {
            log.info("开始基于文件的增强聊天 - 文件: {}, 消息: {}", fileName, message);

            // 检查文件是否支持文档解析（对于没有扩展名的文件，跳过检查）
            if (!isFileNameWithoutExtension(fileName) && !documentService.isSupportedDocument(fileName)) {
                return Flux.just(new ChatResponse("不支持的文档类型 '" + getFileExtension(fileName) + "'。支持的类型: " +
                        String.join(", ", documentService.getSupportedDocumentTypes()) +
                        "\n\n提示：对于没有扩展名的文件（如 '" + fileName + "'），系统会尝试作为文本文件处理。", true));
            }

            // 为指定文件创建ContentRetriever
            ContentRetriever fileRetriever = documentService.createRetrieverForFile(fileName);

            // 获取相关内容
            Query query = Query.from(message);
            List<Content> retrievedContents = fileRetriever.retrieve(query);

            // 提取文件引用信息
            List<ChatResponse.FileReference> references = extractFileReferences(retrievedContents, fileName);

            // 创建基于文件的RAG聊天服务
            // 使用AiServices.builder()来配置ContentRetriever，实现真正的RAG功能
            ChatServiceInterface ragChatService = null;
            try {
                ragChatService = AiServices.builder(ChatServiceInterface.class)
                        .streamingChatLanguageModel(streamingChatModel) // 使用流式模型
                        .contentRetriever(fileRetriever)
                        .chatMemoryProvider(chatMemoryProvider)
                        .build();
                log.info("成功创建带RAG功能的聊天服务");
            } catch (Exception e) {
                log.warn("无法创建带ContentRetriever的聊天服务，使用简化版本: {}", e.getMessage());
            }

            // 检查是否成功创建了RAG聊天服务
            if (ragChatService != null) {
                // 使用真正的流式RAG聊天服务
                final ChatServiceInterface finalRagChatService = ragChatService;
                final boolean[] hasReceivedTokens = {false};
                final boolean[] isCompleted = {false};

                return Flux.create(sink -> {
                    try {
                        // 直接使用原始文件内容，不依赖可能被污染的向量检索
                        log.info("开始RAG聊天，文件: {}, 问题: {}", fileName, message);

                        // 重新读取文件内容，确保使用最新且正确的内容
                        String fileContent;
                        try {
                            fileContent = fileService.readFileContent(fileName);
                            log.info("重新读取文件内容成功，文件: {}, 内容长度: {}", fileName, fileContent.length());
                        } catch (Exception e) {
                            log.error("重新读取文件内容失败: {}", e.getMessage());
                            throw new RuntimeException("无法读取文件内容: " + fileName, e);
                        }

                        final String chinesePrompt = "你是一个极其严格的文档分析助手。\n\n" +
                                             "当前文档名：" + fileName + "\n\n" +
                                             "以下是文档的完整内容：\n" +
                                             "=== 文档内容开始 ===\n" +
                                             fileContent + "\n" +
                                             "=== 文档内容结束 ===\n\n" +
                                             "用户问题：" + message + "\n\n" +
                                             "严格要求：\n" +
                                             "1. 只能基于上述「文档内容开始」到「文档内容结束」之间的内容回答\n" +
                                             "2. 如果上述内容中没有相关信息，必须回答'文档中没有相关信息'\n" +
                                             "3. 绝对禁止使用任何预训练知识或其他信息\n" +
                                             "4. 绝对禁止提及任何未在上述内容中出现的信息\n" +
                                             "5. 如果内容很少，就如实说明\n\n" +
                                             "请严格按照要求回答：";

                        // 使用普通的流式聊天模型，因为文件内容已经包含在提示词中
                        streamingChatModel.generate(chinesePrompt, new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                            @Override
                            public void onNext(String token) {
                                hasReceivedTokens[0] = true;
                                ChatResponse streamResponse = new ChatResponse(token, false);
                                sink.next(streamResponse);
                            }

                            @Override
                            public void onComplete(dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                                if (!isCompleted[0]) {
                                    isCompleted[0] = true;
                                    // 发送文件引用信息
                                    try {
                                        ChatResponse finalResponse = new ChatResponse("\n\n📚 参考文件：\n文件名：" + fileName + "\n链接：" + "http:\\\\localhost:8080\\" + "/files/" + fileName, true);
                                        sink.next(finalResponse);
                                    } catch (Exception e) {
                                        log.warn("发送文件引用信息时出错: {}", e.getMessage());
                                    }
                                    sink.complete();
                                }
                            }

                            @Override
                            public void onError(Throwable error) {
                                if (!hasReceivedTokens[0]) {
                                    // 如果没有接收到任何token，说明是真正的AI服务错误
                                    log.error("基于文件的聊天失败: {}", error.getMessage(), error);
                                    String errorMsg = error.getMessage();
                                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                                        errorMsg = "AI服务暂时不可用，请稍后重试";
                                    }
                                    ChatResponse errorResponse = new ChatResponse("抱歉，处理基于文件 " + fileName + " 的聊天时出现错误: " + errorMsg, true);
                                    sink.next(errorResponse);
                                    sink.complete();
                                } else {
                                    // 如果已经接收到token，说明AI响应成功，只是内部处理有问题
                                    log.warn("AI响应完成后出现内部错误（可能是token统计问题）: {}", error.getMessage());
                                    if (!isCompleted[0]) {
                                        isCompleted[0] = true;
                                        sink.complete();
                                    }
                                }
                            }
                        });

                    } catch (Exception e) {
                        log.error("启动基于文件的RAG聊天失败: {}", e.getMessage(), e);
                        String errorMsg = e.getMessage();
                        if (errorMsg == null || errorMsg.trim().isEmpty()) {
                            errorMsg = "系统暂时不可用，请稍后重试";
                        }
                        ChatResponse errorResponse = new ChatResponse("抱歉，基于文件的增强聊天失败: " + errorMsg, true);
                        sink.next(errorResponse);
                        sink.complete();
                    }
                });
            } else {
                // 使用流式模型的原有逻辑
                final ChatServiceInterface tempChatService = AiServices.create(ChatServiceInterface.class, streamingChatModel);

                return Flux.create(sink -> {
                    try {
                        // 构建中文提示，确保AI使用中文回复并基于文件内容
                        String chinesePrompt = "请基于提供的文件内容用中文回答问题。问题：" + message +
                                             "\n\n请仔细分析文件内容，并基于文件中的具体信息来回答问题。如果文件中没有相关信息，请明确说明。";

                        final boolean[] hasReceivedTokens = {false}; // 标记是否已接收到token
                        final boolean[] isCompleted = {false}; // 标记是否已完成

                        tempChatService.chat(chinesePrompt)
                        .onNext(token -> {
                            hasReceivedTokens[0] = true;
                            // 直接发送流式内容，转换为ChatResponse格式
                            ChatResponse streamResponse = new ChatResponse(token, false);
                            sink.next(streamResponse);
                        })
                        .onComplete(response -> {
                            if (!isCompleted[0]) {
                                isCompleted[0] = true;
                                // 发送文件引用信息（如果有的话）
                                try {
                                    if (!references.isEmpty()) {
                                        ChatResponse finalResponse = createFinalResponseWithReferences("", references);
                                        finalResponse.setLast(true);
                                        sink.next(finalResponse);
                                    }
                                } catch (Exception e) {
                                    log.warn("发送文件引用信息时出错: {}", e.getMessage());
                                    // 即使文件引用失败，也要正常完成响应
                                }
                                sink.complete();
                            }
                        })
                        .onError(error -> {
                            if (!isCompleted[0]) {
                                isCompleted[0] = true;
                                // 只有在没有接收到任何token时才显示错误信息
                                if (!hasReceivedTokens[0]) {
                                    log.error("基于文件的增强聊天AI响应失败: {}", error.getMessage(), error);
                                    String errorMsg = error.getMessage();
                                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                                        errorMsg = "AI服务暂时不可用，请稍后重试";
                                    }
                                    ChatResponse errorResponse = new ChatResponse("抱歉，基于文件的AI响应失败: " + errorMsg, true);
                                    sink.next(errorResponse);
                                } else {
                                    // 如果已经接收到token，说明AI响应成功，只是内部处理有问题
                                    // 记录错误但不向用户显示
                                    log.warn("AI响应完成后出现内部错误（可能是token统计问题）: {}", error.getMessage());
                                    // 尝试发送文件引用信息
                                    try {
                                        if (!references.isEmpty()) {
                                            ChatResponse finalResponse = createFinalResponseWithReferences("", references);
                                            finalResponse.setLast(true);
                                            sink.next(finalResponse);
                                        }
                                    } catch (Exception e) {
                                        log.warn("发送文件引用信息时出错: {}", e.getMessage());
                                    }
                                }
                                sink.complete();
                            }
                        })
                        .start();
                } catch (Exception e) {
                    log.error("基于文件的增强聊天启动失败: {}", e.getMessage(), e);
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "AI服务启动失败，请稍后重试";
                    }
                    ChatResponse errorResponse = new ChatResponse("抱歉，基于文件的增强聊天启动失败: " + errorMsg, true);
                    sink.next(errorResponse);
                    sink.complete();
                }
            });
            }

        } catch (Exception e) {
            log.error("基于文件的增强聊天失败: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "系统暂时不可用，请稍后重试";
            }
            return Flux.just(new ChatResponse("抱歉，处理基于文件 " + fileName + " 的聊天时出现错误: " + errorMsg, true));
        }
    }

    /**
     * 从检索到的内容中提取文件引用信息
     */
    private List<ChatResponse.FileReference> extractFileReferences(List<Content> contents) {
        return extractFileReferences(contents, null);
    }

    /**
     * 从检索到的内容中提取文件引用信息
     */
    private List<ChatResponse.FileReference> extractFileReferences(List<Content> contents, String specificFileName) {
        List<ChatResponse.FileReference> references = new ArrayList<>();
        
        if (contents != null) {
            for (Content content : contents) {
                try {
                    String fileName = specificFileName;
                    String filePath = "";
                    
                    // 如果没有指定文件名，尝试从内容元数据中获取
                    if (fileName == null && content.textSegment() != null && content.textSegment().metadata() != null) {
                        // 在LangChain4J 0.29.1中，metadata API可能不同，暂时使用默认值
                        fileName = "unknown_file";
                    }
                    
                    // 如果仍然没有文件名，使用默认值
                    if (fileName == null) {
                        fileName = "未知文件";
                    }
                    
                    // 构建文件路径
                    if (fileService.fileExists(fileName)) {
                        filePath = System.getProperty("user.dir") + "/files/" + fileName;
                    }
                    
                    // 获取内容片段
                    String snippet = content.textSegment() != null ? content.textSegment().text() : "";
                    if (snippet.length() > 200) {
                        snippet = snippet.substring(0, 200) + "...";
                    }
                    
                    // 计算相关性得分（这里简化处理，实际可以根据具体需求计算）
                    Double relevanceScore = 0.8; // 在LangChain4J 0.29.1中，score API可能不同，使用默认值
                    
                    ChatResponse.FileReference reference = new ChatResponse.FileReference(
                        fileName, filePath, relevanceScore, snippet
                    );
                    
                    references.add(reference);
                    
                } catch (Exception e) {
                    log.warn("提取文件引用信息时出错: {}", e.getMessage());
                }
            }
        }
        
        return references;
    }

    /**
     * 创建包含文件引用的最终响应
     */
    private ChatResponse createFinalResponseWithReferences(String fullContent, List<ChatResponse.FileReference> references) {
        // 创建只包含文件引用信息的最终响应（AI内容已经通过流式方式发送）
        StringBuilder referenceContent = new StringBuilder();

        if (!references.isEmpty()) {
            // 去重：只显示第一个文件引用（因为通常都是同一个文件）
            ChatResponse.FileReference firstRef = references.get(0);
            referenceContent.append("\n\n📚 参考文件：\n");
            referenceContent.append(String.format("文件名：%s\n", firstRef.getFileName()));
            referenceContent.append(String.format("路径：%s\n", firstRef.getFilePath()));
        } else {
            // 如果没有文件引用，发送一个空的结束标记
            referenceContent.append("");
        }

        ChatResponse finalResponse = new ChatResponse(referenceContent.toString(), true);
        // 不设置references，避免重复显示文件引用信息
        // 文件引用信息已经包含在referenceContent中了

        return finalResponse;
    }

    /**
     * 检查文件名是否没有扩展名
     */
    private boolean isFileNameWithoutExtension(String fileName) {
        return fileName != null && !fileName.contains(".");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "无扩展名";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 获取文件引用信息
     */
    private List<ChatResponse.FileReference> getFileReferences(String message) {
        try {
            Query query = Query.from(message);
            List<Content> retrievedContents = contentRetriever.retrieve(query);

            // 提取文件引用信息
            List<ChatResponse.FileReference> extractedReferences = extractFileReferences(retrievedContents);

            // 如果没有找到相关文件，添加系统中存在的文件作为参考
            if (extractedReferences.isEmpty()) {
                return createDefaultFileReferences();
            } else {
                return extractedReferences;
            }
        } catch (Exception e) {
            log.warn("检索文件内容时出错: {}", e.getMessage());
            // 如果检索失败，添加默认的文件引用
            return createDefaultFileReferences();
        }
    }

    /**
     * 创建默认的文件引用信息
     */
    private List<ChatResponse.FileReference> createDefaultFileReferences() {
        List<ChatResponse.FileReference> references = new ArrayList<>();
        try {
            // 获取系统中的所有文件
            List<String> allFiles = fileService.getAllFileNames();

            // 最多添加3个文件作为参考
            int maxFiles = Math.min(3, allFiles.size());
            for (int i = 0; i < maxFiles; i++) {
                String fileName = allFiles.get(i);
                String filePath = System.getProperty("user.dir") + "/files/" + fileName;

                ChatResponse.FileReference reference = new ChatResponse.FileReference(
                    fileName,
                    filePath,
                    0.3, // 默认相关性得分
                    "系统中的可用文件"
                );
                references.add(reference);
            }

            if (references.isEmpty()) {
                // 如果系统中没有文件，添加一个提示
                ChatResponse.FileReference reference = new ChatResponse.FileReference(
                    "无可用文件",
                    "系统中暂无文件",
                    0.0,
                    "请上传文件到系统中以获得更准确的回答"
                );
                references.add(reference);
            }

        } catch (Exception e) {
            log.warn("创建默认文件引用时出错: {}", e.getMessage());
            // 添加一个错误提示的引用
            ChatResponse.FileReference reference = new ChatResponse.FileReference(
                "文件系统错误",
                "无法访问文件系统",
                0.0,
                "文件系统暂时不可用"
            );
            references.add(reference);
        }

        return references;
    }

    @Override
    public Flux<ChatResponse> chatWithMultipleFileReferences(String message, List<String> fileNames) {
        try {
            log.info("开始基于多个文件的增强聊天 - 消息: {}, 文件数量: {}", message, fileNames.size());

            // 验证文件名列表
            if (fileNames == null || fileNames.isEmpty()) {
                return Flux.just(new ChatResponse("错误：文件名列表不能为空", true));
            }

            // 验证所有文件是否存在
            for (String fileName : fileNames) {
                if (!fileService.fileExists(fileName)) {
                    return Flux.just(new ChatResponse("错误：文件不存在 - " + fileName, true));
                }
            }

            // 为多个文件创建检索器
            ContentRetriever multiFileRetriever;
            try {
                multiFileRetriever = documentService.createRetrieverForMultipleFiles(fileNames);
                log.info("成功为多个文件创建检索器");
            } catch (Exception e) {
                log.error("为多个文件创建检索器失败: {}", e.getMessage(), e);
                return Flux.just(new ChatResponse("错误：无法处理指定的文件 - " + e.getMessage(), true));
            }

            // 创建基于多文件的RAG聊天服务
            ChatServiceInterface ragChatService = null;
            try {
                ragChatService = AiServices.builder(ChatServiceInterface.class)
                        .streamingChatLanguageModel(streamingChatModel)
                        .contentRetriever(multiFileRetriever)
                        .chatMemoryProvider(chatMemoryProvider)
                        .build();
                log.info("成功创建带多文件RAG功能的聊天服务");
            } catch (Exception e) {
                log.warn("无法创建带ContentRetriever的聊天服务，使用简化版本: {}", e.getMessage());
            }

            // 检查是否成功创建了RAG聊天服务
            if (ragChatService != null) {
                // 使用真正的流式RAG聊天服务
                final ChatServiceInterface finalRagChatService = ragChatService;
                final boolean[] hasReceivedTokens = {false};
                final boolean[] isCompleted = {false};

                return Flux.create(sink -> {
                    try {
                        final StringBuilder fullResponse = new StringBuilder();

                        // 构建严格的提示词，防止AI幻觉，并要求按文件顺序回答
                        StringBuilder fileOrderInfo = new StringBuilder();
                        for (int i = 0; i < fileNames.size(); i++) {
                            fileOrderInfo.append(String.format("文件%d：%s\n", i + 1, fileNames.get(i)));
                        }

                        // 先手动检索内容进行调试
                        Query debugQuery = Query.from(message);
                        List<Content> retrievedContents = multiFileRetriever.retrieve(debugQuery);

                        log.info("🔍 检索调试信息:");
                        log.info("📝 查询: {}", message);
                        log.info("📊 检索到 {} 个内容段", retrievedContents.size());

                        for (int i = 0; i < retrievedContents.size(); i++) {
                            Content content = retrievedContents.get(i);
                            String contentPreview = content.textSegment().text().length() > 100 ?
                                content.textSegment().text().substring(0, 100) + "..." :
                                content.textSegment().text();

                            String sourceFile = "未知";
                            if (content.textSegment().metadata() != null && content.textSegment().metadata().asMap() != null) {
                                sourceFile = content.textSegment().metadata().asMap().getOrDefault("source_file", "未知").toString();
                            }

                            log.info("🔍 检索内容 {}: 来源=[{}], 预览=[{}]", i + 1, sourceFile, contentPreview);
                        }

                        // 按照用户指定的文件顺序重新组织内容映射
                        StringBuilder fileContentMapping = new StringBuilder();

                        // 为每个用户指定的文件查找对应的检索内容
                        for (int i = 0; i < fileNames.size(); i++) {
                            String targetFileName = fileNames.get(i);
                            String fileContent = "未找到内容";

                            // 在检索结果中查找对应文件的内容
                            for (Content content : retrievedContents) {
                                String sourceFile = "未知";
                                if (content.textSegment().metadata() != null && content.textSegment().metadata().asMap() != null) {
                                    sourceFile = content.textSegment().metadata().asMap().getOrDefault("source_file", "未知").toString();
                                }

                                if (targetFileName.equals(sourceFile)) {
                                    fileContent = content.textSegment().text().length() > 200 ?
                                        content.textSegment().text().substring(0, 200) + "..." :
                                        content.textSegment().text();
                                    break;
                                }
                            }

                            fileContentMapping.append(String.format("文件%d（%s）的内容：%s\n",
                                i + 1, targetFileName, fileContent));
                        }

                        String enhancedPrompt = String.format(
                            "请严格按照以下指定的文件顺序回答问题，每个文件必须单独回答。\n\n" +
                            "用户指定的文件顺序和内容：\n%s\n" +
                            "回答要求：\n" +
                            "1. 必须严格按照上述文件顺序（文件1、文件2...）组织回答\n" +
                            "2. 每个文件单独一段，格式：**文件X（文件名）内容回答：**\n" +
                            "3. 每个文件的回答必须基于该文件的实际内容\n" +
                            "4. 如果某个文件内容很少，请如实说明\n" +
                            "5. 绝对不要混淆不同文件的内容\n" +
                            "6. 按顺序逐个回答，不要跳跃或重排\n\n" +
                            "问题：%s",
                            fileContentMapping.toString(), message
                        );

                        finalRagChatService.chat(enhancedPrompt)
                            .onNext(token -> {
                                hasReceivedTokens[0] = true;
                                fullResponse.append(token);
                                ChatResponse streamResponse = new ChatResponse(token, false);
                                sink.next(streamResponse);
                            })
                            .onComplete(response -> {
                                isCompleted[0] = true;
                                try {
                                    // 创建文件引用列表
                                    List<ChatResponse.FileReference> references = new ArrayList<>();
                                    for (String fileName : fileNames) {
                                        String filePath = System.getProperty("user.dir") + File.separator + "files" + File.separator + fileName;
                                        ChatResponse.FileReference ref = new ChatResponse.FileReference();
                                        ref.setFileName(fileName);
                                        ref.setFilePath(filePath);
                                        ref.setRelevanceScore(1.0); // 多文件聊天中所有文件都是相关的
                                        references.add(ref);
                                    }

                                    // 创建包含引用信息的最终响应（不在内容中添加文件引用文本，让Controller处理）
                                    ChatResponse finalResponse = new ChatResponse("", references, true);
                                    sink.next(finalResponse);
                                    sink.complete();

                                } catch (Exception e) {
                                    log.error("添加多文件引用信息时出错: {}", e.getMessage(), e);
                                    sink.error(e);
                                }
                            })
                            .onError(error -> {
                                log.error("多文件RAG聊天过程中出错: {}", error.getMessage(), error);
                                if (!isCompleted[0]) {
                                    isCompleted[0] = true;
                                    if (hasReceivedTokens[0]) {
                                        // 如果已经接收到token，说明AI响应成功，只是内部处理有问题
                                        // 仍然添加文件引用信息
                                        try {
                                            // 创建文件引用列表
                                            List<ChatResponse.FileReference> references = new ArrayList<>();
                                            for (String fileName : fileNames) {
                                                String filePath = System.getProperty("user.dir") + File.separator + "files" + File.separator + fileName;
                                                ChatResponse.FileReference ref = new ChatResponse.FileReference();
                                                ref.setFileName(fileName);
                                                ref.setFilePath(filePath);
                                                ref.setRelevanceScore(1.0);
                                                references.add(ref);
                                            }

                                            // 创建包含引用信息的最终响应（不在内容中添加文件引用文本，让Controller处理）
                                            ChatResponse finalResponse = new ChatResponse("", references, true);
                                            sink.next(finalResponse);
                                        } catch (Exception e) {
                                            log.error("在错误处理中添加文件引用信息失败: {}", e.getMessage(), e);
                                        }
                                    } else {
                                        ChatResponse errorResponse = new ChatResponse("抱歉，处理您的请求时出现了错误: " + error.getMessage(), true);
                                        sink.next(errorResponse);
                                    }
                                }
                                sink.complete();
                            })
                            .start();

                    } catch (Exception e) {
                        log.error("启动多文件RAG聊天时出错: {}", e.getMessage(), e);
                        ChatResponse errorResponse = new ChatResponse("抱歉，启动聊天时出现了错误: " + e.getMessage(), true);
                        sink.next(errorResponse);
                        sink.complete();
                    }
                });

            } else {
                // 如果无法创建RAG服务，返回错误信息
                return Flux.just(new ChatResponse("错误：无法创建多文件聊天服务", true));
            }

        } catch (Exception e) {
            log.error("基于多文件的聊天失败: {}", e.getMessage(), e);
            return Flux.just(new ChatResponse("抱歉，处理您的请求时出现了错误: " + e.getMessage(), true));
        }
    }
}

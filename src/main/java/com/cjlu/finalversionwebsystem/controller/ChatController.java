package com.cjlu.finalversionwebsystem.controller;


import com.cjlu.finalversionwebsystem.entity.ChatResponse;
import com.cjlu.finalversionwebsystem.entity.FileSearchResult;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.service.Interface.ChatServiceInterface;
import com.cjlu.finalversionwebsystem.service.Interface.DocumentService;
import com.cjlu.finalversionwebsystem.service.Interface.EnhancedChatService;
import com.cjlu.finalversionwebsystem.service.Interface.FileService;
import com.cjlu.finalversionwebsystem.service.Interface.FileReferenceDetectionService;
import com.cjlu.finalversionwebsystem.service.impl.ChatServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
public class ChatController {

    @Autowired
    private ChatServiceInterface chatService;

    @Autowired
    private ChatServiceImpl chatServiceImpl;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private FileService fileService;

    @Autowired
    private EnhancedChatService enhancedChatService;

    @Autowired
    private FileReferenceDetectionService fileReferenceDetectionService;

    /**
     * 普通聊天接口（使用默认的RAG配置，包含文件引用）
     */
    @RequestMapping(value = "/ai/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(@RequestParam String message) {
        try {
            log.info("普通聊天请求: {}", message);
            // 使用基础聊天服务，并在最后添加文件引用
            return Flux.create(sink -> {
                try {
                    final StringBuilder fullResponse = new StringBuilder();
                    final boolean[] hasReceivedTokens = {false}; // 标记是否已接收到token
                    final boolean[] isCompleted = {false}; // 标记是否已完成

                    chatService.chat(message)
                        .onNext(token -> {
                            hasReceivedTokens[0] = true;
                            fullResponse.append(token);
                            sink.next(token);
                        })
                        .onComplete(response -> {
                            if (!isCompleted[0]) {
                                isCompleted[0] = true;
                                log.info("基础聊天完成，准备搜索相关文件");
                                // 基于用户输入搜索包含相关内容的文件
                                try {
                                    List<String> matchingFiles = fileReferenceDetectionService.searchFilesContaining(message);

                                    if (!matchingFiles.isEmpty()) {
                                        // 找到相关文件，显示文件信息
                                        StringBuilder fileInfo = new StringBuilder("\n\n📚 参考文件：\n");
                                        for (String fileName : matchingFiles) {
                                            String filePath = fileReferenceDetectionService.getFilePath(fileName);
                                            fileInfo.append("文件名：").append(fileName).append("\n");
                                            fileInfo.append("路径：").append(filePath).append("\n");
                                        }
                                        sink.next(fileInfo.toString());
                                    } else {
                                        // 没有找到相关文件
                                        sink.next("\n\n📚 没参考任何文件");
                                    }
                                } catch (Exception e) {
                                    log.warn("搜索相关文件失败: {}", e.getMessage());
                                    sink.next("\n\n📚 没参考任何文件");
                                }
                                sink.complete();
                            }
                        })
                        .onError(error -> {
                            if (!isCompleted[0]) {
                                isCompleted[0] = true;
                                if (hasReceivedTokens[0]) {
                                    // 如果已经接收到token，说明AI响应成功，只是内部处理有问题
                                    log.warn("AI响应完成后出现内部错误（可能是token统计问题）: {}", error.getMessage());
                                    // 尝试搜索相关文件
                                    try {
                                        List<String> matchingFiles = fileReferenceDetectionService.searchFilesContaining(message);

                                        if (!matchingFiles.isEmpty()) {
                                            StringBuilder fileInfo = new StringBuilder("\n\n📚 参考文件：\n");
                                            for (String fileName : matchingFiles) {
                                                String filePath = fileReferenceDetectionService.getFilePath(fileName);
                                                fileInfo.append("文件名：").append(fileName).append("\n");
                                                fileInfo.append("路径：").append(filePath).append("\n");
                                            }
                                            sink.next(fileInfo.toString());
                                        } else {
                                            sink.next("\n\n📚 没参考任何文件");
                                        }
                                    } catch (Exception e) {
                                        log.warn("搜索相关文件失败: {}", e.getMessage());
                                        sink.next("\n\n📚 没参考任何文件");
                                    }
                                } else {
                                    log.error("基础聊天失败: {}", error.getMessage(), error);
                                }
                                sink.complete();
                            }
                        })
                        .start();
                } catch (Exception e) {
                    log.error("基础聊天启动失败: {}", e.getMessage(), e);
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "AI服务启动失败，请稍后重试";
                    }
                    sink.next("抱歉，聊天启动失败: " + errorMsg);
                    sink.complete();
                }
            });
        } catch (Exception e) {
            log.error("聊天处理失败: {}", e.getMessage(), e);
            return Flux.just("抱歉，处理您的请求时出现了错误: " + e.getMessage());
        }
    }

    /**
     * 基于特定文件的聊天接口 (POST方法)
     */
    @PostMapping("/ai/chat/file")
    public Flux<String> chatWithFile(@RequestParam String message, @RequestParam String fileName) {
        try {
            log.info("基于文件的聊天请求 (POST) - 文件: {}, 消息: {}", fileName, message);

            // 检查文件是否支持文档解析（对于没有扩展名的文件，跳过检查）
            if (!isFileNameWithoutExtension(fileName) && !documentService.isSupportedDocument(fileName)) {
                return Flux.just("不支持的文档类型 '" + getFileExtension(fileName) + "'。支持的类型: " +
                        String.join(", ", documentService.getSupportedDocumentTypes()) +
                        "\n\n提示：对于没有扩展名的文件（如 '" + fileName + "'），系统会尝试作为文本文件处理。");
            }

            // 使用文件特定的聊天服务
            return chatServiceImpl.chatWithFile(message, fileName);

        } catch (Exception e) {
            log.error("基于文件的聊天处理失败: {}", e.getMessage(), e);
            return Flux.just("抱歉，处理您的请求时出现了错误: " + e.getMessage());
        }
    }

    /**
     * 基于特定文件的聊天接口 (GET方法，方便浏览器直接访问)
     */
    @GetMapping(value = "/ai/chat/file", produces = "text/plain;charset=utf-8")
    public Flux<String> chatWithFileGet(@RequestParam String message, @RequestParam String fileName) {
        try {
            log.info("基于文件的聊天请求 (GET) - 文件: {}, 消息: {}", fileName, message);

            // 检查文件是否支持文档解析（对于没有扩展名的文件，跳过检查）
            if (!isFileNameWithoutExtension(fileName) && !documentService.isSupportedDocument(fileName)) {
                return Flux.just("不支持的文档类型 '" + getFileExtension(fileName) + "'。支持的类型: " +
                        String.join(", ", documentService.getSupportedDocumentTypes()) +
                        "\n\n提示：对于没有扩展名的文件（如 '" + fileName + "'），系统会尝试作为文本文件处理。");
            }

            // 使用文件特定的聊天服务
            return chatServiceImpl.chatWithFile(message, fileName);

        } catch (Exception e) {
            log.error("基于文件的聊天处理失败: {}", e.getMessage(), e);
            return Flux.just("抱歉，处理您的请求时出现了错误: " + e.getMessage());
        }
    }

    /**
     * 增强聊天接口（包含文件引用信息）- JSON格式
     */
    @RequestMapping(value = "/ai/chat/enhanced", produces = "application/json;charset=utf-8")
    public Flux<ChatResponse> chatEnhanced(@RequestParam String message) {
        try {
            log.info("增强聊天请求: {}", message);
            // 使用基础聊天服务，然后添加文件引用
            return Flux.create(sink -> {
                try {
                    final StringBuilder fullResponse = new StringBuilder();
                    chatService.chat(message)
                        .onNext(token -> {
                            fullResponse.append(token);
                            ChatResponse response = new ChatResponse(token, false);
                            sink.next(response);
                        })
                        .onComplete(response -> {
                            log.info("增强聊天完成，发送完成标记");
                            // 发送完成标记，并添加文件引用
                            try {
                                String fileReferences = getSimpleFileReferences();
                                if (!fileReferences.isEmpty()) {
                                    ChatResponse finalResponse = new ChatResponse(fileReferences, true);
                                    sink.next(finalResponse);
                                } else {
                                    // 如果没有文件引用，发送空的完成标记
                                    ChatResponse finalResponse = new ChatResponse("", true);
                                    sink.next(finalResponse);
                                }
                            } catch (Exception e) {
                                log.warn("获取文件引用失败: {}", e.getMessage());
                                // 即使文件引用失败，也要发送完成标记
                                ChatResponse finalResponse = new ChatResponse("", true);
                                sink.next(finalResponse);
                            }
                            sink.complete();
                        })
                        .onError(error -> {
                            log.error("增强聊天失败: {}", error.getMessage(), error);
                            // 发送完成标记，即使出错也要正确结束
                            ChatResponse finalResponse = new ChatResponse("", true);
                            sink.next(finalResponse);
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
            log.error("增强聊天处理失败: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "系统暂时不可用，请稍后重试";
            }
            return Flux.just(new ChatResponse("抱歉，处理您的请求时出现了错误: " + errorMsg, true));
        }
    }

    /**
     * 增强聊天接口（流式文本格式，适合浏览器实时显示）
     */
    @RequestMapping(value = "/ai/chat/enhanced/stream", produces = "text/plain;charset=utf-8")
    public Flux<String> chatEnhancedStream(@RequestParam String message) {
        try {
            log.info("增强聊天流式请求: {}", message);
            // 使用基础聊天服务，然后添加文件引用
            return Flux.create(sink -> {
                try {
                    final StringBuilder fullResponse = new StringBuilder();
                    final boolean[] hasReceivedTokens = {false}; // 标记是否已接收到token
                    final boolean[] isCompleted = {false}; // 标记是否已完成

                    chatService.chat(message)
                        .onNext(token -> {
                            hasReceivedTokens[0] = true;
                            fullResponse.append(token);
                            sink.next(token);
                        })
                        .onComplete(response -> {
                            if (!isCompleted[0]) {
                                isCompleted[0] = true;
                                // 搜索相关文件信息
                                try {
                                    System.out.println("增强聊天完成，开始搜索相关文件 - 用户输入: " + message);
                                    List<String> matchingFiles = fileReferenceDetectionService.searchFilesContaining(message);

                                    if (!matchingFiles.isEmpty()) {
                                        StringBuilder references = new StringBuilder();
                                        references.append("\n\n📚 参考文件：\n");
                                        for (int i = 0; i < matchingFiles.size(); i++) {
                                            String fileName = matchingFiles.get(i);
                                            String filePath = fileReferenceDetectionService.getFilePath(fileName);
                                            references.append(String.format("%d. 文件名：%s\n", i + 1, fileName));
                                            references.append(String.format("   路径：%s\n", filePath));
                                            if (i < matchingFiles.size() - 1) {
                                                references.append("\n");
                                            }
                                        }
                                        sink.next(references.toString());
                                    } else {
                                        sink.next("\n\n📚 参考文件：\n没参考任何文件");
                                    }
                                } catch (Exception e) {
                                    log.warn("搜索相关文件失败: {}", e.getMessage());
                                }
                                sink.complete();
                            }
                        })
                        .onError(error -> {
                            if (!isCompleted[0]) {
                                isCompleted[0] = true;
                                // 只有在没有接收到任何token时才显示错误信息
                                if (!hasReceivedTokens[0]) {
                                    log.error("增强聊天流式失败: {}", error.getMessage(), error);
                                    String errorMsg = error.getMessage();
                                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                                        errorMsg = "AI服务暂时不可用，请稍后重试";
                                    }
                                    sink.next("抱歉，AI响应失败: " + errorMsg);
                                } else {
                                    // 如果已经接收到token，说明AI响应成功，只是内部处理有问题
                                    // 记录错误但不向用户显示
                                    log.warn("AI响应完成后出现内部错误（可能是token统计问题）: {}", error.getMessage());
                                    // 尝试搜索相关文件信息
                                    try {
                                        System.out.println("增强聊天在onError中开始搜索相关文件 - 用户输入: " + message);
                                        List<String> matchingFiles = fileReferenceDetectionService.searchFilesContaining(message);

                                        if (!matchingFiles.isEmpty()) {
                                            StringBuilder references = new StringBuilder();
                                            references.append("\n\n📚 参考文件：\n");
                                            for (int i = 0; i < matchingFiles.size(); i++) {
                                                String fileName = matchingFiles.get(i);
                                                String filePath = fileReferenceDetectionService.getFilePath(fileName);
                                                references.append(String.format("%d. 文件名：%s\n", i + 1, fileName));
                                                references.append(String.format("   路径：%s\n", filePath));
                                                if (i < matchingFiles.size() - 1) {
                                                    references.append("\n");
                                                }
                                            }
                                            sink.next(references.toString());
                                        } else {
                                            sink.next("\n\n📚 参考文件：\n没参考任何文件");
                                        }
                                    } catch (Exception e) {
                                        log.warn("搜索相关文件失败: {}", e.getMessage());
                                    }
                                }
                                sink.complete();
                            }
                        })
                        .start();
                } catch (Exception e) {
                    log.error("增强聊天流式启动失败: {}", e.getMessage(), e);
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "AI服务启动失败，请稍后重试";
                    }
                    sink.next("抱歉，增强聊天启动失败: " + errorMsg);
                    sink.complete();
                }
            });
        } catch (Exception e) {
            log.error("增强聊天流式处理失败: {}", e.getMessage(), e);
            return Flux.just("抱歉，处理您的请求时出现了错误: " + e.getMessage());
        }
    }

    /**
     * 基于特定文件的增强聊天接口（包含文件引用信息）- POST方法
     */
    @PostMapping(value = "/ai/chat/file/enhanced", produces = "application/json;charset=utf-8")
    public Flux<ChatResponse> chatWithFileEnhanced(@RequestParam String message, @RequestParam String fileName) {
        try {
            log.info("基于文件的增强聊天请求 (POST) - 文件: {}, 消息: {}", fileName, message);
            return enhancedChatService.chatWithFileReferences(message, fileName);
        } catch (Exception e) {
            log.error("基于文件的增强聊天处理失败: {}", e.getMessage(), e);
            return Flux.just(new ChatResponse("抱歉，处理基于文件 " + fileName + " 的聊天时出现错误: " + e.getMessage(), true));
        }
    }

    /**
     * 基于特定文件的增强聊天接口（包含文件引用信息）- GET方法，方便浏览器直接访问
     */
    @GetMapping(value = "/ai/chat/file/enhanced", produces = "application/json;charset=utf-8")
    public Flux<ChatResponse> chatWithFileEnhancedGet(@RequestParam String message, @RequestParam String fileName) {
        try {
            log.info("基于文件的增强聊天请求 (GET) - 文件: {}, 消息: {}", fileName, message);
            return enhancedChatService.chatWithFileReferences(message, fileName);
        } catch (Exception e) {
            log.error("基于文件的增强聊天处理失败: {}", e.getMessage(), e);
            return Flux.just(new ChatResponse("抱歉，处理基于文件 " + fileName + " 的聊天时出现错误: " + e.getMessage(), true));
        }
    }

    /**
     * 获取支持的文档类型
     */
    @GetMapping("/ai/supported-types")
    public Result getSupportedDocumentTypes() {
        try {
            return Result.success(documentService.getSupportedDocumentTypes());
        } catch (Exception e) {
            log.error("获取支持的文档类型失败: {}", e.getMessage(), e);
            return Result.error("获取支持的文档类型失败: " + e.getMessage());
        }
    }

    /**
     * 文件检索接口
     */
    @GetMapping("/ai/search-files")
    public Result searchFiles(@RequestParam String keyword) {
        try {
            log.info("文件检索请求 - 关键字: {}", keyword);

            if (keyword == null || keyword.trim().isEmpty()) {
                return Result.error("搜索关键字不能为空");
            }

            List<FileSearchResult> results = fileService.searchFiles(keyword.trim());

            log.info("文件检索完成 - 关键字: {}, 找到 {} 个匹配文件", keyword, results.size());
            return Result.success(results);

        } catch (Exception e) {
            log.error("文件检索失败: {}", e.getMessage(), e);
            return Result.error("文件检索失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有文件列表
     */
    @GetMapping("/ai/files")
    public Result getAllFiles() {
        try {
            log.info("获取所有文件列表请求");

            List<String> fileNames = fileService.getAllFileNames();

            log.info("获取文件列表完成 - 共 {} 个文件", fileNames.size());
            return Result.success(fileNames);

        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return Result.error("获取文件列表失败: " + e.getMessage());
        }
    }

//    @RequestMapping("/chat")
//    public String chat(String message)
//    {
//        String result=chatService.chat(message);
//        return result;
//    }

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
     * 带文件引用的聊天接口 - 专门用于显示文件引用
     */
    @RequestMapping(value = "/ai/chat/with-files", produces = "text/html;charset=utf-8")
    public Flux<String> chatWithFileReferences(@RequestParam String message) {
        try {
            log.info("带文件引用的聊天请求: {}", message);
            return Flux.create(sink -> {
                try {
                    final StringBuilder fullResponse = new StringBuilder();
                    chatService.chat(message)
                        .onNext(token -> {
                            fullResponse.append(token);
                            sink.next(token);
                        })
                        .onComplete(response -> {
                            // 直接添加文件引用信息
                            String fileReferences = getSimpleFileReferences();
                            sink.next(fileReferences);
                            sink.complete();
                        })
                        .onError(error -> {
                            log.error("带文件引用的聊天失败: {}", error.getMessage(), error);
                            sink.next("\n\n抱歉，AI响应过程中出现了问题，但这里是系统中的文件信息：");
                            String fileReferences = getSimpleFileReferences();
                            sink.next(fileReferences);
                            sink.complete();
                        })
                        .start();
                } catch (Exception e) {
                    log.error("带文件引用的聊天启动失败: {}", e.getMessage(), e);
                    sink.next("抱歉，聊天启动失败，但这里是系统中的文件信息：");
                    String fileReferences = getSimpleFileReferences();
                    sink.next(fileReferences);
                    sink.complete();
                }
            });
        } catch (Exception e) {
            log.error("带文件引用的聊天处理失败: {}", e.getMessage(), e);
            return Flux.just("抱歉，处理您的请求时出现了错误: " + e.getMessage());
        }
    }

    /**
     * 获取简单的文件引用信息
     */
    private String getSimpleFileReferences() {
        try {
            List<String> allFiles = fileService.getAllFileNames();
            if (allFiles.isEmpty()) {
                return "\n\n📚 参考文件：\n暂无可用文件，请上传文件到系统中以获得更准确的回答。\n";
            }

            StringBuilder references = new StringBuilder();
            references.append("\n\n📚 参考文件：\n");

            // 最多显示3个文件
            int maxFiles = Math.min(3, allFiles.size());
            for (int i = 0; i < maxFiles; i++) {
                String fileName = allFiles.get(i);
                String filePath = System.getProperty("user.dir") + "/files/" + fileName;
                references.append(String.format("%d. 文件名：%s\n", i + 1, fileName));
                references.append(String.format("   路径：%s\n", filePath));
                references.append("\n");
            }

            if (allFiles.size() > 3) {
                references.append(String.format("... 还有 %d 个文件\n", allFiles.size() - 3));
            }

            return references.toString();
        } catch (Exception e) {
            log.warn("获取文件引用失败: {}", e.getMessage());
            return "\n\n📚 参考文件：\n文件系统暂时不可用。\n";
        }
    }
}

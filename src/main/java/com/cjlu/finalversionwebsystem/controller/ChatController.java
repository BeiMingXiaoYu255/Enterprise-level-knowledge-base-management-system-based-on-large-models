package com.cjlu.finalversionwebsystem.controller;


import com.cjlu.finalversionwebsystem.entity.ChatResponse;
import com.cjlu.finalversionwebsystem.entity.FileSearchResult;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.mapper.NewKLBMapper;
import com.cjlu.finalversionwebsystem.service.Interface.ChatServiceInterface;
import com.cjlu.finalversionwebsystem.service.Interface.DocumentService;
import com.cjlu.finalversionwebsystem.service.Interface.EnhancedChatService;
import com.cjlu.finalversionwebsystem.service.Interface.FileService;
import com.cjlu.finalversionwebsystem.service.Interface.FileReferenceDetectionService;
import com.cjlu.finalversionwebsystem.service.Interface.ModelManagementService;
import com.cjlu.finalversionwebsystem.service.impl.ChatServiceImpl;
import com.cjlu.finalversionwebsystem.service.impl.NewKLBServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private ModelManagementService modelManagementService;

    @Autowired
    private NewKLBServiceImpl newKLBService;

    /**
     * 普通聊天接口（使用动态切换的模型配置，包含文件引用）
     */
    @RequestMapping(value = "/ai/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(@RequestParam String message) {
        try {
            log.info("普通聊天请求: {}", message);
            // 使用动态切换的聊天服务，并在最后添加文件引用
            return Flux.create(sink -> {
                try {
                    final StringBuilder fullResponse = new StringBuilder();
                    final boolean[] hasReceivedTokens = {false}; // 标记是否已接收到token
                    final boolean[] isCompleted = {false}; // 标记是否已完成

                    // 获取当前动态切换的聊天服务
                    ChatServiceInterface dynamicChatService = modelManagementService.getCurrentChatService();
                    if (dynamicChatService == null) {
                        log.warn("动态聊天服务为null，使用默认聊天服务");
                        dynamicChatService = chatService;
                    }

                    dynamicChatService.chat(message)
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
            // 使用动态切换的聊天服务，然后添加文件引用
            return Flux.create(sink -> {
                try {
                    final StringBuilder fullResponse = new StringBuilder();
                    final boolean[] hasReceivedTokens = {false}; // 标记是否已接收到token
                    final boolean[] isCompleted = {false}; // 标记是否已完成

                    // 获取当前动态切换的聊天服务
                    ChatServiceInterface dynamicChatService = modelManagementService.getCurrentChatService();
                    if (dynamicChatService == null) {
                        log.warn("动态聊天服务为null，使用默认聊天服务");
                        dynamicChatService = chatService;
                    }

                    dynamicChatService.chat(message)
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

    /**
     * 多文件聊天接口（流式响应）
     * @param message 用户消息
     * @param fileNames 文件名列表，用逗号分隔
     * @return 流式聊天响应
     */
    @RequestMapping(value = "/ai/chat/multifile", produces = "text/html;charset=utf-8")
    public Flux<String> chatWithMultipleFiles(@RequestParam String message, @RequestParam String fileNames) {
        try {
            log.info("多文件聊天请求 - 消息: {}, 文件: {}", message, fileNames);

            // 解析文件名列表
            List<String> fileNameList = Arrays.asList(fileNames.split(","));
            // 去除空格
            fileNameList = fileNameList.stream()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            if (fileNameList.isEmpty()) {
                return Flux.just("错误：请提供至少一个文件名");
            }

            log.info("解析后的文件列表: {}", fileNameList);

            // 使用增强聊天服务的多文件功能，确保包含文件引用信息
            return enhancedChatService.chatWithMultipleFileReferences(message, fileNameList)
                    .map(chatResponse -> {
                        if (chatResponse.isLast() && chatResponse.getReferences() != null && !chatResponse.getReferences().isEmpty()) {
                            // 最后一个响应包含文件引用信息，转换为纯文本格式
                            StringBuilder result = new StringBuilder();
                            result.append(chatResponse.getContent()); // 先添加内容
                            result.append("\n\n📚 参考文件：\n");
                            for (int i = 0; i < chatResponse.getReferences().size(); i++) {
                                ChatResponse.FileReference ref = chatResponse.getReferences().get(i);
                                result.append(String.format("文件%d：%s\n", i + 1, ref.getFileName()));
                                result.append(String.format("路径：%s\n", ref.getFilePath()));
                                if (i < chatResponse.getReferences().size() - 1) {
                                    result.append("\n");
                                }
                            }
                            return result.toString();
                        } else {
                            // 普通的流式内容
                            return chatResponse.getContent();
                        }
                    });

        } catch (Exception e) {
            log.error("多文件聊天处理失败: {}", e.getMessage(), e);
            return Flux.just("抱歉，处理您的请求时出现了错误: " + e.getMessage());
        }
    }

    /**
     * 多文件聊天接口（JSON格式流式响应）
     * @param message 用户消息
     * @param fileNames 文件名列表，用逗号分隔
     * @return 包含内容和文件引用的流式响应
     */
    @RequestMapping(value = "/ai/chat/multifile/stream", produces = "application/json;charset=utf-8")
    public Flux<ChatResponse> chatWithMultipleFilesStream(@RequestParam String message, @RequestParam String fileNames) {
        try {
            log.info("多文件流式聊天请求 - 消息: {}, 文件: {}", message, fileNames);

            // 解析文件名列表
            List<String> fileNameList = Arrays.asList(fileNames.split(","));
            // 去除空格
            fileNameList = fileNameList.stream()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            if (fileNameList.isEmpty()) {
                return Flux.just(new ChatResponse("错误：请提供至少一个文件名", true));
            }

            log.info("解析后的文件列表: {}", fileNameList);

            // 使用增强聊天服务的多文件功能
            return enhancedChatService.chatWithMultipleFileReferences(message, fileNameList);

        } catch (Exception e) {
            log.error("多文件流式聊天处理失败: {}", e.getMessage(), e);
            return Flux.just(new ChatResponse("抱歉，处理您的请求时出现了错误: " + e.getMessage(), true));
        }
    }

    /**
     * 多文件增强聊天接口（流式文本格式，类似增强聊天的体验）
     * @param message 用户消息
     * @param fileNames 文件名列表，用逗号分隔
     * @return 流式文本响应，实时显示AI回答和文件引用
     */
    @RequestMapping(value = "/ai/chat/multifile/enhanced", produces = "text/plain;charset=utf-8")
    public Flux<String> chatWithMultipleFilesEnhanced(@RequestParam String message, @RequestParam String fileNames) {
        try {
            log.info("多文件增强聊天请求 - 消息: {}, 文件: {}", message, fileNames);

            // 解析文件名列表
            final List<String> fileNameList = Arrays.stream(fileNames.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            log.info("解析后的文件列表: {}", fileNameList);

            if (fileNameList.isEmpty()) {
                return Flux.just("请提供至少一个文件名");
            }

            // 使用增强聊天的方式处理多文件聊天
            return Flux.create(sink -> {
                try {
                    final StringBuilder fullResponse = new StringBuilder();
                    final boolean[] hasReceivedTokens = {false}; // 标记是否已接收到token
                    final boolean[] isCompleted = {false}; // 标记是否已完成

                    enhancedChatService.chatWithMultipleFileReferences(message, fileNameList)
                            .subscribe(
                                    chatResponse -> {
                                        if (!chatResponse.getContent().isEmpty()) {
                                            hasReceivedTokens[0] = true;
                                            fullResponse.append(chatResponse.getContent());
                                            sink.next(chatResponse.getContent());
                                        }

                                        // 如果是最后一个响应且包含文件引用信息
                                        if (chatResponse.isLast()) {
                                            if (!isCompleted[0]) {
                                                isCompleted[0] = true;

                                                // 检查是否有文件引用信息，如果有则显示
                                                if (chatResponse.getReferences() != null && !chatResponse.getReferences().isEmpty()) {
                                                    // 添加格式化的文件引用信息
                                                    try {
                                                        StringBuilder references = new StringBuilder();
                                                        references.append("\n\n📚 参考文件：\n");
                                                        for (int i = 0; i < chatResponse.getReferences().size(); i++) {
                                                            ChatResponse.FileReference ref = chatResponse.getReferences().get(i);
                                                            references.append(String.format("%d. 文件名：%s\n", i + 1, ref.getFileName()));
                                                            references.append(String.format("   路径：%s\n", ref.getFilePath()));
                                                            if (i < chatResponse.getReferences().size() - 1) {
                                                                references.append("\n");
                                                            }
                                                        }
                                                        sink.next(references.toString());
                                                    } catch (Exception e) {
                                                        log.warn("添加文件引用信息失败: {}", e.getMessage());
                                                    }
                                                }
                                                sink.complete();
                                            }
                                        }
                                    },
                                    error -> {
                                        if (!isCompleted[0]) {
                                            isCompleted[0] = true;
                                            // 只有在没有接收到任何token时才显示错误信息
                                            if (!hasReceivedTokens[0]) {
                                                log.error("多文件增强聊天失败: {}", error.getMessage(), error);
                                                String errorMsg = error.getMessage();
                                                if (errorMsg == null || errorMsg.trim().isEmpty()) {
                                                    errorMsg = "AI服务暂时不可用，请稍后重试";
                                                }
                                                sink.next("抱歉，AI响应失败: " + errorMsg);
                                            } else {
                                                // 如果已经接收到token，说明AI响应成功，只是内部处理有问题
                                                log.warn("AI响应完成后出现内部错误: {}", error.getMessage());
                                                // 仍然添加文件引用信息
                                                try {
                                                    StringBuilder references = new StringBuilder();
                                                    references.append("\n\n📚 参考文件：\n");
                                                    for (int i = 0; i < fileNameList.size(); i++) {
                                                        String fileName = fileNameList.get(i);
                                                        String filePath = System.getProperty("user.dir") + File.separator + "files" + File.separator + fileName;
                                                        references.append(String.format("%d. 文件名：%s\n", i + 1, fileName));
                                                        references.append(String.format("   路径：%s\n", filePath));
                                                        if (i < fileNameList.size() - 1) {
                                                            references.append("\n");
                                                        }
                                                    }
                                                    sink.next(references.toString());
                                                } catch (Exception e) {
                                                    log.warn("在错误处理中添加文件引用信息失败: {}", e.getMessage());
                                                }
                                            }
                                            sink.complete();
                                        }
                                    }
                            );
                } catch (Exception e) {
                    log.error("多文件增强聊天启动失败: {}", e.getMessage(), e);
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.trim().isEmpty()) {
                        errorMsg = "AI服务启动失败，请稍后重试";
                    }
                    sink.next("抱歉，多文件增强聊天启动失败: " + errorMsg);
                    sink.complete();
                }
            });

        } catch (Exception e) {
            log.error("多文件增强聊天处理失败: {}", e.getMessage(), e);
            return Flux.just("抱歉，处理您的多文件聊天请求时出现了错误: " + e.getMessage());
        }
    }

    /**
     * 清理文档检索器缓存
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearDocumentCache() {
        try {
            if (documentService instanceof com.cjlu.finalversionwebsystem.service.impl.DocumentServiceImpl) {
                com.cjlu.finalversionwebsystem.service.impl.DocumentServiceImpl impl =
                        (com.cjlu.finalversionwebsystem.service.impl.DocumentServiceImpl) documentService;
                int oldSize = impl.getCacheSize();
                impl.clearRetrieverCache();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "缓存已清理");
                response.put("previousCacheSize", oldSize);
                response.put("currentCacheSize", impl.getCacheSize());

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无法访问缓存清理功能");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("清理缓存失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清理缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 清理所有缓存（包括内存中的向量存储）
     */
    @PostMapping("/clear-all-caches")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        try {
            if (documentService instanceof com.cjlu.finalversionwebsystem.service.impl.DocumentServiceImpl) {
                com.cjlu.finalversionwebsystem.service.impl.DocumentServiceImpl impl =
                        (com.cjlu.finalversionwebsystem.service.impl.DocumentServiceImpl) documentService;
                int oldSize = impl.getCacheSize();
                impl.clearAllCaches();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "所有缓存已清理");
                response.put("previousCacheSize", oldSize);
                response.put("currentCacheSize", impl.getCacheSize());

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无法访问缓存清理功能");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("清理所有缓存失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清理所有缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

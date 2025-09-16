package com.cjlu.finalversionwebsystem.service.Interface;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.TokenStream;

public interface ChatServiceInterface {
    //用于聊天的方法 - 使用TokenStream实现流式响应
    public TokenStream chat(/*@MemoryId String memoryId,*/ @UserMessage String message);

    //用于聊天的方法 - 返回完整响应字符串（非流式）
    public String chatSync(/*@MemoryId String memoryId,*/ @UserMessage String message);
}

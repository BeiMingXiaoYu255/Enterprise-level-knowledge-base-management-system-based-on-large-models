package com.cjlu.finalversionwebsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI模型配置实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {
    
    /**
     * 模型名称（如：qwen-plus, deepseek-chat, gpt-3.5-turbo等）
     */
    private String modelName;
    
    /**
     * API基础URL
     */
    private String baseUrl;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 模型提供商（如：qwen, deepseek, openai等）
     */
    private String provider;
    
    /**
     * 模型描述
     */
    private String description;
    
    /**
     * 是否启用请求日志
     */
    private boolean logRequests = true;
    
    /**
     * 是否启用响应日志
     */
    private boolean logResponses = true;
    
    /**
     * 嵌入模型名称（用于RAG功能）
     */
    private String embeddingModelName;
    
    /**
     * 验证配置是否完整
     */
    public boolean isValid() {
        return modelName != null && !modelName.trim().isEmpty() &&
               baseUrl != null && !baseUrl.trim().isEmpty() &&
               apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        return modelName + " (" + provider + ")";
    }
    
    /**
     * 创建Qwen模型配置
     */
    public static ModelConfig createQwenConfig(String apiKey) {
        ModelConfig config = new ModelConfig();
        config.setModelName("qwen-plus");
        config.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        config.setApiKey(apiKey);
        config.setProvider("qwen");
        config.setDescription("通义千问Plus模型");
        config.setEmbeddingModelName("text-embedding-v3");
        return config;
    }
    
    /**
     * 创建DeepSeek模型配置
     */
    public static ModelConfig createDeepSeekConfig(String apiKey) {
        ModelConfig config = new ModelConfig();
        config.setModelName("deepseek-chat");
        config.setBaseUrl("https://api.deepseek.com/v1");
        config.setApiKey(apiKey);
        config.setProvider("deepseek");
        config.setDescription("DeepSeek聊天模型");
        config.setEmbeddingModelName("text-embedding-ada-002"); // DeepSeek可能使用OpenAI兼容的嵌入模型
        return config;
    }
    
    /**
     * 创建OpenAI模型配置
     */
    public static ModelConfig createOpenAIConfig(String apiKey, String modelName) {
        ModelConfig config = new ModelConfig();
        config.setModelName(modelName != null ? modelName : "gpt-3.5-turbo");
        config.setBaseUrl("https://api.openai.com/v1");
        config.setApiKey(apiKey);
        config.setProvider("openai");
        config.setDescription("OpenAI " + (modelName != null ? modelName : "GPT-3.5"));
        config.setEmbeddingModelName("text-embedding-ada-002");
        return config;
    }
    
    /**
     * 创建自定义模型配置
     */
    public static ModelConfig createCustomConfig(String modelName, String baseUrl, String apiKey, String provider) {
        ModelConfig config = new ModelConfig();
        config.setModelName(modelName);
        config.setBaseUrl(baseUrl);
        config.setApiKey(apiKey);
        config.setProvider(provider);
        config.setDescription("自定义" + provider + "模型");
        return config;
    }
}

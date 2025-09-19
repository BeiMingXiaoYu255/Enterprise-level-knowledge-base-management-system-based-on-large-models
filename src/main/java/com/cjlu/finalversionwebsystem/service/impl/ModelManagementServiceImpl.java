package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.entity.ModelConfig;
import com.cjlu.finalversionwebsystem.service.Interface.ChatServiceInterface;
import com.cjlu.finalversionwebsystem.service.Interface.ModelManagementService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型管理服务实现类
 */
@Slf4j
@Service
public class ModelManagementServiceImpl implements ModelManagementService {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private ConfigurableEnvironment environment;
    
    // 当前模型配置
    private final AtomicReference<ModelConfig> currentModelConfig = new AtomicReference<>();
    
    // 当前模型实例
    private volatile OpenAiChatModel currentChatModel;
    private volatile OpenAiStreamingChatModel currentStreamingChatModel;
    private volatile EmbeddingModel currentEmbeddingModel;
    private volatile ChatServiceInterface currentChatService;
    
    // 默认模型配置
    private ModelConfig defaultModelConfig;
    
    // 支持的模型列表（经过验证的稳定模型）
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
        // 通义千问系列
        "qwen-plus",
        "qwen-turbo",
        "qwen-max",
        "qwen-long",
        "qwen-vl-plus",
        "qwen-vl-max",

        // Qwen2系列
        "qwen2-72b-instruct",
        "qwen2-57b-a14b-instruct",
        "qwen2-7b-instruct",
        "qwen2-1.5b-instruct",

        // Qwen1.5系列
        "qwen1.5-110b-chat",
        "qwen1.5-72b-chat",
        "qwen1.5-32b-chat",
        "qwen1.5-14b-chat",
        "qwen1.5-7b-chat",

        // DeepSeek系列
        "deepseek-v3",
        "deepseek-v3.1",
        "deepseek-r1",
        "deepseek-r1-distill-qwen-1.5b",
        "deepseek-r1-distill-qwen-7b",
        "deepseek-r1-distill-qwen-14b",
        "deepseek-r1-distill-qwen-32b",
        "deepseek-r1-distill-llama-8b"
        // 注意：deepseek-r1-distill-llama-70b 因权限问题已移除
    );

    // 预配置的API密钥映射
    private static final Map<String, String> DEFAULT_API_KEYS = new HashMap<String, String>() {{
        // 通义千问系列使用相同的API密钥
        put("qwen", "sk-948f9c0c86cc44438afc6c0bfb2f19ab");
        // DeepSeek系列通过阿里云百炼调用，使用相同的API密钥
        put("deepseek", "sk-948f9c0c86cc44438afc6c0bfb2f19ab");
        // OpenAI系列API密钥（需要用户配置）
        put("openai", "sk-your-openai-api-key");
    }};
    
    @PostConstruct
    public void init() {
        // 从配置文件加载默认模型配置
        loadDefaultModelConfig();
        log.info("模型管理服务初始化完成，当前模型: {}", getCurrentModelConfig().getDisplayName());
    }
    
    /**
     * 从配置文件加载默认模型配置
     */
    private void loadDefaultModelConfig() {
        try {
            String modelName = environment.getProperty("langchain4j.open-ai.chat-model.model-name", "qwen-plus");
            String baseUrl = environment.getProperty("langchain4j.open-ai.chat-model.base-url", "https://dashscope.aliyuncs.com/compatible-mode/v1");
            String apiKey = environment.getProperty("langchain4j.open-ai.chat-model.api-key", "");
            String embeddingModelName = environment.getProperty("langchain4j.open-ai.embedding-model.model-name", "text-embedding-v3");
            
            ModelConfig config = new ModelConfig();
            config.setModelName(modelName);
            config.setBaseUrl(baseUrl);
            config.setApiKey(apiKey);
            config.setEmbeddingModelName(embeddingModelName);
            config.setLogRequests(environment.getProperty("langchain4j.open-ai.chat-model.log-requests", Boolean.class, true));
            config.setLogResponses(environment.getProperty("langchain4j.open-ai.chat-model.log-responses", Boolean.class, true));
            
            // 根据baseUrl判断提供商
            if (baseUrl.contains("dashscope.aliyuncs.com")) {
                config.setProvider("qwen");
                config.setDescription("通义千问模型");
            } else if (baseUrl.contains("deepseek.com")) {
                config.setProvider("deepseek");
                config.setDescription("DeepSeek模型");
            } else if (baseUrl.contains("openai.com")) {
                config.setProvider("openai");
                config.setDescription("OpenAI模型");
            } else {
                config.setProvider("custom");
                config.setDescription("自定义模型");
            }
            
            defaultModelConfig = config;
            currentModelConfig.set(config);
            
            // 初始化模型实例
            initializeModels(config);
            
        } catch (Exception e) {
            log.error("加载默认模型配置失败: {}", e.getMessage(), e);
            // 使用硬编码的默认配置
            defaultModelConfig = ModelConfig.createQwenConfig("sk-948f9c0c86cc44438afc6c0bfb2f19ab");
            currentModelConfig.set(defaultModelConfig);
            initializeModels(defaultModelConfig);
        }
    }
    
    @Override
    public boolean switchModel(ModelConfig modelConfig) {
        if (modelConfig == null || !modelConfig.isValid()) {
            log.error("模型配置无效: {}", modelConfig);
            return false;
        }
        
        try {
            log.info("开始切换模型: {} -> {}", 
                getCurrentModelConfig().getDisplayName(), 
                modelConfig.getDisplayName());
            
            // 创建新的模型实例
            OpenAiChatModel newChatModel = createChatModel(modelConfig);
            OpenAiStreamingChatModel newStreamingChatModel = createStreamingChatModel(modelConfig);
            EmbeddingModel newEmbeddingModel = createEmbeddingModel(modelConfig);
            
            // 测试新模型是否可用
            String testResult = testModelInstance(newChatModel);
            if (!testResult.equals("SUCCESS")) {
                log.error("新模型测试失败: {}", testResult);
                return false;
            }
            
            // 创建新的聊天服务
            ChatServiceInterface newChatService = AiServices.create(ChatServiceInterface.class, newStreamingChatModel);
            
            // 原子性地更新所有模型实例
            currentChatModel = newChatModel;
            currentStreamingChatModel = newStreamingChatModel;
            currentEmbeddingModel = newEmbeddingModel;
            currentChatService = newChatService;
            currentModelConfig.set(modelConfig);
            
            // 更新Spring环境配置
            updateEnvironmentProperties(modelConfig);
            
            log.info("模型切换成功: {}", modelConfig.getDisplayName());
            return true;
            
        } catch (Exception e) {
            log.error("切换模型失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean switchModelByName(String modelName, String apiKey) {
        if (modelName == null || modelName.trim().isEmpty()) {
            log.error("模型名称不能为空");
            return false;
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("API密钥不能为空");
            return false;
        }

        ModelConfig config = createModelConfigByName(modelName.trim(), apiKey.trim());
        if (config == null) {
            log.error("不支持的模型名称: {}", modelName);
            return false;
        }

        return switchModel(config);
    }

    @Override
    public boolean quickSwitchModel(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            log.error("模型名称不能为空");
            return false;
        }

        String trimmedModelName = modelName.trim();
        log.info("收到快速切换模型请求: {}", trimmedModelName);

        // 获取模型对应的默认API密钥
        String apiKey = getDefaultApiKeyForModel(trimmedModelName);
        if (apiKey == null) {
            log.error("未找到模型 {} 的默认API密钥", trimmedModelName);
            return false;
        }

        // 使用现有的switchModelByName方法
        return switchModelByName(trimmedModelName, apiKey);
    }
    
    @Override
    public ModelConfig getCurrentModelConfig() {
        ModelConfig config = currentModelConfig.get();
        return config != null ? config : defaultModelConfig;
    }
    
    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(SUPPORTED_MODELS);
    }
    
    @Override
    public String testModelConfig(ModelConfig modelConfig) {
        if (modelConfig == null || !modelConfig.isValid()) {
            return "ERROR: 模型配置无效";
        }
        
        try {
            OpenAiChatModel testModel = createChatModel(modelConfig);
            return testModelInstance(testModel);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    @Override
    public boolean resetToDefaultModel() {
        if (defaultModelConfig == null) {
            log.error("默认模型配置不存在");
            return false;
        }
        
        return switchModel(defaultModelConfig);
    }
    
    @Override
    public String getModelStatus() {
        ModelConfig config = getCurrentModelConfig();
        StringBuilder status = new StringBuilder();
        status.append("当前模型状态:\n");
        status.append("模型名称: ").append(config.getModelName()).append("\n");
        status.append("提供商: ").append(config.getProvider()).append("\n");
        status.append("基础URL: ").append(config.getBaseUrl()).append("\n");
        status.append("描述: ").append(config.getDescription()).append("\n");
        status.append("聊天模型状态: ").append(currentChatModel != null ? "已加载" : "未加载").append("\n");
        status.append("流式模型状态: ").append(currentStreamingChatModel != null ? "已加载" : "未加载").append("\n");
        status.append("嵌入模型状态: ").append(currentEmbeddingModel != null ? "已加载" : "未加载").append("\n");
        status.append("聊天服务状态: ").append(currentChatService != null ? "已加载" : "未加载");
        return status.toString();
    }
    
    @Override
    public boolean saveModelConfig(ModelConfig modelConfig) {
        // 这里可以实现保存到配置文件的逻辑
        // 暂时只更新环境变量
        try {
            updateEnvironmentProperties(modelConfig);
            log.info("模型配置已保存: {}", modelConfig.getDisplayName());
            return true;
        } catch (Exception e) {
            log.error("保存模型配置失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean loadModelConfig() {
        try {
            loadDefaultModelConfig();
            return true;
        } catch (Exception e) {
            log.error("加载模型配置失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取当前聊天模型实例（供其他服务使用）
     */
    public OpenAiChatModel getCurrentChatModel() {
        return currentChatModel;
    }
    
    /**
     * 获取当前流式聊天模型实例（供其他服务使用）
     */
    public OpenAiStreamingChatModel getCurrentStreamingChatModel() {
        return currentStreamingChatModel;
    }
    
    /**
     * 获取当前嵌入模型实例（供其他服务使用）
     */
    public EmbeddingModel getCurrentEmbeddingModel() {
        return currentEmbeddingModel;
    }
    
    /**
     * 获取当前聊天服务实例（供其他服务使用）
     */
    public ChatServiceInterface getCurrentChatService() {
        return currentChatService;
    }

    /**
     * 更新支持的模型列表
     * @param newModels 新的模型列表
     * @return 更新是否成功
     */
    @Override
    public boolean updateSupportedModels(List<String> newModels) {
        try {
            log.info("开始更新支持的模型列表，新模型数量: {}", newModels.size());

            // 验证模型名称格式
            for (String model : newModels) {
                if (model == null || model.trim().isEmpty()) {
                    log.warn("发现无效的模型名称: {}", model);
                    return false;
                }
            }

            // 创建新的模型列表（合并现有和新增的）
            Set<String> updatedModels = new HashSet<>(SUPPORTED_MODELS);
            updatedModels.addAll(newModels);

            // 更新静态列表（注意：这只在当前运行时生效）
            SUPPORTED_MODELS.clear();
            SUPPORTED_MODELS.addAll(updatedModels);

            log.info("模型列表更新成功，当前支持{}个模型", SUPPORTED_MODELS.size());
            log.info("新增模型: {}", newModels);

            return true;

        } catch (Exception e) {
            log.error("更新支持的模型列表失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 移除不支持的模型
     * @param unsupportedModels 不支持的模型列表
     * @return 移除是否成功
     */
    @Override
    public boolean removeUnsupportedModels(List<String> unsupportedModels) {
        try {
            log.info("开始移除不支持的模型，数量: {}", unsupportedModels.size());

            if (unsupportedModels == null || unsupportedModels.isEmpty()) {
                log.info("没有需要移除的模型");
                return true;
            }

            // 记录移除前的模型数量
            int originalCount = SUPPORTED_MODELS.size();

            // 移除不支持的模型
            boolean removed = SUPPORTED_MODELS.removeAll(unsupportedModels);

            if (removed) {
                int newCount = SUPPORTED_MODELS.size();
                int removedCount = originalCount - newCount;

                log.info("成功移除{}个不支持的模型，剩余{}个模型", removedCount, newCount);
                log.info("移除的模型: {}", unsupportedModels);
                log.info("剩余的模型: {}", SUPPORTED_MODELS);

                return true;
            } else {
                log.warn("没有找到需要移除的模型");
                return false;
            }

        } catch (Exception e) {
            log.error("移除不支持的模型失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 初始化模型实例
     */
    private void initializeModels(ModelConfig config) {
        try {
            currentChatModel = createChatModel(config);
            currentStreamingChatModel = createStreamingChatModel(config);
            currentEmbeddingModel = createEmbeddingModel(config);
            currentChatService = AiServices.create(ChatServiceInterface.class, currentStreamingChatModel);
            log.info("模型实例初始化成功: {}", config.getDisplayName());
        } catch (Exception e) {
            log.error("初始化模型实例失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建聊天模型实例
     */
    private OpenAiChatModel createChatModel(ModelConfig config) {
        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofMinutes(2))
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses())
                .build();
    }

    /**
     * 创建流式聊天模型实例
     */
    private OpenAiStreamingChatModel createStreamingChatModel(ModelConfig config) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(Duration.ofMinutes(2))
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses())
                .build();
    }

    /**
     * 创建嵌入模型实例
     */
    private EmbeddingModel createEmbeddingModel(ModelConfig config) {
        String embeddingModelName = config.getEmbeddingModelName();
        if (embeddingModelName == null || embeddingModelName.trim().isEmpty()) {
            // 根据提供商设置默认嵌入模型
            if ("qwen".equals(config.getProvider())) {
                embeddingModelName = "text-embedding-v3";
            } else {
                embeddingModelName = "text-embedding-ada-002";
            }
        }

        return OpenAiEmbeddingModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(embeddingModelName)
                .timeout(Duration.ofMinutes(2))
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses())
                .build();
    }

    /**
     * 测试模型实例是否可用
     */
    private String testModelInstance(OpenAiChatModel model) {
        try {
            // 发送一个简单的测试消息
            String response = model.generate("Hello");
            if (response != null && !response.trim().isEmpty()) {
                return "SUCCESS";
            } else {
                return "ERROR: 模型响应为空";
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * 根据模型名称创建模型配置（动态支持所有API模型）
     */
    private ModelConfig createModelConfigByName(String modelName, String apiKey) {
        String lowerModelName = modelName.toLowerCase();

        // 检查模型是否在支持列表中
        if (!SUPPORTED_MODELS.contains(modelName)) {
            log.warn("不支持的模型名称: {}", modelName);
            return null;
        }

        // 根据模型名称前缀动态创建配置
        if (lowerModelName.startsWith("qwen") || lowerModelName.startsWith("qvq") || lowerModelName.startsWith("codeqwen")) {
            // 通义千问系列（包括所有Qwen变体）
            ModelConfig qwenConfig = ModelConfig.createQwenConfig(apiKey);
            qwenConfig.setModelName(modelName);
            return qwenConfig;

        } else if (lowerModelName.startsWith("deepseek")) {
            // DeepSeek系列（通过阿里云百炼）
            ModelConfig deepseekConfig = new ModelConfig();
            deepseekConfig.setModelName(modelName);
            deepseekConfig.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
            deepseekConfig.setApiKey(apiKey);
            deepseekConfig.setProvider("deepseek");
            deepseekConfig.setDescription("DeepSeek模型（通过阿里云百炼）");
            deepseekConfig.setEmbeddingModelName("text-embedding-v3");
            return deepseekConfig;

        } else if (lowerModelName.startsWith("gpt")) {
            // OpenAI系列
            return ModelConfig.createOpenAIConfig(apiKey, modelName);

        } else {
            // 其他模型默认使用阿里云百炼配置
            log.info("使用默认阿里云百炼配置处理模型: {}", modelName);
            ModelConfig defaultConfig = new ModelConfig();
            defaultConfig.setModelName(modelName);
            defaultConfig.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
            defaultConfig.setApiKey(apiKey);
            defaultConfig.setProvider("alibaba");
            defaultConfig.setDescription("阿里云百炼模型");
            defaultConfig.setEmbeddingModelName("text-embedding-v3");
            return defaultConfig;
        }
    }

    /**
     * 根据模型名称获取默认API密钥
     */
    private String getDefaultApiKeyForModel(String modelName) {
        String lowerModelName = modelName.toLowerCase();

        // 通义千问系列
        if (lowerModelName.startsWith("qwen")) {
            return DEFAULT_API_KEYS.get("qwen");
        }

        // DeepSeek系列
        if (lowerModelName.startsWith("deepseek")) {
            return DEFAULT_API_KEYS.get("deepseek");
        }

        // OpenAI系列
        if (lowerModelName.startsWith("gpt")) {
            return DEFAULT_API_KEYS.get("openai");
        }

        log.warn("未找到模型 {} 对应的默认API密钥", modelName);
        return null;
    }

    /**
     * 更新Spring环境属性
     */
    private void updateEnvironmentProperties(ModelConfig config) {
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("langchain4j.open-ai.chat-model.model-name", config.getModelName());
            properties.put("langchain4j.open-ai.chat-model.base-url", config.getBaseUrl());
            properties.put("langchain4j.open-ai.chat-model.api-key", config.getApiKey());
            properties.put("langchain4j.open-ai.chat-model.log-requests", config.isLogRequests());
            properties.put("langchain4j.open-ai.chat-model.log-responses", config.isLogResponses());

            properties.put("langchain4j.open-ai.streaming-chat-model.model-name", config.getModelName());
            properties.put("langchain4j.open-ai.streaming-chat-model.base-url", config.getBaseUrl());
            properties.put("langchain4j.open-ai.streaming-chat-model.api-key", config.getApiKey());
            properties.put("langchain4j.open-ai.streaming-chat-model.log-requests", config.isLogRequests());
            properties.put("langchain4j.open-ai.streaming-chat-model.log-responses", config.isLogResponses());

            if (config.getEmbeddingModelName() != null) {
                properties.put("langchain4j.open-ai.embedding-model.model-name", config.getEmbeddingModelName());
                properties.put("langchain4j.open-ai.embedding-model.base-url", config.getBaseUrl());
                properties.put("langchain4j.open-ai.embedding-model.api-key", config.getApiKey());
                properties.put("langchain4j.open-ai.embedding-model.log-requests", config.isLogRequests());
                properties.put("langchain4j.open-ai.embedding-model.log-responses", config.isLogResponses());
            }

            // 添加到环境变量
            MapPropertySource propertySource = new MapPropertySource("dynamicModelConfig", properties);
            environment.getPropertySources().addFirst(propertySource);

            log.debug("环境属性已更新: {}", config.getDisplayName());

        } catch (Exception e) {
            log.error("更新环境属性失败: {}", e.getMessage(), e);
        }
    }
}

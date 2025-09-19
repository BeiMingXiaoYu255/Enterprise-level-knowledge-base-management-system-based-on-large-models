package com.cjlu.finalversionwebsystem.service.Interface;

import com.cjlu.finalversionwebsystem.entity.ModelConfig;

import java.util.List;

/**
 * 模型管理服务接口
 * 支持运行时动态切换AI模型配置
 */
public interface ModelManagementService {
    
    /**
     * 切换到指定的模型配置
     * @param modelConfig 新的模型配置
     * @return 是否切换成功
     */
    boolean switchModel(ModelConfig modelConfig);
    
    /**
     * 根据模型名称切换模型（使用预定义配置）
     * @param modelName 模型名称
     * @param apiKey API密钥
     * @return 是否切换成功
     */
    boolean switchModelByName(String modelName, String apiKey);

    /**
     * 根据模型名称快速切换模型（使用预配置的API密钥）
     * @param modelName 模型名称
     * @return 是否切换成功
     */
    boolean quickSwitchModel(String modelName);
    
    /**
     * 获取当前使用的模型配置
     * @return 当前模型配置
     */
    ModelConfig getCurrentModelConfig();
    
    /**
     * 获取所有支持的模型列表
     * @return 支持的模型列表
     */
    List<String> getSupportedModels();
    
    /**
     * 测试模型配置是否可用
     * @param modelConfig 要测试的模型配置
     * @return 测试结果消息
     */
    String testModelConfig(ModelConfig modelConfig);
    
    /**
     * 重置为默认模型配置
     * @return 是否重置成功
     */
    boolean resetToDefaultModel();
    
    /**
     * 获取模型状态信息
     * @return 模型状态信息
     */
    String getModelStatus();
    
    /**
     * 保存模型配置到配置文件
     * @param modelConfig 要保存的模型配置
     * @return 是否保存成功
     */
    boolean saveModelConfig(ModelConfig modelConfig);
    
    /**
     * 从配置文件加载模型配置
     * @return 是否加载成功
     */
    boolean loadModelConfig();

    /**
     * 获取当前的聊天服务实例
     * @return 当前聊天服务实例
     */
    ChatServiceInterface getCurrentChatService();

    /**
     * 更新支持的模型列表
     * @param newModels 新的模型列表
     * @return 更新是否成功
     */
    boolean updateSupportedModels(List<String> newModels);

    /**
     * 移除不支持的模型
     * @param unsupportedModels 不支持的模型列表
     * @return 移除是否成功
     */
    boolean removeUnsupportedModels(List<String> unsupportedModels);
}

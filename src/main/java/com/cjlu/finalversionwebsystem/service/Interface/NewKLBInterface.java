package com.cjlu.finalversionwebsystem.service.Interface;

import java.util.List;
import java.util.Map;

public interface NewKLBInterface {
    /**
     * 插入新的知识库记录
     *
     * @param KLBName                 知识库名称
     * @param KLBCreator              创建者
     * @param primaryClassification   一级分类
     * @param secondaryClassification 二级分类
     * @param KLBReviseTime           修改时间
     * @param supportedDataFormats    支持的数据格式
     * @param KLBSearchStrategy       搜索策略
     * @param description             描述
     * @param creatTime               创建时间
     * @param KLBStatus               状态
     */
    void insertKLB(String KLBName, String KLBCreator, String primaryClassification, String secondaryClassification, String KLBReviseTime, String supportedDataFormats, String KLBSearchStrategy, String description, String creatTime, String KLBStatus);

    /**
     * 根据ID删除知识库记录
     *
     * @param id 知识库ID
     */
    void deleteKLBById(int id);

    /**
     * 根据ID更新知识库记录
     *
     * @param id                      知识库ID
     * @param KLBName                 知识库名称
     * @param KLBCreator              创建者
     * @param primaryClassification   一级分类
     * @param secondaryClassification 二级分类
     * @param KLBReviseTime           修改时间
     * @param supportedDataFormats    支持的数据格式
     * @param KLBSearchStrategy       搜索策略
     * @param description             描述
     * @param creatTime               创建时间
     * @param KLBStatus               状态
     */
    void updateKLBById(int id, String KLBName, String KLBCreator, String primaryClassification, String secondaryClassification, String KLBReviseTime, String supportedDataFormats, String KLBSearchStrategy, String description, String creatTime, String KLBStatus);

    
    
    /**
     * 根据ID查询知识库记录
     *
     * @param id 知识库ID
     * @return 知识库记录
     */
    Map<String, Object> selectKLBById(int id);

    /**
     * 查询所有知识库记录
     *
     * @return 知识库记录列表
     */
    List<Map<String, Object>> selectAllKLBs();

    /**
     * 根据创建者查询知识库记录
     *
     * @param KLBCreator 创建者
     * @return 知识库记录列表
     */
    List<Map<String, Object>> selectKLBByKLBCreator(String KLBCreator);

    /**
     * 根据知识库名称更新知识库记录
     *
     * @param KLBName                 知识库名称
     * @param KLBCreator              创建者
     * @param primaryClassification   一级分类
     * @param secondaryClassification 二级分类
     * @param KLBReviseTime           修改时间
     * @param supportedDataFormats    支持的数据格式
     * @param KLBSearchStrategy       搜索策略
     * @param description             描述
     * @param creatTime               创建时间
     * @param KLBStatus               状态
     */
    void updateKLBByKLBName(String KLBName, String KLBCreator, String primaryClassification, String secondaryClassification, String KLBReviseTime, String supportedDataFormats, String KLBSearchStrategy, String description, String creatTime, String KLBStatus);
}

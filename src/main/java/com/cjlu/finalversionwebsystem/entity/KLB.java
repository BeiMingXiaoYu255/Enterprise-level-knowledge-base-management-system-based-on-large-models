package com.cjlu.finalversionwebsystem.entity;

import java.util.List;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KLB {
    // 主键id
    private Integer id;
    // 知识库名称
    private String KLBName;
    // 创建者
    private String KLBCreator;
    // 一级分类
    private String primaryClassification;
    // 二级分类
    private String secondaryClassification;
    // 最后修订时间
    private String KLBReviseTime;
    // 支持的数据格式列表（业务层使用）
    private List<String> supportedDataFormats;
    // 支持的数据格式字符串（数据库存储用，逗号分隔）
    private String supportedDataFormatsStr;
    // 搜索策略
    private String KLBSearchStrategy;
    // 描述
    private String description;
    // 创建时间
    private String creatTime;
    // 状态
    private String KLBStatus;
    // 路径
    private String location;
    //访问次数
    private Integer accessCount;
    /**
     * 重写supportedDataFormats的setter方法，自动同步转换为字符串
     * 当设置List时，自动将其转换为逗号分隔的字符串存入supportedDataFormatsStr
     */
    public void setSupportedDataFormats(List<String> supportedDataFormats) {
        this.supportedDataFormats = supportedDataFormats;
        // 转换逻辑：List -> 逗号分隔字符串
        if (supportedDataFormats != null && !supportedDataFormats.isEmpty()) {
            this.supportedDataFormatsStr = String.join(",", supportedDataFormats);
        } else {
            this.supportedDataFormatsStr = null; // 空列表时设为null，避免存储空字符串
        }
    }

    /**
     * 新增方法：从字符串反转为List（用于查询时将数据库字符串转换为List）
     * 当从数据库查询到supportedDataFormatsStr时，调用此方法转换为List
     */
    public void setSupportedDataFormatsStr(String supportedDataFormatsStr) {
        this.supportedDataFormatsStr = supportedDataFormatsStr;
        // 转换逻辑：逗号分隔字符串 -> List
        if (supportedDataFormatsStr != null && !supportedDataFormatsStr.trim().isEmpty()) {
            this.supportedDataFormats = Arrays.asList(supportedDataFormatsStr.split(","));
        } else {
            this.supportedDataFormats = null; // 空字符串时设为null
        }
    }
}
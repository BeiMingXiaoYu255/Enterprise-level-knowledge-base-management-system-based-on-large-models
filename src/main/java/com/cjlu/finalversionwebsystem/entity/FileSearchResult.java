package com.cjlu.finalversionwebsystem.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 文件搜索结果实体类
 * 用于封装文件检索的结果信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSearchResult {
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件完整路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件扩展名
     */
    private String extension;
    
    /**
     * 最后修改时间
     */
    private Long lastModified;
    
    /**
     * 匹配类型：FILENAME（文件名匹配）、CONTENT（内容匹配）
     */
    private MatchType matchType;
    
    /**
     * 匹配的内容片段（仅当matchType为CONTENT时有值）
     */
    private List<String> matchedSnippets;
    
    /**
     * 匹配得分（0-1之间，1表示完全匹配）
     */
    private Double matchScore;
    
    /**
     * 匹配类型枚举
     */
    public enum MatchType {
        FILENAME,   // 文件名匹配
        CONTENT,    // 内容匹配
        BOTH        // 文件名和内容都匹配
    }
    
    /**
     * 构造函数 - 仅文件名匹配
     */
    public FileSearchResult(String fileName, String filePath, Long fileSize, 
                           String extension, Long lastModified, Double matchScore) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.extension = extension;
        this.lastModified = lastModified;
        this.matchType = MatchType.FILENAME;
        this.matchScore = matchScore;
    }
    
    /**
     * 构造函数 - 内容匹配
     */
    public FileSearchResult(String fileName, String filePath, Long fileSize, 
                           String extension, Long lastModified, 
                           List<String> matchedSnippets, Double matchScore) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.extension = extension;
        this.lastModified = lastModified;
        this.matchType = MatchType.CONTENT;
        this.matchedSnippets = matchedSnippets;
        this.matchScore = matchScore;
    }
}

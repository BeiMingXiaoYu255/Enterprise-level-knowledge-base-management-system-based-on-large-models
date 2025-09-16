package com.cjlu.finalversionwebsystem.service.Interface;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.List;

/**
 * 文档服务接口
 * 用于动态加载和索引文档
 */
public interface DocumentService {
    
    /**
     * 根据文件名加载文档并创建检索器
     * @param fileName 文件名
     * @return 内容检索器
     * @throws Exception 异常
     */
    ContentRetriever createRetrieverForFile(String fileName) throws Exception;
    
    /**
     * 加载指定文件的文档
     * @param fileName 文件名
     * @return 文档列表
     * @throws Exception 异常
     */
    List<Document> loadDocumentFromFile(String fileName) throws Exception;
    
    /**
     * 检查文件是否支持文档解析
     * @param fileName 文件名
     * @return 是否支持
     */
    boolean isSupportedDocument(String fileName);
    
    /**
     * 获取所有支持的文档类型
     * @return 支持的文档扩展名列表
     */
    List<String> getSupportedDocumentTypes();
}

package com.cjlu.finalversionwebsystem.service.Interface;

import java.util.List;

/**
 * 文件引用检测服务接口
 * 用于检测AI回答是否参考了files目录中的文件
 */
public interface FileReferenceDetectionService {
    
    /**
     * 检测AI回答是否参考了files目录中的文件
     * @param aiResponse AI的回答内容
     * @return 参考的文件名列表，如果没有参考任何文件则返回空列表
     */
    List<String> detectReferencedFiles(String aiResponse);
    
    /**
     * 根据用户输入的字符串搜索包含该字符串的文件
     * @param searchString 用户输入的搜索字符串
     * @return 包含该字符串的文件名列表
     */
    List<String> searchFilesContaining(String searchString);

    /**
     * 获取文件的完整路径
     * @param fileName 文件名
     * @return 文件的完整路径
     */
    String getFilePath(String fileName);
}

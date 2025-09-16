package com.cjlu.finalversionwebsystem.service.Interface;

import com.cjlu.finalversionwebsystem.entity.FileSearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {
    /**
     * 加密并保存文件
     * @param file 上传的文件
     * @param fileName 文件名
     * @return 保存的文件路径
     * @throws IOException IO异常
     */
    String encryptAndSaveFile(MultipartFile file, String fileName) throws IOException;

    /**
     * 解密并读取文件
     * @param fileName 文件名
     * @return 解密后的文件字节数组
     * @throws IOException IO异常
     */
    byte[] decryptAndReadFile(String fileName) throws IOException;

    /**
     * 检查文件是否存在
     * @param fileName 文件名
     * @return 是否存在
     */
    boolean fileExists(String fileName);

    /**
     * 删除文件
     * @param fileName 文件名
     * @return 是否删除成功
     */
    boolean deleteFile(String fileName);

    /**
     * 根据关键字搜索文件
     * @param keyword 搜索关键字
     * @return 匹配的文件搜索结果列表
     * @throws IOException IO异常
     */
    List<FileSearchResult> searchFiles(String keyword) throws IOException;

    /**
     * 获取所有文件列表
     * @return 文件名列表
     */
    List<String> getAllFileNames();

    /**
     * 读取文件内容为字符串
     * @param fileName 文件名
     * @return 文件内容字符串
     * @throws IOException IO异常
     */
    String readFileContent(String fileName) throws IOException;
}

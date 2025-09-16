package com.cjlu.finalversionwebsystem.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件测试工具类
 * 用于创建测试文件
 */
@Component
public class FileTestUtil {
    
    @Autowired
    private EncryptionUtil encryptionUtil;
    
    private static final String ROOT_PATH = System.getProperty("user.dir") + File.separator + "files";
    
    /**
     * 创建加密的测试文件
     */
    public void createEncryptedTestFile(String fileName, String content) throws Exception {
        // 确保目录存在
        File parentFile = new File(ROOT_PATH);
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        
        // 加密内容
        byte[] encryptedContent = encryptionUtil.encrypt(content.getBytes("UTF-8"));
        
        // 保存加密文件
        String filePath = ROOT_PATH + File.separator + fileName;
        Path path = Paths.get(filePath);
        Files.write(path, encryptedContent);
        
        System.out.println("加密测试文件创建成功: " + fileName);
    }
}

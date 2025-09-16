package com.cjlu.finalversionwebsystem.service.impl;

import cn.hutool.core.io.FileUtil;
import com.cjlu.finalversionwebsystem.entity.FileSearchResult;
import com.cjlu.finalversionwebsystem.service.Interface.FileService;
import com.cjlu.finalversionwebsystem.utils.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final String ROOT_PATH = System.getProperty("user.dir") + File.separator + "files";

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Override
    public String encryptAndSaveFile(MultipartFile file, String fileName) throws IOException {
        try {
            // 确保目录存在
            File parentFile = new File(ROOT_PATH);
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }

            // 读取文件内容
            byte[] fileContent = file.getBytes();

            // 加密文件内容
            byte[] encryptedContent = encryptionUtil.encrypt(fileContent);

            // 保存加密后的文件
            String filePath = ROOT_PATH + File.separator + fileName;
            Path path = Paths.get(filePath);
            Files.write(path, encryptedContent);

            log.info("文件加密保存成功: {}", fileName);
            return filePath;

        } catch (Exception e) {
            log.error("文件加密保存失败: {}", e.getMessage(), e);
            throw new IOException("文件加密保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] decryptAndReadFile(String fileName) throws IOException {
        try {
            String filePath = ROOT_PATH + File.separator + fileName;

            // 检查文件是否存在
            if (!FileUtil.exist(filePath)) {
                throw new IOException("文件不存在: " + fileName);
            }

            // 读取文件内容
            Path path = Paths.get(filePath);
            byte[] fileContent = Files.readAllBytes(path);

            // 尝试判断文件是否为加密格式
            if (isEncryptedFile(fileContent)) {
                // 解密文件内容
                byte[] decryptedContent = encryptionUtil.decrypt(fileContent);
                log.info("文件解密读取成功: {}", fileName);
                return decryptedContent;
            } else {
                // 直接返回普通文本文件内容
                log.info("文件直接读取成功（普通文本文件）: {}", fileName);
                return fileContent;
            }

        } catch (Exception e) {
            log.error("文件读取失败: {}", e.getMessage(), e);
            throw new IOException("文件读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断文件是否为加密格式
     * 通过尝试解密来判断，如果解密失败则认为是普通文本文件
     */
    private boolean isEncryptedFile(byte[] fileContent) {
        try {
            // 尝试解密，如果成功则认为是加密文件
            encryptionUtil.decrypt(fileContent);
            return true;
        } catch (Exception e) {
            // 解密失败，认为是普通文本文件
            log.debug("文件不是加密格式，将作为普通文本文件处理");
            return false;
        }
    }

    @Override
    public boolean fileExists(String fileName) {
        String filePath = ROOT_PATH + File.separator + fileName;
        return FileUtil.exist(filePath);
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            String filePath = ROOT_PATH + File.separator + fileName;
            if (FileUtil.exist(filePath)) {
                FileUtil.del(filePath);
                log.info("文件删除成功: {}", fileName);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<FileSearchResult> searchFiles(String keyword) throws IOException {
        List<FileSearchResult> results = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        String normalizedKeyword = keyword.toLowerCase().trim();
        File rootDir = new File(ROOT_PATH);

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            log.warn("文件根目录不存在: {}", ROOT_PATH);
            return results;
        }

        File[] files = rootDir.listFiles();
        if (files == null) {
            return results;
        }

        for (File file : files) {
            if (file.isFile()) {
                try {
                    FileSearchResult result = searchInFile(file, normalizedKeyword);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    log.warn("搜索文件 {} 时出错: {}", file.getName(), e.getMessage());
                }
            }
        }

        // 按匹配得分排序，得分高的在前
        results.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

        log.info("搜索关键字 '{}' 找到 {} 个匹配文件", keyword, results.size());
        return results;
    }

    @Override
    public List<String> getAllFileNames() {
        List<String> fileNames = new ArrayList<>();
        File rootDir = new File(ROOT_PATH);

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            log.warn("文件根目录不存在: {}", ROOT_PATH);
            return fileNames;
        }

        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }
        }

        return fileNames;
    }

    /**
     * 在单个文件中搜索关键字
     */
    private FileSearchResult searchInFile(File file, String keyword) throws IOException {
        String fileName = file.getName();
        String filePath = file.getAbsolutePath();
        long fileSize = file.length();
        String extension = FileUtil.extName(fileName);
        long lastModified = file.lastModified();

        boolean fileNameMatch = fileName.toLowerCase().contains(keyword);
        boolean contentMatch = false;
        List<String> matchedSnippets = new ArrayList<>();
        double matchScore = 0.0;

        // 检查文件名匹配
        if (fileNameMatch) {
            matchScore += 0.5; // 文件名匹配给0.5分
        }

        // 检查内容匹配（仅对文本类型文件）
        if (isTextFile(extension)) {
            try {
                // 尝试直接读取文件内容（普通文本文件）
                String content;
                try {
                    content = FileUtil.readUtf8String(file);
                } catch (Exception e) {
                    // 如果直接读取失败，尝试解密读取（加密文件）
                    log.debug("直接读取文件失败，尝试解密读取: {}", fileName);
                    byte[] decryptedContent = decryptAndReadFile(fileName);
                    content = new String(decryptedContent, StandardCharsets.UTF_8);
                }

                // 搜索内容中的关键字
                String[] lines = content.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].toLowerCase();
                    if (line.contains(keyword)) {
                        contentMatch = true;
                        // 添加匹配的行，限制长度
                        String snippet = lines[i].trim();
                        if (snippet.length() > 200) {
                            snippet = snippet.substring(0, 200) + "...";
                        }
                        matchedSnippets.add("第" + (i + 1) + "行: " + snippet);

                        // 限制匹配片段数量
                        if (matchedSnippets.size() >= 3) {
                            break;
                        }
                    }
                }

                if (contentMatch) {
                    matchScore += 0.5; // 内容匹配给0.5分
                }

            } catch (Exception e) {
                log.debug("无法读取文件内容进行搜索: {}", fileName);
            }
        }

        // 如果没有任何匹配，返回null
        if (!fileNameMatch && !contentMatch) {
            return null;
        }

        // 创建搜索结果
        FileSearchResult result = new FileSearchResult();
        result.setFileName(fileName);
        result.setFilePath(filePath);
        result.setFileSize(fileSize);
        result.setExtension(extension);
        result.setLastModified(lastModified);
        result.setMatchScore(matchScore);

        if (fileNameMatch && contentMatch) {
            result.setMatchType(FileSearchResult.MatchType.BOTH);
            result.setMatchedSnippets(matchedSnippets);
        } else if (contentMatch) {
            result.setMatchType(FileSearchResult.MatchType.CONTENT);
            result.setMatchedSnippets(matchedSnippets);
        } else {
            result.setMatchType(FileSearchResult.MatchType.FILENAME);
        }

        return result;
    }

    /**
     * 判断是否为文本文件
     */
    private boolean isTextFile(String extension) {
        if (extension == null) {
            return false;
        }

        Set<String> textExtensions = Set.of(
            "txt", "md", "csv", "json", "xml", "html", "htm",
            "css", "js", "java", "py", "cpp", "c", "h", "sql"
        );

        return textExtensions.contains(extension.toLowerCase());
    }

    @Override
    public String readFileContent(String fileName) throws IOException {
        String filePath = ROOT_PATH + File.separator + fileName;
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("文件不存在: " + fileName);
        }

        try {
            // 首先尝试解密读取
            byte[] encryptedContent = Files.readAllBytes(file.toPath());
            byte[] decryptedContent = encryptionUtil.decrypt(encryptedContent);
            String content = new String(decryptedContent, StandardCharsets.UTF_8);
            log.info("文件解密读取成功: {}", fileName);
            return content;
        } catch (Exception e) {
            // 如果解密失败，尝试直接读取（普通文本文件）
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                log.info("文件直接读取成功（普通文本文件）: {}", fileName);
                return content;
            } catch (Exception e2) {
                log.error("文件读取失败: {}, 解密错误: {}, 直接读取错误: {}", fileName, e.getMessage(), e2.getMessage());
                throw new IOException("无法读取文件: " + fileName, e2);
            }
        }
    }
}

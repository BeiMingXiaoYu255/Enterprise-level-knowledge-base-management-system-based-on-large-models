package com.cjlu.finalversionwebsystem.File.service;


import com.cjlu.finalversionwebsystem.File.model.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {


    private static String uploadDir = "D:\\基于大模型的企业级知识管理系统\\知识库";

    // 初始化上传目录的方法
    private void initUploadDirectory() {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    // 存储文件
    public String storeFile(MultipartFile file) throws IOException {
        // 生成唯一文件名，避免冲突
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String baseFileName = originalFileName.substring(0, originalFileName.length() - fileExtension.length());
        String uniqueFileName = originalFileName;
        int counter = 1;

        while (fileExists(uniqueFileName)) {
            uniqueFileName = baseFileName + "(" + counter + ")" + fileExtension;
            counter++;
        }

        Path targetLocation = Paths.get(uploadDir).resolve(uniqueFileName);

        // 保存文件
        file.transferTo(targetLocation);

        return uniqueFileName;
    }

    // 获取文件列表
    public List<FileInfo> getFileList() throws IOException {
        File directory = new File(uploadDir);
        File[] files = directory.listFiles();

        if (files == null) {
            return new ArrayList<>();
        }

        return List.of(files)
                .stream()
                .filter(File::isFile)
                .map(file -> {
                    try {
                        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        return new FileInfo(
                                file.getName(),
                                file.length(),
                                new Date(attr.creationTime().toMillis())
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .sorted((f1, f2) -> f2.getUploadDate().compareTo(f1.getUploadDate())) // 按上传时间降序排列
                .collect(Collectors.toList());
    }

    // 获取文件路径
    public Path getFilePath(String fileName) {
        return Paths.get(uploadDir).resolve(fileName).normalize();
    }

    // 检查文件是否存在
    public boolean fileExists(String fileName) {
        Path filePath = getFilePath(fileName);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
}

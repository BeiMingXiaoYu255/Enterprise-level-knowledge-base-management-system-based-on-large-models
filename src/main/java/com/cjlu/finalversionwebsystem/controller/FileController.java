package com.cjlu.finalversionwebsystem.controller;

import cn.hutool.core.io.FileUtil;

import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.service.Interface.FileService;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    private static final String ROOT_PATH = System.getProperty("user.dir") + File.separator + "files"; //文件存储的目录

    // 允许的文件类型
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", // 图片
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", // 文档
            "txt", "md", "csv", // 文本
            "zip", "rar", "7z", // 压缩包
            "mp4", "avi", "mov", "wmv", // 视频
            "mp3", "wav", "flac" // 音频
    );

    // 最大文件大小 (50MB)
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Autowired
    private FileService fileService;

    @PostMapping("/upload") //文件上传
    public Result upload(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件是否为空
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 验证文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                return Result.error("文件大小不能超过50MB");
            }

            String originalFilename = file.getOriginalFilename(); //文件的原始名称
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return Result.error("文件名不能为空");
            }

            String mainName = FileUtil.mainName(originalFilename); //文件的主名称
            String extName = FileUtil.extName(originalFilename); //文件的扩展名 - 修复bug：应该传入originalFilename而不是字符串

            // 验证文件类型
            if (extName == null || !ALLOWED_EXTENSIONS.contains(extName.toLowerCase())) {
                return Result.error("不支持的文件类型，支持的类型：" + String.join(", ", ALLOWED_EXTENSIONS));
            }

            // 生成唯一文件名
            String fileName = originalFilename;
            if (fileService.fileExists(fileName)) {
                fileName = System.currentTimeMillis() + "_" + mainName + "." + extName;
            }

            // 加密保存文件
            fileService.encryptAndSaveFile(file, fileName);

            String url = "http://localhost:8080/file/download/" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            log.info("文件上传成功: {}", fileName);
            return Result.success(url);

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response) {
        try {
            // 验证文件名
            if (fileName == null || fileName.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("文件名不能为空");
                return;
            }

            // 检查文件是否存在
            if (!fileService.fileExists(fileName)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在");
                return;
            }

            // 解密并读取文件
            byte[] decryptedBytes = fileService.decryptAndReadFile(fileName);

            // 设置响应头
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");

            // 根据文件扩展名设置Content-Type
            String extName = FileUtil.extName(fileName);
            if (extName != null) {
                switch (extName.toLowerCase()) {
                    case "jpg":
                    case "jpeg":
                        response.setContentType("image/jpeg");
                        break;
                    case "png":
                        response.setContentType("image/png");
                        break;
                    case "gif":
                        response.setContentType("image/gif");
                        break;
                    case "pdf":
                        response.setContentType("application/pdf");
                        break;
                    case "txt":
                        response.setContentType("text/plain; charset=utf-8");
                        break;
                    case "doc":
                    case "docx":
                        response.setContentType("application/msword");
                        break;
                    case "xls":
                    case "xlsx":
                        response.setContentType("application/vnd.ms-excel");
                        break;
                    default:
                        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                }
            } else {
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            }

            response.setContentLength(decryptedBytes.length);

            // 写入响应
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(decryptedBytes);
            outputStream.flush();

            log.info("文件下载成功: {}", fileName);

        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("文件下载失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 获取文件列表
     */
    @GetMapping("/list")
    public Result listFiles() {
        try {
            File dir = new File(ROOT_PATH);
            if (!dir.exists() || !dir.isDirectory()) {
                return Result.success(Arrays.asList());
            }

            File[] files = dir.listFiles();
            if (files == null) {
                return Result.success(Arrays.asList());
            }

            List<FileInfo> fileInfos = Arrays.stream(files)
                    .filter(File::isFile)
                    .map(file -> {
                        FileInfo info = new FileInfo();
                        info.setName(file.getName());
                        info.setSize(file.length());
                        info.setLastModified(file.lastModified());
                        info.setExtension(FileUtil.extName(file.getName()));
                        return info;
                    })
                    .collect(Collectors.toList());

            return Result.success(fileInfos);

        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return Result.error("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete/{fileName}")
    public Result deleteFile(@PathVariable String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                return Result.error("文件名不能为空");
            }

            boolean deleted = fileService.deleteFile(fileName);
            if (deleted) {
                log.info("文件删除成功: {}", fileName);
                return Result.success("文件删除成功");
            } else {
                return Result.error("文件不存在或删除失败");
            }

        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return Result.error("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     */
    @GetMapping("/exists/{fileName}")
    public Result fileExists(@PathVariable String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                return Result.error("文件名不能为空");
            }

            boolean exists = fileService.fileExists(fileName);
            return Result.success(exists);

        } catch (Exception e) {
            log.error("检查文件存在性失败: {}", e.getMessage(), e);
            return Result.error("检查文件存在性失败: " + e.getMessage());
        }
    }

    /**
     * 文件信息类
     */
    public static class FileInfo {
        private String name;
        private long size;
        private long lastModified;
        private String extension;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
    }
}

package com.cjlu.finalversionwebsystem.File.Controller;

import com.cjlu.finalversionwebsystem.File.service.FileStorageService;
import com.cjlu.finalversionwebsystem.File.model.FileInfo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RequestMapping("FILE")
@RestController
public class NewFileController {

    private final FileStorageService fileStorageService;

    public NewFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // 上传文件
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "请选择要上传的文件"));
            }

            String fileName = fileStorageService.storeFile(file);
            return ResponseEntity.ok(Map.of(
                    "message", "文件上传成功",
                    "filename", fileName,
                    "originalname", file.getOriginalFilename(),
                    "size", file.getSize()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "文件上传失败: " + e.getMessage()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 获取文件列表
    @GetMapping("/files")
    public ResponseEntity<List<FileInfo>> getFileList() {
        try {
            List<FileInfo> fileList = fileStorageService.getFileList();
            return ResponseEntity.ok(fileList);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 下载文件
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            // 检查文件是否存在
            if (!fileStorageService.fileExists(fileName)) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = fileStorageService.getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            // 设置响应头，触发文件下载
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}


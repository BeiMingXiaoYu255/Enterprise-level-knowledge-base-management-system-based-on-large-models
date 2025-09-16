package com.cjlu.finalversionwebsystem.service;

import com.cjlu.finalversionwebsystem.entity.FileSearchResult;
import com.cjlu.finalversionwebsystem.service.impl.FileServiceImpl;
import com.cjlu.finalversionwebsystem.utils.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * FileService测试类
 */
public class FileServiceTest {

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private FileServiceImpl fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置临时目录作为文件根目录
        System.setProperty("user.dir", tempDir.toString());
    }

    @Test
    void testSearchFiles_EmptyKeyword() throws IOException {
        List<FileSearchResult> results = fileService.searchFiles("");
        assertTrue(results.isEmpty());
        
        results = fileService.searchFiles(null);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchFiles_NoFiles() throws IOException {
        List<FileSearchResult> results = fileService.searchFiles("test");
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchFiles_FileNameMatch() throws IOException {
        // 创建测试文件
        Path filesDir = tempDir.resolve("files");
        Files.createDirectories(filesDir);
        
        Path testFile = filesDir.resolve("test_document.txt");
        Files.write(testFile, "This is a test content".getBytes());
        
        // 模拟加密/解密
        try {
            when(encryptionUtil.decrypt(any())).thenReturn("This is a test content".getBytes());
        } catch (Exception e) {
            // 处理异常
        }
        
        List<FileSearchResult> results = fileService.searchFiles("test");
        
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        
        FileSearchResult result = results.get(0);
        assertEquals("test_document.txt", result.getFileName());
        assertTrue(result.getMatchType() == FileSearchResult.MatchType.FILENAME || 
                  result.getMatchType() == FileSearchResult.MatchType.BOTH);
    }

    @Test
    void testGetAllFileNames_EmptyDirectory() {
        List<String> fileNames = fileService.getAllFileNames();
        assertTrue(fileNames.isEmpty());
    }

    @Test
    void testGetAllFileNames_WithFiles() throws IOException {
        // 创建测试文件
        Path filesDir = tempDir.resolve("files");
        Files.createDirectories(filesDir);
        
        Files.write(filesDir.resolve("file1.txt"), "content1".getBytes());
        Files.write(filesDir.resolve("file2.pdf"), "content2".getBytes());
        
        List<String> fileNames = fileService.getAllFileNames();
        
        assertEquals(2, fileNames.size());
        assertTrue(fileNames.contains("file1.txt"));
        assertTrue(fileNames.contains("file2.pdf"));
    }

    @Test
    void testFileExists() throws IOException {
        // 创建测试文件
        Path filesDir = tempDir.resolve("files");
        Files.createDirectories(filesDir);
        
        Path testFile = filesDir.resolve("existing_file.txt");
        Files.write(testFile, "content".getBytes());
        
        assertTrue(fileService.fileExists("existing_file.txt"));
        assertFalse(fileService.fileExists("non_existing_file.txt"));
    }
}

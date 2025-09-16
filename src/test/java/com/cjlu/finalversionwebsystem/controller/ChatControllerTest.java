package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.entity.FileSearchResult;
import com.cjlu.finalversionwebsystem.service.Interface.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController测试类
 */
@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSearchFiles_Success() throws Exception {
        // 准备测试数据
        FileSearchResult result1 = new FileSearchResult();
        result1.setFileName("test1.txt");
        result1.setFilePath("/files/test1.txt");
        result1.setMatchType(FileSearchResult.MatchType.FILENAME);
        result1.setMatchScore(0.8);

        FileSearchResult result2 = new FileSearchResult();
        result2.setFileName("test2.pdf");
        result2.setFilePath("/files/test2.pdf");
        result2.setMatchType(FileSearchResult.MatchType.CONTENT);
        result2.setMatchScore(0.6);

        List<FileSearchResult> mockResults = Arrays.asList(result1, result2);

        // 模拟服务调用
        when(fileService.searchFiles(anyString())).thenReturn(mockResults);

        // 执行测试
        mockMvc.perform(get("/ai/search-files")
                .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fileName").value("test1.txt"))
                .andExpect(jsonPath("$.data[1].fileName").value("test2.pdf"));
    }

    @Test
    void testSearchFiles_EmptyKeyword() throws Exception {
        mockMvc.perform(get("/ai/search-files")
                .param("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索关键字不能为空"));
    }

    @Test
    void testGetAllFiles_Success() throws Exception {
        // 准备测试数据
        List<String> mockFileNames = Arrays.asList("file1.txt", "file2.pdf", "file3.doc");

        // 模拟服务调用
        when(fileService.getAllFileNames()).thenReturn(mockFileNames);

        // 执行测试
        mockMvc.perform(get("/ai/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0]").value("file1.txt"))
                .andExpect(jsonPath("$.data[1]").value("file2.pdf"))
                .andExpect(jsonPath("$.data[2]").value("file3.doc"));
    }
}

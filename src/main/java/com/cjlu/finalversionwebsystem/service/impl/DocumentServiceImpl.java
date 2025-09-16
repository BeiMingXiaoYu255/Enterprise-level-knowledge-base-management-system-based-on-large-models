package com.cjlu.finalversionwebsystem.service.impl;

import cn.hutool.core.io.FileUtil;
import com.cjlu.finalversionwebsystem.service.Interface.DocumentService;
import com.cjlu.finalversionwebsystem.service.Interface.FileService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {
    
    private static final String ROOT_PATH = System.getProperty("user.dir") + File.separator + "files";
    
    // 支持的文档类型
    private static final List<String> SUPPORTED_DOCUMENT_TYPES = Arrays.asList(
            "pdf", "txt", "md", "doc", "docx"
    );
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private FileService fileService;
    
    @Override
    public ContentRetriever createRetrieverForFile(String fileName) throws Exception {
        if (!isSupportedDocument(fileName)) {
            throw new IllegalArgumentException("不支持的文档类型: " + fileName);
        }
        
        if (!fileService.fileExists(fileName)) {
            throw new IllegalArgumentException("文件不存在: " + fileName);
        }
        
        // 加载文档
        List<Document> documents = loadDocumentFromFile(fileName);
        
        if (documents.isEmpty()) {
            throw new IllegalArgumentException("无法从文件中加载文档: " + fileName);
        }
        
        // 创建全新的向量数据库，确保每次都是独立的存储空间
        InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment> store = new InMemoryEmbeddingStore<>();
        log.info("为文件 {} 创建新的向量存储", fileName);
        
        // 构建文档分割器 - 增加分割大小以减少文本段数量，避免超过OpenAI API批处理限制
        // 使用更大的分割大小：1000字符，重叠200字符
        DocumentSplitter splitter = DocumentSplitters.recursive(1000, 200);
        
        // 手动处理文档分割和嵌入，以控制批处理大小
        try {
            // 先分割文档
            List<dev.langchain4j.data.segment.TextSegment> allSegments = new ArrayList<>();
            for (Document document : documents) {
                List<dev.langchain4j.data.segment.TextSegment> segments = splitter.split(document);
                allSegments.addAll(segments);
            }

            log.info("文档分割完成，共生成 {} 个文本段", allSegments.size());

            // 分批处理嵌入，每批最多8个（留出安全边际）
            int batchSize = 8;
            for (int i = 0; i < allSegments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allSegments.size());
                List<dev.langchain4j.data.segment.TextSegment> batch = allSegments.subList(i, endIndex);

                log.debug("处理第 {} 批文本段，包含 {} 个段落", (i / batchSize + 1), batch.size());

                // 为这批文本段生成嵌入
                List<dev.langchain4j.data.embedding.Embedding> embeddings = embeddingModel.embedAll(batch).content();

                // 存储到向量数据库
                for (int j = 0; j < batch.size(); j++) {
                    store.add(embeddings.get(j), batch.get(j));
                }
            }

            log.info("所有文本段嵌入完成");

        } catch (Exception e) {
            log.error("手动处理文档嵌入时出错: {}", e.getMessage(), e);
            throw new RuntimeException("文档嵌入处理失败: " + e.getMessage(), e);
        }
        
        // 创建检索器
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .minScore(0.5)
                .maxResults(3)
                .embeddingModel(embeddingModel)
                .build();
        
        log.info("为文件 {} 创建检索器成功", fileName);
        return retriever;
    }
    
    @Override
    public List<Document> loadDocumentFromFile(String fileName) throws Exception {
        String extName = FileUtil.extName(fileName);
        if (extName == null) {
            throw new IllegalArgumentException("无法确定文件类型: " + fileName);
        }
        
        // 解密文件内容并保存到临时文件
        byte[] decryptedContent = fileService.decryptAndReadFile(fileName);
        
        // 创建临时文件
        Path tempFile = createTempFile(fileName, decryptedContent);
        
        try {
            List<Document> documents;
            
            switch (extName.toLowerCase()) {
                case "pdf":
                    // 使用PDF解析器
                    documents = List.of(FileSystemDocumentLoader.loadDocument(
                            tempFile,
                            new ApachePdfBoxDocumentParser()
                    ));
                    break;
                case "txt":
                case "md":
                    // 直接加载文本文件
                    documents = List.of(FileSystemDocumentLoader.loadDocument(tempFile));
                    break;
                case "doc":
                case "docx":
                    // 对于Word文档，暂时作为文本处理
                    // 实际项目中可能需要专门的Word文档解析器
                    documents = List.of(FileSystemDocumentLoader.loadDocument(tempFile));
                    break;
                default:
                    throw new IllegalArgumentException("不支持的文档类型: " + extName);
            }
            
            log.info("从文件 {} 加载了 {} 个文档", fileName, documents.size());
            return documents;
            
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", tempFile, e);
            }
        }
    }
    
    @Override
    public boolean isSupportedDocument(String fileName) {
        String extName = FileUtil.extName(fileName);
        return extName != null && SUPPORTED_DOCUMENT_TYPES.contains(extName.toLowerCase());
    }
    
    @Override
    public List<String> getSupportedDocumentTypes() {
        return Collections.unmodifiableList(SUPPORTED_DOCUMENT_TYPES);
    }
    
    /**
     * 创建临时文件
     */
    private Path createTempFile(String fileName, byte[] content) throws IOException {
        String extName = FileUtil.extName(fileName);
        String tempFileName = "temp_" + System.currentTimeMillis();
        if (extName != null) {
            tempFileName += "." + extName;
        }
        
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), tempFileName);
        Files.write(tempFile, content);
        
        return tempFile;
    }
}

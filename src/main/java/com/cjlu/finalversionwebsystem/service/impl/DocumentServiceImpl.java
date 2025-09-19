package com.cjlu.finalversionwebsystem.service.impl;

import cn.hutool.core.io.FileUtil;
import com.cjlu.finalversionwebsystem.service.Interface.DocumentService;
import com.cjlu.finalversionwebsystem.service.Interface.FileService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    // 缓存检索器，避免重复处理相同文件组合
    private final ConcurrentHashMap<String, ContentRetriever> retrieverCache = new ConcurrentHashMap<>();

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

        // 调试：验证读取的文件内容
        String contentPreview = new String(decryptedContent, StandardCharsets.UTF_8);
        String preview = contentPreview.length() > 100 ? contentPreview.substring(0, 100) + "..." : contentPreview;
        log.info("文件 {} 读取内容预览: [{}], 字节长度: {}", fileName, preview, decryptedContent.length);

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
                case "doc":
                case "docx":
                    // 直接从内容创建文档，避免LangChain4j文档加载器的潜在问题
                    String textContent = new String(decryptedContent, StandardCharsets.UTF_8);

                    // 调试：验证文本内容
                    String textPreview = textContent.length() > 100 ? textContent.substring(0, 100) + "..." : textContent;
                    log.info("🔍 文件 {} 解密后内容预览: [{}], 字符长度: {}", fileName, textPreview, textContent.length());

                    // 直接创建Document对象，避免文件系统加载器的问题
                    Metadata metadata = new Metadata();
                    metadata.add("file_name", fileName);
                    metadata.add("source_file", fileName);

                    // 添加唯一标识符确保文档不会混淆
                    String uniqueId = fileName + "_" + System.currentTimeMillis() + "_" + textContent.hashCode();
                    metadata.add("unique_id", uniqueId);

                    Document document = Document.from(textContent, metadata);
                    documents = List.of(document);

                    log.info("✅ 直接创建文档: {}, 内容长度: {}, 唯一ID: {}", fileName, textContent.length(), uniqueId);

                    // 验证创建的文档内容
                    String docContentPreview = document.text().length() > 100 ? document.text().substring(0, 100) + "..." : document.text();
                    log.info("🔍 验证创建的文档内容: 文件={}, 内容预览=[{}]", fileName, docContentPreview);
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

    @Override
    public ContentRetriever createRetrieverForMultipleFiles(List<String> fileNames) throws Exception {
        if (fileNames == null || fileNames.isEmpty()) {
            throw new IllegalArgumentException("文件名列表不能为空");
        }

        // 创建缓存键（排序后的文件名组合）
        String cacheKey = fileNames.stream().sorted().collect(Collectors.joining(","));

        // 检查缓存
        ContentRetriever cachedRetriever = retrieverCache.get(cacheKey);
        if (cachedRetriever != null) {
            log.info("使用缓存的检索器，文件组合: {}", cacheKey);
            return cachedRetriever;
        }

        log.info("开始为多个文件创建检索器，文件数量: {}", fileNames.size());

        // 验证所有文件
        for (String fileName : fileNames) {
            if (!isSupportedDocument(fileName)) {
                throw new IllegalArgumentException("不支持的文档类型: " + fileName);
            }
            if (!fileService.fileExists(fileName)) {
                throw new IllegalArgumentException("文件不存在: " + fileName);
            }
        }

        // 加载所有文档
        List<Document> allDocuments = loadDocumentsFromMultipleFiles(fileNames);

        if (allDocuments.isEmpty()) {
            throw new IllegalArgumentException("无法从文件中加载任何文档");
        }

        // 创建全新的向量数据库，用于存储所有文件的内容
        InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment> store = new InMemoryEmbeddingStore<>();
        log.info("为多个文件创建新的向量存储，文档总数: {}", allDocuments.size());

        // 构建文档分割器 - 增加块大小减少段数，提升性能
        DocumentSplitter splitter = DocumentSplitters.recursive(1500, 300);

        // 手动处理文档分割和嵌入
        try {
            // 先分割所有文档
            List<dev.langchain4j.data.segment.TextSegment> allSegments = new ArrayList<>();
            for (Document document : allDocuments) {
                List<dev.langchain4j.data.segment.TextSegment> segments = splitter.split(document);
                allSegments.addAll(segments);
            }

            log.info("文档分割完成，总段数: {}", allSegments.size());

            // 调试：打印所有分割的段内容
            for (int i = 0; i < allSegments.size(); i++) {
                dev.langchain4j.data.segment.TextSegment segment = allSegments.get(i);
                String segmentText = segment.text();
                String preview = segmentText.length() > 100 ? segmentText.substring(0, 100) + "..." : segmentText;

                // 获取源文件信息
                String sourceFile = "未知";
                String uniqueId = "未知";
                if (segment.metadata() != null && segment.metadata().asMap() != null) {
                    sourceFile = segment.metadata().asMap().getOrDefault("source_file", "未知").toString();
                    uniqueId = segment.metadata().asMap().getOrDefault("unique_id", "未知").toString();
                }

                log.info("🔍 段 {}: 来源文件=[{}], 唯一ID=[{}], 预览=[{}], 长度={}",
                        i + 1, sourceFile, uniqueId, preview, segmentText.length());

                // 检查段的元数据
                if (segment.metadata() != null && segment.metadata().asMap() != null) {
                    log.info("📋 段 {} 完整元数据: {}", i + 1, segment.metadata().asMap());
                }
            }

            // 分批处理嵌入，避免API限制 - 遵守阿里云API限制
            int batchSize = 10; // 阿里云API最大批次大小为10
            for (int i = 0; i < allSegments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, allSegments.size());
                List<dev.langchain4j.data.segment.TextSegment> batch = allSegments.subList(i, endIndex);

                log.debug("处理嵌入批次 {}/{}, 段数: {}",
                        (i / batchSize) + 1,
                        (allSegments.size() + batchSize - 1) / batchSize,
                        batch.size());

                // 为这批文本段生成嵌入
                List<dev.langchain4j.data.embedding.Embedding> embeddings = embeddingModel.embedAll(batch).content();

                // 存储到向量数据库
                for (int j = 0; j < batch.size(); j++) {
                    store.add(embeddings.get(j), batch.get(j));
                }

                // 减少延迟时间，提升响应速度
                if (i + batchSize < allSegments.size()) {
                    try {
                        Thread.sleep(50); // 减少到50ms延迟
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.error("处理多文件文档嵌入时出错: {}", e.getMessage(), e);
            throw new Exception("处理多文件文档嵌入失败: " + e.getMessage(), e);
        }

        // 创建检索器 - 平衡性能和质量
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .minScore(0.4) // 适中的分数，平衡质量和覆盖度
                .maxResults(10) // 减少检索结果，提升速度
                .embeddingModel(embeddingModel)
                .build();

        // 存储到缓存
        retrieverCache.put(cacheKey, retriever);
        log.info("为多个文件创建检索器成功，文件列表: {}，已缓存", String.join(", ", fileNames));
        return retriever;
    }

    /**
     * 清理检索器缓存
     */
    public void clearRetrieverCache() {
        retrieverCache.clear();
        log.info("检索器缓存已清理");
    }

    public void clearAllCaches() {
        retrieverCache.clear();
        // 强制垃圾回收，清理内存中的向量存储
        System.gc();
        log.info("所有缓存已清理，包括内存中的向量存储");
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return retrieverCache.size();
    }

    @Override
    public List<Document> loadDocumentsFromMultipleFiles(List<String> fileNames) throws Exception {
        if (fileNames == null || fileNames.isEmpty()) {
            throw new IllegalArgumentException("文件名列表不能为空");
        }

        List<Document> allDocuments = new ArrayList<>();

        for (String fileName : fileNames) {
            try {
                log.info("加载文件: {}", fileName);
                List<Document> documents = loadDocumentFromFile(fileName);

                // 为每个文档添加文件来源信息
                for (Document doc : documents) {
                    // 创建新的文档，添加文件来源元数据
                    Document documentWithSource = Document.from(doc.text(), doc.metadata().copy().add("source_file", fileName));
                    allDocuments.add(documentWithSource);
                }

                log.info("从文件 {} 加载了 {} 个文档", fileName, documents.size());

            } catch (Exception e) {
                log.error("加载文件 {} 失败: {}", fileName, e.getMessage(), e);
                throw new Exception("加载文件 " + fileName + " 失败: " + e.getMessage(), e);
            }
        }

        log.info("多文件加载完成，总文档数: {}", allDocuments.size());
        return allDocuments;
    }

    /**
     * 创建临时文件
     */
    private Path createTempFile(String fileName, byte[] content) throws IOException {
        String extName = FileUtil.extName(fileName);
        // 使用更唯一的临时文件名，包含原文件名和随机数
        String tempFileName = "temp_" + fileName.replace(".", "_") + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        if (extName != null) {
            tempFileName += "." + extName;
        }

        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), tempFileName);
        Files.write(tempFile, content);

        // 调试：验证临时文件内容
        String contentPreview = new String(content, StandardCharsets.UTF_8);
        String preview = contentPreview.length() > 100 ? contentPreview.substring(0, 100) + "..." : contentPreview;
        log.info("创建临时文件: {} -> {}, 内容预览: [{}]", fileName, tempFile.getFileName(), preview);

        return tempFile;
    }
}

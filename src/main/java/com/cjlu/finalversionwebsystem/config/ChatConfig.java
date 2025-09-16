package com.cjlu.finalversionwebsystem.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import com.cjlu.finalversionwebsystem.service.Interface.ChatServiceInterface;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

@Slf4j
@Configuration
public class ChatConfig {

    @Autowired
    private OpenAiChatModel model;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private OpenAiStreamingChatModel streamingChatModel;

    @Bean
    public ChatServiceInterface chatServiceInterface(){
        try {
            // 检查流式模型是否为null
            if (streamingChatModel == null) {
                log.error("流式模型为null，尝试使用普通模型");
                if (model == null) {
                    log.error("普通模型也为null，无法创建ChatServiceInterface");
                    throw new RuntimeException("模型未正确注入");
                }
                log.info("使用普通模型创建ChatServiceInterface");
                ChatServiceInterface chatService = AiServices.create(ChatServiceInterface.class, model);
                log.info("ChatServiceInterface创建成功（普通模型）");
                return chatService;
            }

            log.info("尝试创建ChatServiceInterface，使用流式模型");
            ChatServiceInterface chatService = AiServices.create(ChatServiceInterface.class, streamingChatModel);
            log.info("ChatServiceInterface创建成功（流式模型）");
            return chatService;
        } catch (Exception e) {
            log.error("创建ChatServiceInterface失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法创建ChatServiceInterface", e);
        }
    }

    //构建会话记忆
    @Bean
    public ChatMemory chatMemory(){
        MessageWindowChatMemory memory= MessageWindowChatMemory.builder()
                .maxMessages(5)  //最大保留消息数
                .build();
        return memory;
    }

    //构建ChatMemoryProvider对象
    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        ChatMemoryProvider chatMemoryProvider=new ChatMemoryProvider(){
            @Override
            public ChatMemory get(Object memoryId){
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(5)
                        .build();
            }
        };
        return chatMemoryProvider;
    }

    // 注释掉全局的EmbeddingStore和ContentRetriever Bean
    // 这些会导致不同文件之间的内容混合
    // 现在每个文件都会创建独立的向量存储和检索器

    /*
    //存储（构建向量数据库操作对象）
    @Bean
    public EmbeddingStore store(){//embeddingStore的对象，这个对象的名字不能重复，所以这里使用store
        //创建一个空的向量数据库，用于默认的聊天功能
        //如果需要默认加载某些文档，可以在这里添加
        InMemoryEmbeddingStore store = new InMemoryEmbeddingStore<>();

        // 可选：加载默认文档
        // 如果files目录下有默认文档，可以在这里加载
        try {
            String defaultDocPath = System.getProperty("user.dir") + File.separator + "files" + File.separator + "default";
            File defaultDir = new File(defaultDocPath);
            if (defaultDir.exists() && defaultDir.isDirectory()) {
                List<Document> documents = FileSystemDocumentLoader.loadDocuments(defaultDocPath, new ApachePdfBoxDocumentParser());
                if (!documents.isEmpty()) {
                    // 使用更大的分割大小以减少文本段数量，避免超过OpenAI API批处理限制
                    DocumentSplitter ds = DocumentSplitters.recursive(1000, 200);
                    EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                            .embeddingStore(store)
                            .documentSplitter(ds)
                            .embeddingModel(embeddingModel)
                            .build();
                    ingestor.ingest(documents);
                    log.info("加载了 {} 个默认文档", documents.size());
                }
            }
        } catch (Exception e) {
            log.warn("加载默认文档失败，将使用空的向量数据库: {}", e.getMessage());
        }

        return store;
    }

    //检索（构建向量数据库检索对象）
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore store){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .minScore(0.5)
                .maxResults(3)
                .embeddingModel(embeddingModel)
                .build();
    }
    */
}

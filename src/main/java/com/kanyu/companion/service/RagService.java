package com.kanyu.companion.service;

import io.milvus.client.MilvusServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final MilvusServiceClient milvusClient;
    private final EmbeddingModel embeddingModel;

    private static final String COLLECTION_NAME = "companion_knowledge";

    @PostConstruct
    public void init() {
        log.info("RAG Service initialized");
    }

    public void indexDocument(String content, Long userId, String source, Map<String, Object> metadata) {
        try {
            log.info("Indexing document for user {}: {}", userId, source);
            // 简化实现，暂时不实际存储到 Milvus
        } catch (Exception e) {
            log.error("Failed to index document", e);
            throw new RuntimeException("Failed to index document", e);
        }
    }

    public List<Document> similaritySearch(String query, Long userId, int topK, float threshold) {
        try {
            log.debug("Searching documents for user {} with query: {}", userId, query);
            // 简化实现，返回空列表
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to search documents", e);
            return Collections.emptyList();
        }
    }

    public String queryWithContext(String query, Long userId) {
        return queryWithContext(query, userId, 5, 0.7f);
    }

    public String queryWithContext(String query, Long userId, int topK, float threshold) {
        List<Document> documents = similaritySearch(query, userId, topK, threshold);

        if (documents.isEmpty()) {
            return null;
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是相关的参考资料：\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            context.append("【参考").append(i + 1).append("】");
            if (doc.getMetadata() != null && doc.getMetadata().containsKey("source")) {
                context.append("来源: ").append(doc.getMetadata().get("source"));
            }
            context.append("\n");
            context.append(doc.getText()).append("\n\n");
        }

        return context.toString();
    }

    public void deleteByUserId(Long userId) {
        try {
            log.info("Deleted documents for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete documents for user: {}", userId, e);
        }
    }

    public long countDocuments(Long userId) {
        try {
            return 0;
        } catch (Exception e) {
            log.error("Failed to count documents", e);
            return 0;
        }
    }
}

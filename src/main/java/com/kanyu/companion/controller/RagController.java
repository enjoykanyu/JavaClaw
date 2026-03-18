package com.kanyu.companion.controller;

import com.kanyu.companion.service.RagService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {
    
    private final RagService ragService;
    
    @PostMapping("/index")
    public ResponseEntity<IndexResponse> indexDocument(@RequestBody IndexRequest request) {
        log.info("Indexing document for user: {}", request.getUserId());
        
        try {
            ragService.indexDocument(
                request.getContent(),
                request.getUserId(),
                request.getSource(),
                request.getMetadata() != null ? request.getMetadata() : new HashMap<>()
            );
            
            IndexResponse response = new IndexResponse();
            response.setSuccess(true);
            response.setMessage("Document indexed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to index document", e);
            IndexResponse response = new IndexResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        log.info("Searching documents for user: {}", request.getUserId());
        
        try {
            List<Document> documents = ragService.similaritySearch(
                request.getQuery(),
                request.getUserId(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getThreshold() != null ? request.getThreshold() : 0.7f
            );
            
            SearchResponse response = new SearchResponse();
            response.setSuccess(true);
            response.setDocuments(documents.stream()
                .map(this::toDocumentInfo)
                .toList());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to search documents", e);
            SearchResponse response = new SearchResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> queryWithContext(@RequestBody QueryRequest request) {
        log.info("Query with context for user: {}", request.getUserId());
        
        try {
            String context = ragService.queryWithContext(
                request.getQuery(),
                request.getUserId(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getThreshold() != null ? request.getThreshold() : 0.7f
            );
            
            QueryResponse response = new QueryResponse();
            response.setSuccess(true);
            response.setContext(context);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to query with context", e);
            QueryResponse response = new QueryResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/count/{userId}")
    public ResponseEntity<Long> countDocuments(@PathVariable Long userId) {
        long count = ragService.countDocuments(userId);
        return ResponseEntity.ok(count);
    }
    
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<String> deleteUserDocuments(@PathVariable Long userId) {
        ragService.deleteByUserId(userId);
        return ResponseEntity.ok("Documents deleted for user: " + userId);
    }
    
    private DocumentInfo toDocumentInfo(Document doc) {
        DocumentInfo info = new DocumentInfo();
        info.setContent(doc.getText());
        info.setMetadata(doc.getMetadata());
        return info;
    }
    
    @Data
    public static class IndexRequest {
        private Long userId;
        private String content;
        private String source;
        private Map<String, Object> metadata;
    }
    
    @Data
    public static class IndexResponse {
        private boolean success;
        private String message;
        private String error;
    }
    
    @Data
    public static class SearchRequest {
        private Long userId;
        private String query;
        private Integer topK;
        private Float threshold;
    }
    
    @Data
    public static class SearchResponse {
        private boolean success;
        private List<DocumentInfo> documents;
        private String error;
    }
    
    @Data
    public static class QueryRequest {
        private Long userId;
        private String query;
        private Integer topK;
        private Float threshold;
    }
    
    @Data
    public static class QueryResponse {
        private boolean success;
        private String context;
        private String error;
    }
    
    @Data
    public static class DocumentInfo {
        private String content;
        private Map<String, Object> metadata;
    }
}

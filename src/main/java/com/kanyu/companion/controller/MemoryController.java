package com.kanyu.companion.controller;

import com.kanyu.companion.model.Memory;
import com.kanyu.companion.service.MemoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {
    
    private final MemoryService memoryService;
    
    @PostMapping("/store")
    public ResponseEntity<MemoryResponse> storeMemory(@RequestBody MemoryRequest request) {
        log.info("Storing memory for user: {}", request.getUserId());
        
        try {
            Memory memory = memoryService.storeMemory(
                request.getUserId(),
                request.getContent(),
                Memory.MemoryType.valueOf(request.getType()),
                request.getImportance(),
                request.getMetadata()
            );
            
            return ResponseEntity.ok(toMemoryResponse(memory));
            
        } catch (Exception e) {
            log.error("Failed to store memory", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MemoryResponse>> getUserMemories(@PathVariable Long userId) {
        List<Memory> memories = memoryService.getRecentMemories(userId, 20);
        List<MemoryResponse> responses = memories.stream()
            .map(this::toMemoryResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/context/{userId}")
    public ResponseEntity<String> getMemoryContext(@PathVariable Long userId) {
        String context = memoryService.buildMemoryContext(userId);
        return ResponseEntity.ok(context);
    }
    
    @PostMapping("/search")
    public ResponseEntity<List<MemoryResponse>> searchMemories(@RequestBody SearchRequest request) {
        List<Memory> memories = memoryService.retrieveRelevantMemories(
            request.getUserId(),
            request.getQuery(),
            request.getLimit() != null ? request.getLimit() : 10
        );
        
        List<MemoryResponse> responses = memories.stream()
            .map(this::toMemoryResponse)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/consolidate/{userId}")
    public ResponseEntity<String> consolidateMemories(@PathVariable Long userId) {
        memoryService.consolidateMemories(userId);
        return ResponseEntity.ok("Memory consolidation completed");
    }
    
    @PostMapping("/important/{memoryId}")
    public ResponseEntity<String> markAsImportant(@PathVariable Long memoryId) {
        memoryService.markAsImportant(memoryId);
        return ResponseEntity.ok("Memory marked as important");
    }
    
    @DeleteMapping("/old/{userId}")
    public ResponseEntity<String> forgetOldMemories(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days) {
        memoryService.forgetOldMemories(userId, days);
        return ResponseEntity.ok("Old memories removed");
    }
    
    private MemoryResponse toMemoryResponse(Memory memory) {
        MemoryResponse response = new MemoryResponse();
        response.setId(memory.getId());
        response.setUserId(memory.getUserId());
        response.setContent(memory.getContent());
        response.setType(memory.getType().name());
        response.setImportance(memory.getImportance());
        response.setCreatedAt(memory.getCreatedAt() != null ? memory.getCreatedAt().toString() : null);
        response.setMetadata(memory.getMetadata());
        return response;
    }
    
    @Data
    public static class MemoryRequest {
        private Long userId;
        private String content;
        private String type;
        private Float importance;
        private Map<String, Object> metadata;
    }
    
    @Data
    public static class MemoryResponse {
        private Long id;
        private Long userId;
        private String content;
        private String type;
        private Float importance;
        private String createdAt;
        private Map<String, Object> metadata;
    }
    
    @Data
    public static class SearchRequest {
        private Long userId;
        private String query;
        private Integer limit;
    }
}

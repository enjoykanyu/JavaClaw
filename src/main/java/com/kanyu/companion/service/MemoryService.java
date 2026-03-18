package com.kanyu.companion.service;

import com.kanyu.companion.config.MemoryConfig;
import com.kanyu.companion.model.Memory;
import com.kanyu.companion.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {
    
    private final MemoryRepository memoryRepository;
    private final MemoryConfig memoryConfig;
    
    @Transactional
    public Memory storeMemory(Long userId, String content, Memory.MemoryType type) {
        return storeMemory(userId, content, type, 0.5f, null);
    }
    
    @Transactional
    public Memory storeMemory(Long userId, String content, Memory.MemoryType type, 
                               Float importance, Map<String, Object> metadata) {
        log.debug("Storing memory for user {}: type={}, importance={}", userId, type, importance);
        
        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setContent(content);
        memory.setType(type);
        memory.setImportance(importance != null ? importance : calculateImportance(content));
        if (metadata != null) {
            memory.setMetadata(metadata);
        }
        
        return memoryRepository.save(memory);
    }
    
    public List<Memory> retrieveRelevantMemories(Long userId, String query, int topK) {
        log.debug("Retrieving memories for user {}: query={}", userId, query);
        
        List<Memory> longTermMemories = memoryRepository.findByUserIdAndType(
            userId, Memory.MemoryType.LONG_TERM
        );
        
        List<Memory> importantMemories = memoryRepository.findImportantMemories(
            userId, memoryConfig.getImportanceThreshold()
        );
        
        List<Memory> recentMemories = memoryRepository.findRecentMemories(
            userId, LocalDateTime.now().minusDays(7)
        );
        
        return java.util.stream.Stream.concat(
                longTermMemories.stream(),
                java.util.stream.Stream.concat(
                    importantMemories.stream(),
                    recentMemories.stream()
                )
            )
            .distinct()
            .limit(topK)
            .collect(Collectors.toList());
    }
    
    public List<Memory> getRecentMemories(Long userId, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        List<Memory> memories = memoryRepository.findRecentMemories(userId, since);
        return memories.stream().limit(limit).collect(Collectors.toList());
    }
    
    public List<Memory> getConversationContext(Long userId) {
        return memoryRepository.findByUserIdAndTypes(
            userId,
            List.of(Memory.MemoryType.SHORT_TERM, Memory.MemoryType.EPISODIC)
        ).stream()
         .limit(memoryConfig.getShortTermLimit())
         .collect(Collectors.toList());
    }
    
    @Transactional
    public void consolidateMemories(Long userId) {
        log.info("Consolidating memories for user: {}", userId);
        
        List<Memory> shortTermMemories = memoryRepository.findByUserIdAndType(
            userId, Memory.MemoryType.SHORT_TERM
        );
        
        for (Memory memory : shortTermMemories) {
            if (memory.getImportance() >= memoryConfig.getLongTermThreshold()) {
                memory.setType(Memory.MemoryType.LONG_TERM);
                memoryRepository.save(memory);
                log.debug("Promoted memory to long-term: {}", memory.getId());
            }
        }
    }
    
    @Transactional
    @Scheduled(fixedRateString = "${companion.memory.consolidation-interval:3600000}")
    public void consolidateAllMemories() {
        log.info("Running scheduled memory consolidation");
        memoryRepository.findAll().stream()
            .map(Memory::getUserId)
            .distinct()
            .forEach(this::consolidateMemories);
    }
    
    @Transactional
    public void forgetOldMemories(Long userId, int daysToKeep) {
        log.info("Forgetting old memories for user: {}, keeping {} days", userId, daysToKeep);
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        memoryRepository.deleteByUserIdAndCreatedAtBefore(userId, cutoff);
    }
    
    @Transactional
    public void markAsImportant(Long memoryId) {
        memoryRepository.findById(memoryId).ifPresent(memory -> {
            memory.setType(Memory.MemoryType.IMPORTANT_EVENT);
            memory.setImportance(1.0f);
            memoryRepository.save(memory);
        });
    }
    
    @Transactional
    public void updateAccessInfo(Long memoryId) {
        memoryRepository.findById(memoryId).ifPresent(memory -> {
            memory.setLastAccessedAt(LocalDateTime.now());
            memory.setAccessCount(memory.getAccessCount() + 1);
            memoryRepository.save(memory);
        });
    }
    
    public String buildMemoryContext(Long userId) {
        List<Memory> memories = retrieveRelevantMemories(userId, "", 10);
        
        if (memories.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder("以下是关于用户的一些记忆：\n");
        for (Memory memory : memories) {
            context.append("- ").append(memory.getContent()).append("\n");
        }
        
        return context.toString();
    }
    
    private float calculateImportance(String content) {
        float importance = 0.5f;
        
        String[] importantKeywords = {"重要", "生日", "纪念日", "喜欢", "讨厌", "过敏", "工作", "家庭"};
        for (String keyword : importantKeywords) {
            if (content.contains(keyword)) {
                importance += 0.1f;
            }
        }
        
        return Math.min(importance, 1.0f);
    }
}

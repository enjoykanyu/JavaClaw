package com.kanyu.companion.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "memories", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
public class Memory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private MemoryType type = MemoryType.SHORT_TERM;
    
    @Column(name = "importance")
    private Float importance = 0.5f;
    
    @Column(name = "embedding_id", length = 100)
    private String embeddingId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "access_count")
    private Integer accessCount = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        if (accessCount == null) {
            accessCount = 0;
        }
    }
    
    public enum MemoryType {
        SHORT_TERM,
        LONG_TERM,
        EPISODIC,
        PREFERENCE,
        IMPORTANT_EVENT
    }
}

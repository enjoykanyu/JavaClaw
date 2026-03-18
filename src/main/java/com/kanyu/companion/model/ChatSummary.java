package com.kanyu.companion.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "chat_summaries")
@Data
public class ChatSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "chat_source", length = 100)
    private String chatSource;
    
    @Column(name = "chat_type", length = 20)
    private String chatType;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "todo_items", columnDefinition = "JSON")
    private List<TodoItem> todoItems = new ArrayList<>();
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completed_items", columnDefinition = "JSON")
    private List<CompletedItem> completedItems = new ArrayList<>();
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_points", columnDefinition = "JSON")
    private List<String> keyPoints = new ArrayList<>();
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @Data
    public static class TodoItem {
        private String content;
        private String priority;
        private LocalDateTime deadline;
        private String assignee;
        private String status;
    }
    
    @Data
    public static class CompletedItem {
        private String content;
        private LocalDateTime completedAt;
        private String completedBy;
    }
}

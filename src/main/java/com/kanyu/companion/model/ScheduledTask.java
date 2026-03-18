package com.kanyu.companion.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "scheduled_tasks")
@Data
public class ScheduledTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "task_name", length = 100, nullable = false)
    private String taskName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private TaskType type;
    
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "task_config", columnDefinition = "JSON")
    private Map<String, Object> taskConfig = new HashMap<>();
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @Column(name = "last_run_time")
    private LocalDateTime lastRunTime;
    
    @Column(name = "next_run_time")
    private LocalDateTime nextRunTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum TaskType {
        NEWS_PUSH,
        REMINDER,
        SUMMARY,
        GREETING,
        CUSTOM
    }
}

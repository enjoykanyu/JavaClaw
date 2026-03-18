package com.kanyu.companion.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "companion_profiles")
@Data
public class CompanionProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "name", length = 50)
    private String name;
    
    @Column(name = "personality", columnDefinition = "TEXT")
    private String personality;
    
    @Column(name = "speaking_style", length = 100)
    private String speakingStyle;
    
    @Column(name = "relationship", length = 50)
    private String relationship;
    
    @Column(name = "custom_rules", columnDefinition = "TEXT")
    private String customRules;
    
    @Column(name = "avatar", length = 500)
    private String avatar;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "JSON")
    private Map<String, Object> preferences = new HashMap<>();
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personality_traits", columnDefinition = "JSON")
    private PersonalityTraits personalityTraits;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (personalityTraits == null) {
            personalityTraits = new PersonalityTraits();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

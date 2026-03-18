package com.kanyu.companion.repository;

import com.kanyu.companion.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<ChatMessage> findByUserIdAndSessionIdOrderByCreatedAtAsc(Long userId, String sessionId);
    
    @Query("SELECT m FROM ChatMessage m WHERE m.userId = :userId AND m.createdAt >= :since ORDER BY m.createdAt ASC")
    List<ChatMessage> findRecentMessages(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT m FROM ChatMessage m WHERE m.userId = :userId ORDER BY m.createdAt DESC LIMIT :limit")
    List<ChatMessage> findTopNByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}

package com.kanyu.companion.repository;

import com.kanyu.companion.model.ChatSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatSummaryRepository extends JpaRepository<ChatSummary, Long> {
    
    List<ChatSummary> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT s FROM ChatSummary s WHERE s.userId = :userId AND s.startTime >= :start AND s.endTime <= :end")
    List<ChatSummary> findByUserIdAndTimeRange(
        @Param("userId") Long userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT s FROM ChatSummary s WHERE s.userId = :userId ORDER BY s.createdAt DESC LIMIT 1")
    ChatSummary findLatestByUserId(@Param("userId") Long userId);
}

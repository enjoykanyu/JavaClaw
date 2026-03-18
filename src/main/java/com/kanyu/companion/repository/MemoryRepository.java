package com.kanyu.companion.repository;

import com.kanyu.companion.model.Memory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, Long> {
    
    List<Memory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Memory> findByUserIdAndType(Long userId, Memory.MemoryType type);
    
    @Query("SELECT m FROM Memory m WHERE m.userId = :userId AND m.importance >= :minImportance ORDER BY m.importance DESC, m.createdAt DESC")
    List<Memory> findImportantMemories(@Param("userId") Long userId, @Param("minImportance") Float minImportance);
    
    @Query("SELECT m FROM Memory m WHERE m.userId = :userId AND m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<Memory> findRecentMemories(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT m FROM Memory m WHERE m.userId = :userId AND m.type IN :types ORDER BY m.lastAccessedAt DESC")
    List<Memory> findByUserIdAndTypes(@Param("userId") Long userId, @Param("types") List<Memory.MemoryType> types);
    
    void deleteByUserIdAndCreatedAtBefore(Long userId, LocalDateTime before);
    
    @Query("SELECT COUNT(m) FROM Memory m WHERE m.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}

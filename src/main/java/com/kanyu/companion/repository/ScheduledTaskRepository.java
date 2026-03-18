package com.kanyu.companion.repository;

import com.kanyu.companion.model.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {
    
    List<ScheduledTask> findByUserId(Long userId);
    
    List<ScheduledTask> findByEnabledTrue();
    
    List<ScheduledTask> findByUserIdAndEnabledTrue(Long userId);
    
    @Query("SELECT t FROM ScheduledTask t WHERE t.enabled = true AND t.nextRunTime <= :time")
    List<ScheduledTask> findDueTasks(@Param("time") LocalDateTime time);
    
    @Query("SELECT t FROM ScheduledTask t WHERE t.userId = :userId AND t.type = :type")
    List<ScheduledTask> findByUserIdAndType(@Param("userId") Long userId, @Param("type") ScheduledTask.TaskType type);
}

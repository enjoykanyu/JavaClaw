package com.kanyu.companion.service;

import com.kanyu.companion.model.ScheduledTask;
import com.kanyu.companion.repository.ScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final ScheduledTaskRepository taskRepository;
    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    @Transactional
    public ScheduledTask createTask(Long userId, String taskName, ScheduledTask.TaskType type,
                                     String cronExpression, Map<String, Object> config) {
        log.info("Creating task: {} for user: {}", taskName, userId);
        
        ScheduledTask task = new ScheduledTask();
        task.setUserId(userId);
        task.setTaskName(taskName);
        task.setType(type);
        task.setCronExpression(cronExpression);
        task.setTaskConfig(config != null ? config : new HashMap<>());
        task.setEnabled(true);
        
        CronExpression cron = CronExpression.parse(cronExpression);
        LocalDateTime nextRun = cron.next(LocalDateTime.now());
        task.setNextRunTime(nextRun);
        
        task = taskRepository.save(task);
        
        scheduleTask(task);
        
        return task;
    }
    
    @Transactional
    public void enableTask(Long taskId) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setEnabled(true);
            taskRepository.save(task);
            scheduleTask(task);
        });
    }
    
    @Transactional
    public void disableTask(Long taskId) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setEnabled(false);
            taskRepository.save(task);
            cancelScheduledTask(taskId);
        });
    }
    
    @Transactional
    public void deleteTask(Long taskId) {
        cancelScheduledTask(taskId);
        taskRepository.deleteById(taskId);
    }
    
    public List<ScheduledTask> getUserTasks(Long userId) {
        return taskRepository.findByUserId(userId);
    }
    
    public List<ScheduledTask> getEnabledTasks(Long userId) {
        return taskRepository.findByUserIdAndEnabledTrue(userId);
    }
    
    @Scheduled(fixedRate = 60000)
    public void checkDueTasks() {
        List<ScheduledTask> dueTasks = taskRepository.findDueTasks(LocalDateTime.now());
        
        for (ScheduledTask task : dueTasks) {
            try {
                executeTask(task);
                updateTaskRunTime(task);
            } catch (Exception e) {
                log.error("Failed to execute task: {}", task.getId(), e);
            }
        }
    }
    
    private void executeTask(ScheduledTask task) {
        log.info("Executing task: {} for user: {}", task.getTaskName(), task.getUserId());
        
        switch (task.getType()) {
            case NEWS_PUSH -> executeNewsPush(task);
            case REMINDER -> executeReminder(task);
            case SUMMARY -> executeSummary(task);
            case GREETING -> executeGreeting(task);
            case CUSTOM -> executeCustomTask(task);
        }
    }
    
    private void executeNewsPush(ScheduledTask task) {
        log.info("Executing news push for user: {}", task.getUserId());
    }
    
    private void executeReminder(ScheduledTask task) {
        log.info("Executing reminder for user: {}", task.getUserId());
    }
    
    private void executeSummary(ScheduledTask task) {
        log.info("Executing summary for user: {}", task.getUserId());
    }
    
    private void executeGreeting(ScheduledTask task) {
        log.info("Executing greeting for user: {}", task.getUserId());
    }
    
    private void executeCustomTask(ScheduledTask task) {
        log.info("Executing custom task for user: {}", task.getUserId());
    }
    
    @Transactional
    public void updateTaskRunTime(ScheduledTask task) {
        CronExpression cron = CronExpression.parse(task.getCronExpression());
        LocalDateTime nextRun = cron.next(LocalDateTime.now());
        
        task.setLastRunTime(LocalDateTime.now());
        task.setNextRunTime(nextRun);
        taskRepository.save(task);
    }
    
    private void scheduleTask(ScheduledTask task) {
        if (!task.getEnabled()) {
            return;
        }
        
        cancelScheduledTask(task.getId());
        
        Date nextRunTime = Date.from(task.getNextRunTime().atZone(ZoneId.systemDefault()).toInstant());
        
        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> executeTask(task),
            nextRunTime.toInstant()
        );
        
        scheduledTasks.put(task.getId(), future);
        log.info("Scheduled task: {} to run at: {}", task.getId(), nextRunTime);
    }
    
    private void cancelScheduledTask(Long taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled scheduled task: {}", taskId);
        }
    }
}

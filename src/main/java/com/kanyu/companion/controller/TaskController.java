package com.kanyu.companion.controller;

import com.kanyu.companion.model.ScheduledTask;
import com.kanyu.companion.service.TaskService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody TaskRequest request) {
        log.info("Creating task for user: {}", request.getUserId());
        
        try {
            ScheduledTask task = taskService.createTask(
                request.getUserId(),
                request.getTaskName(),
                ScheduledTask.TaskType.valueOf(request.getType()),
                request.getCronExpression(),
                request.getConfig()
            );
            
            return ResponseEntity.ok(toTaskResponse(task));
            
        } catch (Exception e) {
            log.error("Failed to create task", e);
            TaskResponse response = new TaskResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskResponse>> getUserTasks(@PathVariable Long userId) {
        List<ScheduledTask> tasks = taskService.getUserTasks(userId);
        List<TaskResponse> responses = tasks.stream()
            .map(this::toTaskResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/{taskId}/enable")
    public ResponseEntity<String> enableTask(@PathVariable Long taskId) {
        taskService.enableTask(taskId);
        return ResponseEntity.ok("Task enabled");
    }
    
    @PostMapping("/{taskId}/disable")
    public ResponseEntity<String> disableTask(@PathVariable Long taskId) {
        taskService.disableTask(taskId);
        return ResponseEntity.ok("Task disabled");
    }
    
    @DeleteMapping("/{taskId}")
    public ResponseEntity<String> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok("Task deleted");
    }
    
    private TaskResponse toTaskResponse(ScheduledTask task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setUserId(task.getUserId());
        response.setTaskName(task.getTaskName());
        response.setType(task.getType().name());
        response.setCronExpression(task.getCronExpression());
        response.setEnabled(task.getEnabled());
        response.setLastRunTime(task.getLastRunTime() != null ? task.getLastRunTime().toString() : null);
        response.setNextRunTime(task.getNextRunTime() != null ? task.getNextRunTime().toString() : null);
        response.setSuccess(true);
        return response;
    }
    
    @Data
    public static class TaskRequest {
        private Long userId;
        private String taskName;
        private String type;
        private String cronExpression;
        private Map<String, Object> config;
    }
    
    @Data
    public static class TaskResponse {
        private boolean success;
        private Long id;
        private Long userId;
        private String taskName;
        private String type;
        private String cronExpression;
        private boolean enabled;
        private String lastRunTime;
        private String nextRunTime;
        private String error;
    }
}

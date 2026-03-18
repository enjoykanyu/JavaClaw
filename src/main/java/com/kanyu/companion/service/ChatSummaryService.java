package com.kanyu.companion.service;

import com.kanyu.companion.model.ChatMessage;
import com.kanyu.companion.model.ChatSummary;
import com.kanyu.companion.repository.ChatMessageRepository;
import com.kanyu.companion.repository.ChatSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSummaryService {
    
    private final ChatModel chatModel;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSummaryRepository chatSummaryRepository;
    
    private static final String SUMMARY_PROMPT = """
        请分析以下聊天记录，提取任务清单和完成事项。
        
        聊天记录：
        %s
        
        请返回以下JSON格式：
        {
            "summary": "对话摘要",
            "todo_items": [
                {
                    "content": "任务内容",
                    "priority": "high/medium/low",
                    "status": "pending/in_progress/completed"
                }
            ],
            "completed_items": [
                {
                    "content": "已完成事项",
                    "completed_at": "完成时间"
                }
            ],
            "key_points": ["关键点1", "关键点2"]
        }
        """;
    
    @Transactional
    public ChatSummary summarizeChat(Long userId, String chatSource, 
                                     LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Summarizing chat for user: {}", userId);
        
        List<ChatMessage> messages = chatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(
            userId, chatSource
        );
        
        if (messages.isEmpty()) {
            return null;
        }
        
        StringBuilder chatContent = new StringBuilder();
        for (ChatMessage msg : messages) {
            chatContent.append(msg.getRole()).append(": ")
                      .append(msg.getContent()).append("\n");
        }
        
        Map<String, Object> summaryResult = generateSummary(chatContent.toString());
        
        ChatSummary summary = new ChatSummary();
        summary.setUserId(userId);
        summary.setChatSource(chatSource);
        summary.setStartTime(startTime != null ? startTime : messages.get(0).getCreatedAt());
        summary.setEndTime(endTime != null ? endTime : messages.get(messages.size() - 1).getCreatedAt());
        summary.setSummary((String) summaryResult.get("summary"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> todoItems = (List<Map<String, Object>>) summaryResult.get("todo_items");
        if (todoItems != null) {
            List<ChatSummary.TodoItem> todos = new ArrayList<>();
            for (Map<String, Object> item : todoItems) {
                ChatSummary.TodoItem todo = new ChatSummary.TodoItem();
                todo.setContent((String) item.get("content"));
                todo.setPriority((String) item.get("priority"));
                todo.setStatus((String) item.get("status"));
                todos.add(todo);
            }
            summary.setTodoItems(todos);
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> completedItems = (List<Map<String, Object>>) summaryResult.get("completed_items");
        if (completedItems != null) {
            List<ChatSummary.CompletedItem> completed = new ArrayList<>();
            for (Map<String, Object> item : completedItems) {
                ChatSummary.CompletedItem ci = new ChatSummary.CompletedItem();
                ci.setContent((String) item.get("content"));
                completed.add(ci);
            }
            summary.setCompletedItems(completed);
        }
        
        @SuppressWarnings("unchecked")
        List<String> keyPoints = (List<String>) summaryResult.get("key_points");
        summary.setKeyPoints(keyPoints != null ? keyPoints : new ArrayList<>());
        
        return chatSummaryRepository.save(summary);
    }
    
    public Map<String, Object> generateSummary(String chatContent) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的对话分析助手，擅长从对话中提取任务和关键信息。"));
            messages.add(new UserMessage(String.format(SUMMARY_PROMPT, chatContent)));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            if (response != null && response.getResult() != null) {
                String responseText = response.getResult().getOutput().getText();
                return parseSummaryResult(responseText);
            }
            
        } catch (Exception e) {
            log.error("Failed to generate summary", e);
        }
        
        return Map.of("summary", "无法生成摘要", "todo_items", List.of(), "key_points", List.of());
    }
    
    private Map<String, Object> parseSummaryResult(String responseText) {
        try {
            String json = responseText;
            if (responseText.contains("{")) {
                json = responseText.substring(responseText.indexOf("{"), responseText.lastIndexOf("}") + 1);
            }
            
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, Map.class);
            
        } catch (Exception e) {
            log.warn("Failed to parse summary result", e);
            return Map.of("summary", responseText, "todo_items", List.of(), "key_points", List.of());
        }
    }
    
    public List<ChatSummary> getUserSummaries(Long userId) {
        return chatSummaryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public ChatSummary getLatestSummary(Long userId) {
        return chatSummaryRepository.findLatestByUserId(userId);
    }
}

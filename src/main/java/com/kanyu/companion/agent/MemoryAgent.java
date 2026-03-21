package com.kanyu.companion.agent;

import com.kanyu.companion.model.Memory;
import com.kanyu.companion.service.MemoryService;
import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MemoryAgent {
    
    private final ChatModel chatModel;
    private final MemoryService memoryService;
    
    private static final String MEMORY_EXTRACTION_PROMPT = """
        分析以下对话内容，提取需要记住的重要信息。
        
        对话内容：%s
        
        请判断是否包含以下类型的信息：
        1. 用户的个人信息（姓名、生日、职业等）
        2. 用户的偏好和喜好
        3. 重要的事件或计划
        4. 用户的情感状态或需求
        
        如果有重要信息，返回JSON格式：
        {
            "should_remember": true/false,
            "memory_type": "PREFERENCE/IMPORTANT_EVENT/PERSONAL_INFO",
            "content": "要记住的内容摘要",
            "importance": 0.0-1.0,
            "keywords": ["关键词"]
        }
        
        如果没有重要信息，返回：
        {
            "should_remember": false
        }
        
        只返回JSON，不要其他内容。
        """;
    
    public MemoryAgent(ChatModel chatModel, MemoryService memoryService) {
        this.chatModel = chatModel;
        this.memoryService = memoryService;
    }
    
    public GraphState execute(GraphState state) {
        log.info("Executing MemoryAgent");
        
        try {
            Long userId = state.get("userId");
            String userInput = state.getUserInput();
            String companionResponse = state.get("companion_response");
            
            if (userInput != null && !userInput.isEmpty()) {
                String conversationText = "用户: " + userInput;
                if (companionResponse != null) {
                    conversationText += "\n助手: " + companionResponse;
                }
                
                Map<String, Object> extractionResult = extractMemoryInfo(conversationText);
                
                if (extractionResult != null && Boolean.TRUE.equals(extractionResult.get("should_remember"))) {
                    String memoryType = (String) extractionResult.get("memory_type");
                    String content = (String) extractionResult.get("content");
                    Float importance = ((Number) extractionResult.get("importance")).floatValue();
                    
                    Memory.MemoryType type = Memory.MemoryType.valueOf(memoryType);
                    
                    memoryService.storeMemory(userId, content, type, importance, extractionResult);
                    
                    state.put("new_memory_created", true);
                    log.debug("Created new memory: {} for user: {}", content, userId);
                }
            }
            
            List<Memory> relevantMemories = memoryService.retrieveRelevantMemories(userId, userInput, 5);
            state.put("retrieved_memories", relevantMemories);
            
        } catch (Exception e) {
            log.error("MemoryAgent execution failed", e);
        }
        
        return state;
    }
    
    private Map<String, Object> extractMemoryInfo(String conversationText) {
        try {
            String analysisPrompt = String.format(MEMORY_EXTRACTION_PROMPT, conversationText);
            
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个记忆提取助手，擅长从对话中识别重要信息。"));
            messages.add(new SystemMessage(analysisPrompt));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            if (response != null && response.getResult() != null) {
                String responseText = response.getResult().getOutput().getContent();
                return parseMemoryResult(responseText);
            }
            
        } catch (Exception e) {
            log.error("Failed to extract memory info", e);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("should_remember", false);
        return result;
    }
    
    private Map<String, Object> parseMemoryResult(String responseText) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String json = responseText;
            if (responseText.contains("{")) {
                json = responseText.substring(responseText.indexOf("{"), responseText.lastIndexOf("}") + 1);
            }
            
            json = json.trim();
            
            boolean shouldRemember = extractBooleanValue(json, "should_remember", false);
            result.put("should_remember", shouldRemember);
            
            if (shouldRemember) {
                result.put("memory_type", extractValue(json, "memory_type", "PREFERENCE"));
                result.put("content", extractValue(json, "content", ""));
                result.put("importance", extractFloatValue(json, "importance", 0.5f));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse memory result", e);
            result.put("should_remember", false);
        }
        
        return result;
    }
    
    private String extractValue(String json, String key, String defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.debug("Failed to extract value for key: {}", key);
        }
        return defaultValue;
    }
    
    private float extractFloatValue(String json, String key, float defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return Float.parseFloat(m.group(1));
            }
        } catch (Exception e) {
            log.debug("Failed to extract float value for key: {}", key);
        }
        return defaultValue;
    }
    
    private boolean extractBooleanValue(String json, String key, boolean defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(1));
            }
        } catch (Exception e) {
            log.debug("Failed to extract boolean value for key: {}", key);
        }
        return defaultValue;
    }
}

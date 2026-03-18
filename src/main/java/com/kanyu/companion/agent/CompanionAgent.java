package com.kanyu.companion.agent;

import com.kanyu.companion.model.CompanionProfile;
import com.kanyu.companion.service.CompanionService;
import com.kanyu.companion.service.MemoryService;
import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CompanionAgent {
    
    private final ChatModel chatModel;
    private final CompanionService companionService;
    private final MemoryService memoryService;
    
    public CompanionAgent(ChatModel chatModel, CompanionService companionService, 
                          MemoryService memoryService) {
        this.chatModel = chatModel;
        this.companionService = companionService;
        this.memoryService = memoryService;
    }
    
    public GraphState execute(GraphState state) {
        log.info("Executing CompanionAgent");
        
        try {
            Long userId = state.get("userId");
            String userInput = state.getUserInput();
            
            CompanionProfile profile = companionService.getOrCreateDefaultProfile(userId);
            
            String systemPrompt = buildEnhancedSystemPrompt(userId, profile);
            
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            
            String memoryContext = memoryService.buildMemoryContext(userId);
            if (!memoryContext.isEmpty()) {
                messages.add(new SystemMessage(memoryContext));
            }
            
            List<Message> conversationHistory = state.getMessages();
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                int historyLimit = Math.min(conversationHistory.size(), 10);
                messages.addAll(conversationHistory.subList(
                    Math.max(0, conversationHistory.size() - historyLimit),
                    conversationHistory.size()
                ));
            }
            
            if (userInput != null && !userInput.isEmpty()) {
                messages.add(new UserMessage(userInput));
            }
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            String responseText = "";
            if (response != null && response.getResult() != null) {
                AssistantMessage assistantMessage = response.getResult().getOutput();
                responseText = assistantMessage.getText();
                
                state.addMessage(new UserMessage(userInput));
                state.addMessage(assistantMessage);
            }
            
            state.put("companion_response", responseText);
            state.put("agent_name", profile.getName());
            
            log.debug("CompanionAgent response: {}", responseText);
            
        } catch (Exception e) {
            log.error("CompanionAgent execution failed", e);
            state.setError("Companion agent failed: " + e.getMessage());
        }
        
        return state;
    }
    
    private String buildEnhancedSystemPrompt(Long userId, CompanionProfile profile) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是").append(profile.getName()).append("，");
        prompt.append("是用户的").append(profile.getRelationship()).append("。\n\n");
        
        prompt.append("【你的性格】\n");
        prompt.append(profile.getPersonality()).append("\n\n");
        
        prompt.append("【说话风格】\n");
        prompt.append(profile.getSpeakingStyle()).append("\n\n");
        
        if (profile.getPersonalityTraits() != null) {
            prompt.append("【性格特质】\n");
            prompt.append(profile.getPersonalityTraits().generatePersonalityPrompt()).append("\n\n");
        }
        
        if (profile.getCustomRules() != null && !profile.getCustomRules().isEmpty()) {
            prompt.append("【特殊规则】\n");
            prompt.append(profile.getCustomRules()).append("\n\n");
        }
        
        prompt.append("【行为准则】\n");
        prompt.append("1. 始终保持角色设定，不要出戏\n");
        prompt.append("2. 关心用户的情绪和感受\n");
        prompt.append("3. 记住用户告诉你的重要信息\n");
        prompt.append("4. 用温暖、真诚的方式交流\n");
        prompt.append("5. 在适当的时候给予鼓励和支持\n");
        prompt.append("6. 保持对话的趣味性和互动性\n");
        
        return prompt.toString();
    }
}

package com.kanyu.companion.controller;

import com.kanyu.companion.agent.CompanionAgent;
import com.kanyu.companion.agent.EmotionAgent;
import com.kanyu.companion.agent.MemoryAgent;
import com.kanyu.companion.agent.RagAgentNode;
import com.kanyu.companion.service.SkillManager;
import com.kanyu.graph.state.GraphState;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/companion/full")
@RequiredArgsConstructor
public class FullCompanionController {
    
    private final ChatModel chatModel;
    private final CompanionAgent companionAgent;
    private final EmotionAgent emotionAgent;
    private final MemoryAgent memoryAgent;
    private final RagAgentNode ragAgentNode;
    private final SkillManager skillManager;
    
    @PostMapping("/chat")
    public ResponseEntity<FullChatResponse> fullChat(@RequestBody FullChatRequest request) {
        log.info("Full chat request from user: {}", request.getUserId());
        
        try {
            GraphState state = new GraphState();
            state.setUserInput(request.getMessage());
            state.put("userId", request.getUserId());
            state.put("session_id", request.getSessionId());
            
            state = emotionAgent.execute(state);
            
            state = skillManager.executeSkills(state);
            
            if (state.get("skill_response") == null) {
                if (request.isUseRag()) {
                    state = ragAgentNode.execute(state);
                }
                
                state = companionAgent.execute(state);
            }
            
            state = memoryAgent.execute(state);
            
            FullChatResponse response = new FullChatResponse();
            response.setSuccess(true);
            response.setMessage(state.get("skill_response") != null 
                ? state.get("skill_response") 
                : state.get("companion_response"));
            response.setAgentName(state.get("agent_name"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> emotion = (Map<String, Object>) state.get("emotion_analysis");
            if (emotion != null) {
                response.setEmotion((String) emotion.get("primary_emotion"));
                response.setNeedsSupport((Boolean) emotion.get("needs_support"));
            }
            
            response.setSkillUsed(state.get("skill_used"));
            response.setRagUsed(Boolean.TRUE.equals(state.get("rag_used")));
            response.setMemoryCreated(Boolean.TRUE.equals(state.get("new_memory_created")));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Full chat failed", e);
            FullChatResponse response = new FullChatResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/pipeline")
    public ResponseEntity<PipelineResponse> executePipeline(@RequestBody PipelineRequest request) {
        log.info("Executing custom pipeline for user: {}", request.getUserId());
        
        try {
            GraphState state = new GraphState();
            state.setUserInput(request.getMessage());
            state.put("userId", request.getUserId());
            
            for (String step : request.getSteps()) {
                switch (step.toLowerCase()) {
                    case "emotion" -> state = emotionAgent.execute(state);
                    case "skill" -> state = skillManager.executeSkills(state);
                    case "rag" -> state = ragAgentNode.execute(state);
                    case "companion" -> state = companionAgent.execute(state);
                    case "memory" -> state = memoryAgent.execute(state);
                    default -> log.warn("Unknown pipeline step: {}", step);
                }
            }
            
            PipelineResponse response = new PipelineResponse();
            response.setSuccess(true);
            response.setResult(state.getResult());
            response.setFinished(state.isFinished());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Pipeline execution failed", e);
            PipelineResponse response = new PipelineResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @Data
    public static class FullChatRequest {
        private Long userId;
        private String message;
        private String sessionId;
        private boolean useRag = false;
    }
    
    @Data
    public static class FullChatResponse {
        private boolean success;
        private String message;
        private String agentName;
        private String emotion;
        private Boolean needsSupport;
        private String skillUsed;
        private boolean ragUsed;
        private boolean memoryCreated;
        private String error;
    }
    
    @Data
    public static class PipelineRequest {
        private Long userId;
        private String message;
        private String[] steps;
    }
    
    @Data
    public static class PipelineResponse {
        private boolean success;
        private String result;
        private boolean finished;
        private String error;
    }
}

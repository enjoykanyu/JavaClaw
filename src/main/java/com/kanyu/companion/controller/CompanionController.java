package com.kanyu.companion.controller;

import com.kanyu.companion.agent.AgentRegistry;
import com.kanyu.companion.agent.CompanionAgent;
import com.kanyu.companion.agent.EmotionAgent;
import com.kanyu.companion.agent.MemoryAgent;
import com.kanyu.companion.model.CompanionProfile;
import com.kanyu.companion.service.CompanionService;
import com.kanyu.companion.service.MemoryService;
import com.kanyu.companion.service.SkillManager;
import com.kanyu.graph.state.GraphState;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 陪伴助手控制器
 * 使用重构后的Agent系统（通过依赖注入获取）
 */
@Slf4j
@RestController
@RequestMapping("/api/companion")
@RequiredArgsConstructor
public class CompanionController {

    private final ChatModel chatModel;
    private final CompanionService companionService;
    private final MemoryService memoryService;
    private final SkillManager skillManager;

    // 通过依赖注入获取Agent
    private final EmotionAgent emotionAgent;
    private final CompanionAgent companionAgent;
    private final MemoryAgent memoryAgent;
    private final AgentRegistry agentRegistry;

    @PostMapping("/profile")
    public ResponseEntity<ProfileResponse> createProfile(@RequestBody ProfileRequest request) {
        log.info("Creating companion profile for user: {}", request.getUserId());

        try {
            CompanionProfile profile = companionService.createProfile(
                request.getUserId(),
                request.getName(),
                request.getPersonality(),
                request.getSpeakingStyle(),
                request.getRelationship()
            );

            return ResponseEntity.ok(toProfileResponse(profile));

        } catch (Exception e) {
            log.error("Failed to create profile", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable Long userId) {
        return companionService.getProfile(userId)
            .map(profile -> ResponseEntity.ok(toProfileResponse(profile)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> updates) {
        log.info("Updating companion profile for user: {}", userId);

        try {
            CompanionProfile profile = companionService.updateProfile(userId, updates);
            return ResponseEntity.ok(toProfileResponse(profile));
        } catch (Exception e) {
            log.error("Failed to update profile", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Chat request from user: {}", request.getUserId());

        try {
            GraphState state = new GraphState();
            state.setUserInput(request.getMessage());
            state.put("userId", request.getUserId());

            // 1. 情感分析
            state = emotionAgent.execute(state);

            // 2. 技能路由 - 检查是否有技能可以处理
            state = skillManager.executeSkills(state);

            // 3. 如果没有技能处理，则使用 CompanionAgent
            if (state.get("skill_response") == null) {
                state = companionAgent.execute(state);
            }

            // 4. 记忆处理
            state = memoryAgent.execute(state);

            ChatResponse response = new ChatResponse();
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Chat failed", e);
            ChatResponse response = new ChatResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/chat/full")
    public ResponseEntity<ChatResponse> chatWithMemory(@RequestBody ChatRequest request) {
        log.info("Full chat request from user: {}", request.getUserId());

        try {
            GraphState state = new GraphState();
            state.setUserInput(request.getMessage());
            state.put("userId", request.getUserId());

            // 1. 情感分析
            state = emotionAgent.execute(state);

            // 2. 技能路由 - 检查是否有技能可以处理
            state = skillManager.executeSkills(state);

            // 3. 如果没有技能处理，则使用 CompanionAgent
            if (state.get("skill_response") == null) {
                state = companionAgent.execute(state);
            }

            // 4. 记忆处理
            state = memoryAgent.execute(state);

            ChatResponse response = new ChatResponse();
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Full chat failed", e);
            ChatResponse response = new ChatResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/prompt/{userId}")
    public ResponseEntity<String> getSystemPrompt(@PathVariable Long userId) {
        String prompt = companionService.buildSystemPrompt(userId);
        return ResponseEntity.ok(prompt);
    }

    /**
     * 获取所有可用的Agent列表
     */
    @GetMapping("/agents")
    public ResponseEntity<List<AgentInfo>> getAvailableAgents() {
        List<AgentInfo> agents = agentRegistry.getAllAgentDefinitions().stream()
            .map(def -> {
                AgentInfo info = new AgentInfo();
                info.setName(def.getName());
                info.setDescription(def.getDescription());
                info.setRole(def.getRole());
                info.setCapabilities(List.of(def.getCapabilities()));
                return info;
            })
            .toList();
        return ResponseEntity.ok(agents);
    }

    /**
     * 获取指定Agent的详细信息
     */
    @GetMapping("/agents/{agentName}")
    public ResponseEntity<String> getAgentDetail(@PathVariable String agentName) {
        String detail = agentRegistry.readAgent(agentName);
        return ResponseEntity.ok(detail);
    }

    private ProfileResponse toProfileResponse(CompanionProfile profile) {
        ProfileResponse response = new ProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUserId());
        response.setName(profile.getName());
        response.setPersonality(profile.getPersonality());
        response.setSpeakingStyle(profile.getSpeakingStyle());
        response.setRelationship(profile.getRelationship());
        response.setCustomRules(profile.getCustomRules());
        response.setPersonalityTraits(profile.getPersonalityTraits());
        response.setPreferences(profile.getPreferences());
        return response;
    }

    @Data
    public static class ProfileRequest {
        private Long userId;
        private String name;
        private String personality;
        private String speakingStyle;
        private String relationship;
    }

    @Data
    public static class ProfileResponse {
        private Long id;
        private Long userId;
        private String name;
        private String personality;
        private String speakingStyle;
        private String relationship;
        private String customRules;
        private Object personalityTraits;
        private Map<String, Object> preferences;
    }

    @Data
    public static class ChatRequest {
        private Long userId;
        private String message;
        private String sessionId;
    }

    @Data
    public static class ChatResponse {
        private boolean success;
        private String message;
        private String agentName;
        private String emotion;
        private Boolean needsSupport;
        private String error;
    }

    @Data
    public static class AgentInfo {
        private String name;
        private String description;
        private String role;
        private List<String> capabilities;
    }
}

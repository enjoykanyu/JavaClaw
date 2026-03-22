package com.kanyu.companion.agent;

import com.kanyu.companion.context.ContextManager;
import com.kanyu.companion.model.CompanionProfile;
import com.kanyu.companion.service.CompanionService;
import com.kanyu.companion.service.MemoryService;
import com.kanyu.companion.skill.SkillRegistry;
import com.kanyu.companion.mcp.McpToolRegistry;
import com.kanyu.graph.state.GraphState;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 陪伴Agent
 * 符合Spring AI Alibaba规范的主要对话Agent
 * 使用增强的上下文工程能力
 */
@Slf4j
@Component
public class CompanionAgent implements AgentExecutor {

    private final ChatModel chatModel;
    private final CompanionService companionService;
    private final MemoryService memoryService;
    private final SkillRegistry skillRegistry;
    private final McpToolRegistry mcpToolRegistry;
    private final AgentRegistry agentRegistry;
    private final ContextManager contextManager;

    private AgentDefinition definition;

    public CompanionAgent(ChatModel chatModel,
                          CompanionService companionService,
                          MemoryService memoryService,
                          SkillRegistry skillRegistry,
                          McpToolRegistry mcpToolRegistry,
                          AgentRegistry agentRegistry,
                          ContextManager contextManager) {
        this.chatModel = chatModel;
        this.companionService = companionService;
        this.memoryService = memoryService;
        this.skillRegistry = skillRegistry;
        this.mcpToolRegistry = mcpToolRegistry;
        this.agentRegistry = agentRegistry;
        this.contextManager = contextManager;
    }

    @PostConstruct
    public void init() {
        // 构建Agent定义
        this.definition = AgentDefinition.builder()
                .name("companion")
                .description("陪伴助手，提供情感支持和日常对话")
                .detailedDescription("""
                    这是一个陪伴型AI助手，旨在为用户提供情感支持和日常对话。
                    
                    能力：
                    1. 情感陪伴和倾听
                    2. 日常闲聊和互动
                    3. 根据用户记忆提供个性化回复
                    4. 调用Skill和Tool帮助用户
                    5. 维护长期对话记忆
                    """)
                .role("用户的虚拟伙伴")
                .capabilities(new String[]{
                        "情感陪伴",
                        "日常对话",
                        "记忆管理",
                        "Skill调用",
                        "Tool使用"
                })
                .systemPromptTemplate(buildBaseSystemPromptTemplate())
                .build();

        // 注册到AgentRegistry
        agentRegistry.registerAgent(definition, this);
        log.info("CompanionAgent registered to AgentRegistry");
    }

    @Override
    public AgentDefinition getDefinition() {
        return definition;
    }

    @Override
    public GraphState execute(GraphState state) {
        log.info("Executing CompanionAgent with enhanced context engineering");

        try {
            Long userId = state.get("userId");
            String userInput = state.getUserInput();

            CompanionProfile profile = companionService.getOrCreateDefaultProfile(userId);

            // 使用增强的上下文管理器构建上下文
            List<Message> conversationHistory = state.getMessages();

            // 准备模板变量
            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("name", profile.getName());
            templateVariables.put("relationship", profile.getRelationship());
            templateVariables.put("personality", profile.getPersonality());
            templateVariables.put("speakingStyle", profile.getSpeakingStyle());

            // 注入Skill和Tool信息
            String skillPrompt = skillRegistry.generateSkillListPrompt(userId);
            String toolPrompt = mcpToolRegistry.generateToolListPrompt();
            templateVariables.put("skills", skillPrompt);
            templateVariables.put("tools", toolPrompt);
            templateVariables.put("hasSkills", !skillPrompt.isEmpty());
            templateVariables.put("hasTools", !toolPrompt.isEmpty());

            // 构建系统提示词模板（包含条件渲染）
            String systemPromptTemplate = buildEnhancedSystemPromptTemplate();

            // 使用ContextManager构建优化的上下文
            ContextManager.ContextResult contextResult = contextManager.buildContext(
                userId,
                userInput,
                conversationHistory,
                systemPromptTemplate,
                templateVariables
            );

            List<Message> messages = new ArrayList<>(contextResult.messages());

            // 添加用户输入
            if (userInput != null && !userInput.isEmpty()) {
                messages.add(new UserMessage(userInput));
            }

            // 调用模型
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);

            String responseText = "";
            if (response != null && response.getResult() != null) {
                AssistantMessage assistantMessage = response.getResult().getOutput();
                responseText = assistantMessage.getContent();

                state.addMessage(new UserMessage(userInput));
                state.addMessage(assistantMessage);
            }

            state.put("companion_response", responseText);
            state.put("agent_name", profile.getName());

            // 添加上下文报告到状态（用于调试和监控）
            state.put("context_report", contextResult.report());

            log.debug("CompanionAgent response: {} (context: {} tokens, {} optimizations)",
                responseText.substring(0, Math.min(50, responseText.length())),
                contextResult.totalTokens(),
                contextResult.report().optimizations().size());

        } catch (Exception e) {
            log.error("CompanionAgent execution failed", e);
            state.setError("Companion agent failed: " + e.getMessage());
        }

        return state;
    }

    /**
     * 构建基础系统提示词模板
     */
    private String buildBaseSystemPromptTemplate() {
        return """
            你是{{name}}，是用户的{{relationship}}。

            【你的性格】
            {{personality}}

            【说话风格】
            {{speakingStyle}}

            【任务】
            1. 以温暖、理解的态度陪伴用户
            2. 记住用户的喜好和重要信息
            3. 在适当的时候提供帮助和建议
            4. 保持对话的自然和连贯

            【注意事项】
            - 保持你的角色设定一致性
            - 用符合你性格的方式回应
            - 记住用户告诉过你的事情
            {{#if hasSkills}}
            - 可以使用技能来帮助用户
            {{/if}}
            {{#if hasTools}}
            - 可以使用工具来获取信息
            {{/if}}
            """;
    }

    /**
     * 构建增强的系统提示词模板
     * 使用条件渲染和变量替换
     */
    private String buildEnhancedSystemPromptTemplate() {
        return """
            你是{{name}}，是用户的{{relationship}}。

            【你的性格】
            {{personality}}

            【说话风格】
            {{speakingStyle}}

            {{#if hasSkills}}
            【可用技能】
            {{skills}}
            {{/if}}

            {{#if hasTools}}
            【可用工具】
            {{tools}}
            {{/if}}

            【任务】
            1. 以温暖、理解的态度陪伴用户
            2. 记住用户的喜好和重要信息
            3. 在适当的时候提供帮助和建议
            4. 保持对话的自然和连贯

            【注意事项】
            - 保持你的角色设定一致性
            - 用符合你性格的方式回应
            - 记住用户告诉过你的事情
            {{#if hasSkills}}
            - 可以使用技能来帮助用户，格式：use_skill(skill_name, params)
            {{/if}}
            {{#if hasTools}}
            - 可以使用工具来获取信息，格式：use_tool(tool_name, params)
            {{/if}}
            """;
    }
}

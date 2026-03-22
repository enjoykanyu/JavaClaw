package com.kanyu.companion.config;

import com.kanyu.companion.agent.CompanionAgent;
import com.kanyu.companion.agent.EmotionAgent;
import com.kanyu.companion.agent.MemoryAgent;
import com.kanyu.companion.agent.McpToolNode;
import com.kanyu.companion.agent.RagAgentNode;
import com.kanyu.companion.mcp.McpToolRegistry;
import com.kanyu.companion.mcp.tools.GNewsMcpTool;
import com.kanyu.companion.service.CompanionService;
import com.kanyu.companion.service.MemoryService;
import com.kanyu.companion.service.RagService;
import com.kanyu.companion.service.SkillManager;
import com.kanyu.companion.skill.Skill;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Configuration
public class CompanionConfig {
    
    @Bean
    public CompanionAgent companionAgent(ChatModel chatModel, 
                                          CompanionService companionService,
                                          MemoryService memoryService) {
        return new CompanionAgent(chatModel, companionService, memoryService);
    }
    
    @Bean
    public EmotionAgent emotionAgent(ChatModel chatModel) {
        return new EmotionAgent(chatModel);
    }
    
    @Bean
    public MemoryAgent memoryAgent(ChatModel chatModel, MemoryService memoryService) {
        return new MemoryAgent(chatModel, memoryService);
    }
    
    @Bean
    public RagAgentNode ragAgentNode(ChatModel chatModel, RagService ragService) {
        return new RagAgentNode(chatModel, ragService);
    }
    
    @Bean
    public McpToolNode mcpToolNode(McpToolRegistry toolRegistry) {
        return new McpToolNode(toolRegistry);
    }
    
    @Bean
    public McpToolRegistry mcpToolRegistry() {
        McpToolRegistry registry = new McpToolRegistry();
        // 手动注册新闻工具
        registry.registerTool(gNewsMcpTool());
        return registry;
    }

    @Bean
    public GNewsMcpTool gNewsMcpTool() {
        return new GNewsMcpTool();
    }

    @Autowired
    public void registerSkills(SkillManager skillManager, @Lazy List<Skill> skills) {
        skillManager.registerSkills(skills);
    }
}

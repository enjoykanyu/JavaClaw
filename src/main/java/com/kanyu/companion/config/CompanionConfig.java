package com.kanyu.companion.config;

import com.kanyu.companion.agent.McpToolNode;
import com.kanyu.companion.mcp.McpToolRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Companion配置类
 *
 * 注意：Agent、Skill、MCP Tool现在都通过@Component自动注册
 * 无需在此手动创建Bean
 */
@Configuration
public class CompanionConfig {

    @Bean
    public McpToolNode mcpToolNode(McpToolRegistry toolRegistry) {
        return new McpToolNode(toolRegistry);
    }

    // Agent现在通过@Component和@PostConstruct自动注册到AgentRegistry
    // - CompanionAgent
    // - EmotionAgent
    // - MemoryAgent
    // - RagAgentNode

    // Skill现在通过@Component和@PostConstruct自动注册到SkillRegistry
    // - WeatherSkillExecutor
    // - NewsSkillExecutor

    // MCP Tool现在通过@Component和@PostConstruct自动注册到McpToolRegistry
    // - GNewsToolExecutor
}

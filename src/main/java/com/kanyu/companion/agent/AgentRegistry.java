package com.kanyu.companion.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent注册中心
 * 符合Spring AI Alibaba规范的Agent注册和管理
 */
@Slf4j
@Component
public class AgentRegistry {

    /**
     * Agent定义存储
     */
    private final Map<String, AgentDefinition> agentDefinitions = new ConcurrentHashMap<>();

    /**
     * Agent执行器存储
     */
    private final Map<String, AgentExecutor> agentExecutors = new ConcurrentHashMap<>();

    /**
     * 注册Agent
     * @param definition Agent定义
     * @param executor Agent执行器
     */
    public void registerAgent(AgentDefinition definition, AgentExecutor executor) {
        String agentName = definition.getName();
        agentDefinitions.put(agentName, definition);
        agentExecutors.put(agentName, executor);
        log.info("Registered agent: {}", agentName);
    }

    /**
     * 取消注册Agent
     * @param agentName Agent名称
     */
    public void unregisterAgent(String agentName) {
        agentDefinitions.remove(agentName);
        agentExecutors.remove(agentName);
        log.info("Unregistered agent: {}", agentName);
    }

    /**
     * 获取Agent定义
     * @param agentName Agent名称
     * @return Agent定义
     */
    public AgentDefinition getAgentDefinition(String agentName) {
        return agentDefinitions.get(agentName);
    }

    /**
     * 获取Agent执行器
     * @param agentName Agent名称
     * @return Agent执行器
     */
    public AgentExecutor getAgentExecutor(String agentName) {
        return agentExecutors.get(agentName);
    }

    /**
     * 获取所有Agent定义
     * @return Agent定义列表
     */
    public List<AgentDefinition> getAllAgentDefinitions() {
        return new ArrayList<>(agentDefinitions.values());
    }

    /**
     * 获取所有Agent名称
     * @return Agent名称列表
     */
    public Set<String> getAllAgentNames() {
        return new HashSet<>(agentDefinitions.keySet());
    }

    /**
     * 检查Agent是否存在
     * @param agentName Agent名称
     * @return 是否存在
     */
    public boolean hasAgent(String agentName) {
        return agentDefinitions.containsKey(agentName);
    }

    /**
     * 获取Agent数量
     * @return Agent数量
     */
    public int getAgentCount() {
        return agentDefinitions.size();
    }

    /**
     * 生成Agent列表提示词
     * @return Agent列表提示词
     */
    public String generateAgentListPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## 可用Agent\n\n");

        for (AgentDefinition definition : agentDefinitions.values()) {
            sb.append(definition.toBriefDescription()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 获取Agent详细描述
     * @param agentName Agent名称
     * @return Agent详细描述
     */
    public String readAgent(String agentName) {
        AgentDefinition definition = agentDefinitions.get(agentName);
        if (definition == null) {
            return "Agent not found: " + agentName;
        }
        return definition.toFullDescription();
    }
}

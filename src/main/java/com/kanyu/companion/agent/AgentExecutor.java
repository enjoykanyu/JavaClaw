package com.kanyu.companion.agent;

import com.kanyu.graph.state.GraphState;

/**
 * Agent执行器接口
 * 符合Spring AI Alibaba规范的Agent执行器
 */
public interface AgentExecutor {

    /**
     * 获取Agent定义
     * @return Agent定义
     */
    AgentDefinition getDefinition();

    /**
     * 执行Agent
     * @param state 图状态
     * @return 更新后的图状态
     */
    GraphState execute(GraphState state);

    /**
     * 获取Agent名称
     * @return Agent名称
     */
    default String getName() {
        AgentDefinition definition = getDefinition();
        return definition != null ? definition.getName() : getClass().getSimpleName();
    }
}

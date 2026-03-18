package com.kanyu.graph.node;

import com.kanyu.graph.state.GraphState;

/**
 * 图节点接口
 * 所有节点都需要实现此接口
 */
public interface Node {
    
    /**
     * 获取节点ID
     */
    String getId();
    
    /**
     * 获取节点名称
     */
    String getName();
    
    /**
     * 执行节点逻辑
     * @param state 当前状态
     * @return 执行后的状态
     */
    GraphState execute(GraphState state);
    
    /**
     * 获取节点类型
     */
    NodeType getType();
    
    enum NodeType {
        START,      // 起始节点
        AGENT,      // 智能体节点
        TOOL,       // 工具节点
        CONDITION,  // 条件节点
        END         // 结束节点
    }
}

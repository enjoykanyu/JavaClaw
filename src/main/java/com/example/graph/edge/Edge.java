package com.example.graph.edge;

import com.example.graph.state.GraphState;

import java.util.function.Function;

/**
 * 图边定义
 * 用于控制状态流转
 */
public class Edge {
    
    /**
     * 源节点ID
     */
    private final String sourceNodeId;
    
    /**
     * 目标节点ID
     */
    private final String targetNodeId;
    
    /**
     * 条件函数（可选）
     */
    private final Function<GraphState, Boolean> condition;
    
    /**
     * 边的名称/描述
     */
    private final String name;
    
    public Edge(String sourceNodeId, String targetNodeId) {
        this(sourceNodeId, targetNodeId, null, null);
    }
    
    public Edge(String sourceNodeId, String targetNodeId, String name) {
        this(sourceNodeId, targetNodeId, name, null);
    }
    
    public Edge(String sourceNodeId, String targetNodeId, String name, Function<GraphState, Boolean> condition) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.name = name;
        this.condition = condition;
    }
    
    /**
     * 检查是否可以通过此边
     */
    public boolean canTraverse(GraphState state) {
        if (condition == null) {
            return true;
        }
        return condition.apply(state);
    }
    
    public String getSourceNodeId() {
        return sourceNodeId;
    }
    
    public String getTargetNodeId() {
        return targetNodeId;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isConditional() {
        return condition != null;
    }
    
    @Override
    public String toString() {
        return String.format("Edge[%s -> %s%s]", 
            sourceNodeId, 
            targetNodeId,
            name != null ? " (" + name + ")" : "");
    }
}

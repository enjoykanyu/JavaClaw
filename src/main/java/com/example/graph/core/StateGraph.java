package com.example.graph.core;

import com.example.graph.edge.Edge;
import com.example.graph.node.Node;
import com.example.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 状态图
 * 用于编排节点和边的流转
 */
@Slf4j
public class StateGraph {
    
    /**
     * 节点映射
     */
    private final Map<String, Node> nodes = new HashMap<>();
    
    /**
     * 边映射（源节点 -> 边列表）
     */
    private final Map<String, List<Edge>> edges = new HashMap<>();
    
    /**
     * 起始节点ID
     */
    private String startNodeId;
    
    /**
     * 结束节点ID
     */
    private String endNodeId;
    
    /**
     * 添加节点
     */
    public StateGraph addNode(Node node) {
        nodes.put(node.getId(), node);
        log.debug("添加节点: {} ({})", node.getId(), node.getType());
        return this;
    }
    
    /**
     * 设置起始节点
     */
    public StateGraph setStartNode(String nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("节点不存在: " + nodeId);
        }
        this.startNodeId = nodeId;
        log.debug("设置起始节点: {}", nodeId);
        return this;
    }
    
    /**
     * 设置结束节点
     */
    public StateGraph setEndNode(String nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("节点不存在: " + nodeId);
        }
        this.endNodeId = nodeId;
        log.debug("设置结束节点: {}", nodeId);
        return this;
    }
    
    /**
     * 添加边
     */
    public StateGraph addEdge(String sourceNodeId, String targetNodeId) {
        return addEdge(sourceNodeId, targetNodeId, null, null);
    }
    
    /**
     * 添加带名称的边
     */
    public StateGraph addEdge(String sourceNodeId, String targetNodeId, String edgeName) {
        return addEdge(sourceNodeId, targetNodeId, edgeName, null);
    }
    
    /**
     * 添加条件边
     */
    public StateGraph addEdge(String sourceNodeId, String targetNodeId, 
                              String edgeName, java.util.function.Function<GraphState, Boolean> condition) {
        if (!nodes.containsKey(sourceNodeId)) {
            throw new IllegalArgumentException("源节点不存在: " + sourceNodeId);
        }
        if (!nodes.containsKey(targetNodeId)) {
            throw new IllegalArgumentException("目标节点不存在: " + targetNodeId);
        }
        
        Edge edge = new Edge(sourceNodeId, targetNodeId, edgeName, condition);
        edges.computeIfAbsent(sourceNodeId, k -> new ArrayList<>()).add(edge);
        log.debug("添加边: {}", edge);
        return this;
    }
    
    /**
     * 执行图
     */
    public GraphState execute(String input) {
        if (startNodeId == null) {
            throw new IllegalStateException("未设置起始节点");
        }
        
        GraphState state = new GraphState();
        state.setUserInput(input);
        state.setCurrentNodeId(startNodeId);
        
        log.info("开始执行图编排，输入: {}", input);
        
        int maxIterations = 100;
        int iterations = 0;
        
        while (!state.isFinished() && iterations < maxIterations) {
            String currentNodeId = state.getCurrentNodeId();
            Node currentNode = nodes.get(currentNodeId);
            
            if (currentNode == null) {
                state.setError("节点不存在: " + currentNodeId);
                break;
            }
            
            log.info("执行节点: {} ({})", currentNodeId, currentNode.getType());
            
            // 执行节点
            state = currentNode.execute(state);
            
            // 检查是否到达结束节点
            if (currentNodeId.equals(endNodeId)) {
                state.setFinished(true);
                break;
            }
            
            // 查找下一个节点
            String nextNodeId = findNextNode(state);
            if (nextNodeId == null) {
                log.warn("未找到下一个节点，结束执行");
                break;
            }
            
            state.setCurrentNodeId(nextNodeId);
            iterations++;
        }
        
        if (iterations >= maxIterations) {
            state.setError("执行超过最大迭代次数");
        }
        
        log.info("图编排执行完成，结果: {}", state.getResult());
        return state;
    }
    
    /**
     * 查找下一个节点
     */
    private String findNextNode(GraphState state) {
        String currentNodeId = state.getCurrentNodeId();
        List<Edge> outgoingEdges = edges.get(currentNodeId);
        
        if (outgoingEdges == null || outgoingEdges.isEmpty()) {
            return null;
        }
        
        // 优先返回满足条件的边
        for (Edge edge : outgoingEdges) {
            if (edge.canTraverse(state)) {
                log.debug("选择边: {}", edge);
                return edge.getTargetNodeId();
            }
        }
        
        // 如果没有条件边满足，返回第一个无条件边
        for (Edge edge : outgoingEdges) {
            if (!edge.isConditional()) {
                log.debug("选择默认边: {}", edge);
                return edge.getTargetNodeId();
            }
        }
        
        return null;
    }
    
    /**
     * 获取所有节点
     */
    public Collection<Node> getNodes() {
        return nodes.values();
    }
    
    /**
     * 获取所有边
     */
    public List<Edge> getAllEdges() {
        List<Edge> allEdges = new ArrayList<>();
        for (List<Edge> edgeList : edges.values()) {
            allEdges.addAll(edgeList);
        }
        return allEdges;
    }
    
    /**
     * 打印图结构
     */
    public void printGraph() {
        System.out.println("========== 图结构 ==========");
        System.out.println("节点列表:");
        for (Node node : nodes.values()) {
            String marker = "";
            if (node.getId().equals(startNodeId)) marker += " [START]";
            if (node.getId().equals(endNodeId)) marker += " [END]";
            System.out.println("  - " + node.getId() + " (" + node.getType() + ")" + marker);
        }
        System.out.println("\n边列表:");
        for (Edge edge : getAllEdges()) {
            System.out.println("  - " + edge);
        }
        System.out.println("============================");
    }
}

package com.kanyu.graph.node;

import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;

/**
 * 起始节点
 * 作为图编排的入口点，初始化状态
 */
@Slf4j
public class StartNode implements Node {
    
    private final String id;
    private final String name;
    
    public StartNode() {
        this("start", "Start Node");
    }
    
    public StartNode(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public GraphState execute(GraphState state) {
        log.info("执行起始节点: {}", name);
        
        // 初始化状态
        if (state.getUserInput() == null) {
            state.setUserInput("");
        }
        
        // 将用户输入存入数据中
        state.put("input", state.getUserInput());
        state.put("startTime", System.currentTimeMillis());
        
        log.debug("起始节点完成，输入: {}", state.getUserInput());
        return state;
    }
    
    @Override
    public NodeType getType() {
        return NodeType.START;
    }
}

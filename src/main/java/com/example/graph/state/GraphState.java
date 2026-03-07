package com.example.graph.state;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图编排全局状态
 * 用于在节点之间传递和共享数据
 */
@Data
public class GraphState {
    
    /**
     * 当前节点ID
     */
    private String currentNodeId;
    
    /**
     * 用户输入
     */
    private String userInput;
    
    /**
     * 对话历史
     */
    private List<Message> messages = new ArrayList<>();
    
    /**
     * 共享数据存储
     */
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * 是否结束
     */
    private boolean finished = false;
    
    /**
     * 最终结果
     */
    private String result;
    
    /**
     * 错误信息
     */
    private String error;
    
    public void put(String key, Object value) {
        this.data.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) this.data.get(key);
    }
    
    public void addMessage(Message message) {
        this.messages.add(message);
    }
}

package com.kanyu.companion.mcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class McpToolRegistry {
    
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    
    public void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
        log.info("Registered MCP tool: {}", tool.getName());
    }
    
    public void unregisterTool(String toolName) {
        tools.remove(toolName);
        log.info("Unregistered MCP tool: {}", toolName);
    }
    
    public McpTool getTool(String toolName) {
        return tools.get(toolName);
    }
    
    public List<McpTool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
    
    public int getToolCount() {
        return tools.size();
    }
    
    public interface McpTool {
        String getName();
        String getDescription();
        Map<String, Object> getParameters();
        McpToolResult execute(Map<String, Object> parameters);
    }
    
    @Data
    public static class McpToolResult {
        private boolean success;
        private String content;
        private String error;
        private Map<String, Object> metadata;
        
        public static McpToolResult success(String content) {
            McpToolResult result = new McpToolResult();
            result.setSuccess(true);
            result.setContent(content);
            return result;
        }
        
        public static McpToolResult error(String error) {
            McpToolResult result = new McpToolResult();
            result.setSuccess(false);
            result.setError(error);
            return result;
        }
    }
}

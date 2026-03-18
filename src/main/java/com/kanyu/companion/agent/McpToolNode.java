package com.kanyu.companion.agent;

import com.kanyu.companion.mcp.McpToolRegistry;
import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class McpToolNode {
    
    private final McpToolRegistry toolRegistry;
    
    public McpToolNode(McpToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }
    
    public GraphState execute(GraphState state) {
        log.info("Executing McpToolNode");
        
        try {
            String toolName = state.get("mcp_tool");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = state.get("mcp_params");
            
            if (toolName == null || toolName.isEmpty()) {
                log.warn("No MCP tool specified");
                state.put("mcp_result", null);
                return state;
            }
            
            McpToolRegistry.McpTool tool = toolRegistry.getTool(toolName);
            if (tool == null) {
                log.warn("Tool not found: {}", toolName);
                state.put("mcp_error", "Tool not found: " + toolName);
                return state;
            }
            
            Map<String, Object> toolParams = params != null ? params : new HashMap<>();
            
            McpToolRegistry.McpToolResult result = tool.execute(toolParams);
            
            if (result.isSuccess()) {
                state.put("mcp_result", result.getContent());
                state.put("mcp_tool_used", toolName);
            } else {
                state.put("mcp_error", result.getError());
            }
            
            log.debug("MCP tool {} executed: success={}", toolName, result.isSuccess());
            
        } catch (Exception e) {
            log.error("McpToolNode execution failed", e);
            state.put("mcp_error", e.getMessage());
        }
        
        return state;
    }
    
    public GraphState autoDetectAndExecute(GraphState state) {
        log.info("Auto-detecting and executing MCP tools");
        
        try {
            String userInput = state.getUserInput();
            if (userInput == null || userInput.isEmpty()) {
                return state;
            }
            
            userInput = userInput.toLowerCase();
            
            if (userInput.contains("天气") || userInput.contains("温度")) {
                state.put("mcp_tool", "get_weather");
                state.put("mcp_params", extractCityFromInput(userInput));
            } else if (userInput.contains("新闻")) {
                state.put("mcp_tool", "get_news");
            } else if (userInput.contains("提醒") || userInput.contains("闹钟")) {
                state.put("mcp_tool", "set_reminder");
            } else if (userInput.contains("搜索")) {
                state.put("mcp_tool", "web_search");
            }
            
            if (state.get("mcp_tool") != null) {
                return execute(state);
            }
            
        } catch (Exception e) {
            log.error("Auto-detect MCP tools failed", e);
        }
        
        return state;
    }
    
    private Map<String, Object> extractCityFromInput(String input) {
        Map<String, Object> params = new HashMap<>();
        
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "武汉", "西安", "重庆"};
        for (String city : cities) {
            if (input.contains(city)) {
                params.put("city", city);
                return params;
            }
        }
        
        params.put("city", "北京");
        return params;
    }
}

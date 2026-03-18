package com.kanyu.companion.mcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class McpToolDefinition {
    
    private String name;
    
    private String description;
    
    private List<McpToolParameter> inputSchema;
    
    @Data
    public static class McpToolParameter {
        private String name;
        private String type;
        private String description;
        private boolean required;
    }
}

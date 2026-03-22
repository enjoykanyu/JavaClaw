package com.kanyu.companion.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Tool 注册中心
 * 符合Spring AI Alibaba规范的MCP Tool注册和管理
 */
@Slf4j
@Component
public class McpToolRegistry {

    /**
     * Tool定义存储
     */
    private final Map<String, McpToolDefinition> toolDefinitions = new ConcurrentHashMap<>();

    /**
     * Tool执行器存储
     */
    private final Map<String, McpToolExecutor> toolExecutors = new ConcurrentHashMap<>();

    /**
     * 注册Tool
     * @param definition Tool定义
     * @param executor Tool执行器
     */
    public void registerTool(McpToolDefinition definition, McpToolExecutor executor) {
        String toolName = definition.getName();
        toolDefinitions.put(toolName, definition);
        toolExecutors.put(toolName, executor);
        log.info("Registered MCP tool: {}", toolName);
    }

    /**
     * 取消注册Tool
     * @param toolName Tool名称
     */
    public void unregisterTool(String toolName) {
        toolDefinitions.remove(toolName);
        toolExecutors.remove(toolName);
        log.info("Unregistered MCP tool: {}", toolName);
    }

    /**
     * 获取Tool定义
     * @param toolName Tool名称
     * @return Tool定义
     */
    public McpToolDefinition getToolDefinition(String toolName) {
        return toolDefinitions.get(toolName);
    }

    /**
     * 获取Tool执行器
     * @param toolName Tool名称
     * @return Tool执行器
     */
    public McpToolExecutor getToolExecutor(String toolName) {
        return toolExecutors.get(toolName);
    }

    /**
     * 获取所有Tool定义
     * @return Tool定义列表
     */
    public List<McpToolDefinition> getAllToolDefinitions() {
        return new ArrayList<>(toolDefinitions.values());
    }

    /**
     * 获取所有Tool名称
     * @return Tool名称列表
     */
    public Set<String> getAllToolNames() {
        return new HashSet<>(toolDefinitions.keySet());
    }

    /**
     * 检查Tool是否存在
     * @param toolName Tool名称
     * @return 是否存在
     */
    public boolean hasTool(String toolName) {
        return toolDefinitions.containsKey(toolName);
    }

    /**
     * 获取Tool数量
     * @return Tool数量
     */
    public int getToolCount() {
        return toolDefinitions.size();
    }

    /**
     * 执行Tool
     * @param toolName Tool名称
     * @param parameters 参数
     * @return 执行结果
     */
    public McpToolResult executeTool(String toolName, Map<String, Object> parameters) {
        McpToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            log.error("MCP tool not found: {}", toolName);
            return McpToolResult.error("Tool not found: " + toolName);
        }

        try {
            log.info("Executing MCP tool: {} with params: {}", toolName, parameters);
            return executor.execute(parameters);
        } catch (Exception e) {
            log.error("Failed to execute MCP tool: {}", toolName, e);
            return McpToolResult.error("Execution failed: " + e.getMessage());
        }
    }

    /**
     * 生成Tool列表提示词（用于系统提示）
     * @return Tool列表提示词
     */
    public String generateToolListPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## 可用工具\n\n");
        sb.append("你可以使用以下工具来帮助用户：\n\n");

        for (McpToolDefinition definition : toolDefinitions.values()) {
            sb.append(definition.toBriefDescription()).append("\n");
        }

        sb.append("\n使用工具时，请按以下格式调用：\n");
        sb.append("```\n");
        sb.append("tool_name({\"param1\": \"value1\", \"param2\": \"value2\"})\n");
        sb.append("```\n");

        return sb.toString();
    }

    /**
     * 生成Tool详细描述（用于read_tool）
     * @param toolName Tool名称
     * @return Tool详细描述
     */
    public String readTool(String toolName) {
        McpToolDefinition definition = toolDefinitions.get(toolName);
        if (definition == null) {
            return "Tool not found: " + toolName;
        }
        return definition.toFullDescription();
    }

    // ==================== 兼容旧接口 ====================

    /**
     * 兼容旧接口：注册Tool
     * @param tool 旧接口Tool
     * @deprecated 请使用 {@link #registerTool(McpToolDefinition, McpToolExecutor)}
     */
    @Deprecated
    public void registerTool(McpTool tool) {
        // 转换为新接口
        McpToolDefinition definition = convertToDefinition(tool);
        McpToolExecutor executor = new McpToolExecutor() {
            @Override
            public McpToolDefinition getDefinition() {
                return definition;
            }

            @Override
            public McpToolResult execute(Map<String, Object> params) {
                OldMcpToolResult result = tool.execute(params);
                return McpToolResult.builder()
                        .success(result.isSuccess())
                        .content(result.getContent())
                        .error(result.getError())
                        .metadata(result.getMetadata())
                        .build();
            }
        };
        registerTool(definition, executor);
    }

    /**
     * 兼容旧接口：获取Tool
     * @param toolName Tool名称
     * @return 旧接口Tool
     * @deprecated 请使用 {@link #getToolExecutor(String)}
     */
    @Deprecated
    public McpTool getTool(String toolName) {
        McpToolDefinition definition = toolDefinitions.get(toolName);
        McpToolExecutor executor = toolExecutors.get(toolName);

        if (definition == null || executor == null) {
            return null;
        }

        // 包装为旧接口
        return new McpTool() {
            @Override
            public String getName() {
                return definition.getName();
            }

            @Override
            public String getDescription() {
                return definition.getDescription();
            }

            @Override
            public Map<String, Object> getParameters() {
                // 转换为旧格式
                Map<String, Object> params = new LinkedHashMap<>();
                for (McpToolParameter param : definition.getParameters().values()) {
                    Map<String, Object> prop = new LinkedHashMap<>();
                    prop.put("type", param.getType());
                    prop.put("description", param.getDescription());
                    if (param.getEnumValues() != null) {
                        prop.put("enum", param.getEnumValues());
                    }
                    if (param.getDefaultValue() != null) {
                        prop.put("default", param.getDefaultValue());
                    }
                    params.put(param.getName(), prop);
                }
                return params;
            }

            @Override
            public OldMcpToolResult execute(Map<String, Object> parameters) {
                McpToolResult result = executor.execute(parameters);
                OldMcpToolResult oldResult = new OldMcpToolResult();
                oldResult.setSuccess(result.isSuccess());
                oldResult.setContent(result.getContent());
                oldResult.setError(result.getError());
                oldResult.setMetadata(result.getMetadata());
                return oldResult;
            }
        };
    }

    /**
     * 兼容旧接口：获取所有Tool
     * @return 旧接口Tool列表
     * @deprecated 请使用 {@link #getAllToolDefinitions()}
     */
    @Deprecated
    public List<McpTool> getAllTools() {
        List<McpTool> tools = new ArrayList<>();
        for (String toolName : toolDefinitions.keySet()) {
            McpTool tool = getTool(toolName);
            if (tool != null) {
                tools.add(tool);
            }
        }
        return tools;
    }

    /**
     * 将旧接口Tool转换为新定义
     */
    private McpToolDefinition convertToDefinition(McpTool tool) {
        Map<String, McpToolParameter> parameters = new HashMap<>();
        Map<String, Object> oldParams = tool.getParameters();

        if (oldParams != null) {
            for (Map.Entry<String, Object> entry : oldParams.entrySet()) {
                String paramName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> prop = (Map<String, Object>) entry.getValue();

                McpToolParameter param = McpToolParameter.builder()
                        .name(paramName)
                        .type((String) prop.get("type"))
                        .description((String) prop.get("description"))
                        .defaultValue(prop.get("default"))
                        .enumValues((List<String>) prop.get("enum"))
                        .build();
                parameters.put(paramName, param);
            }
        }

        return McpToolDefinition.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .parameters(parameters)
                .build();
    }

    // ==================== 旧接口定义（保持兼容） ====================

    /**
     * 旧接口：MCP Tool
     * @deprecated 请使用 {@link McpToolExecutor}
     */
    @Deprecated
    public interface McpTool {
        String getName();
        String getDescription();
        Map<String, Object> getParameters();
        OldMcpToolResult execute(Map<String, Object> parameters);
    }

    /**
     * 旧接口：MCP Tool结果
     * @deprecated 请使用 {@link com.kanyu.companion.mcp.McpToolResult}
     */
    @Deprecated
    @lombok.Data
    public static class OldMcpToolResult {
        private boolean success;
        private String content;
        private String error;
        private Map<String, Object> metadata;

        public static OldMcpToolResult success(String content) {
            OldMcpToolResult result = new OldMcpToolResult();
            result.setSuccess(true);
            result.setContent(content);
            return result;
        }

        public static OldMcpToolResult error(String error) {
            OldMcpToolResult result = new OldMcpToolResult();
            result.setSuccess(false);
            result.setError(error);
            return result;
        }
    }
}

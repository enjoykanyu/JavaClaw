package com.kanyu.companion.mcp;

import java.util.Map;

/**
 * MCP Tool 执行器接口
 * 符合Spring AI Alibaba规范的MCP Tool执行器
 */
public interface McpToolExecutor {

    /**
     * 获取Tool定义
     * @return Tool定义
     */
    McpToolDefinition getDefinition();

    /**
     * 执行Tool
     * @param parameters 参数
     * @return 执行结果
     */
    McpToolResult execute(Map<String, Object> parameters);
}

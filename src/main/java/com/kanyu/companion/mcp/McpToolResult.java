package com.kanyu.companion.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP Tool 执行结果
 * 符合Spring AI Alibaba规范的MCP Tool执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果内容
     */
    private String content;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建成功结果
     * @param content 结果内容
     * @return 成功结果
     */
    public static McpToolResult success(String content) {
        return McpToolResult.builder()
                .success(true)
                .content(content)
                .build();
    }

    /**
     * 创建成功结果（带元数据）
     * @param content 结果内容
     * @param metadata 元数据
     * @return 成功结果
     */
    public static McpToolResult success(String content, Map<String, Object> metadata) {
        return McpToolResult.builder()
                .success(true)
                .content(content)
                .metadata(metadata)
                .build();
    }

    /**
     * 创建错误结果
     * @param error 错误信息
     * @return 错误结果
     */
    public static McpToolResult error(String error) {
        return McpToolResult.builder()
                .success(false)
                .error(error)
                .build();
    }

    /**
     * 创建错误结果（带内容）
     * @param error 错误信息
     * @param content 降级内容
     * @return 错误结果
     */
    public static McpToolResult error(String error, String content) {
        return McpToolResult.builder()
                .success(false)
                .error(error)
                .content(content)
                .build();
    }
}

package com.kanyu.companion.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP Tool 参数定义
 * 符合Spring AI Alibaba规范的MCP Tool参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolParameter {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数类型：string, integer, number, boolean, array, object
     */
    private String type;

    /**
     * 参数描述
     */
    private String description;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 枚举值（如果有）
     */
    private List<String> enumValues;

    /**
     * 参数示例
     */
    private String example;
}

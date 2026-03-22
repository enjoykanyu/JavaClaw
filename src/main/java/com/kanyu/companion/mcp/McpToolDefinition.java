package com.kanyu.companion.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP Tool 定义
 * 符合Spring AI Alibaba规范的MCP Tool元数据定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolDefinition {

    /**
     * Tool唯一标识
     */
    private String name;

    /**
     * Tool描述
     */
    private String description;

    /**
     * Tool详细说明（用于描述Tool的能力）
     */
    private String detailedDescription;

    /**
     * Tool参数定义
     */
    @Builder.Default
    private Map<String, McpToolParameter> parameters = new HashMap<>();

    /**
     * Tool使用示例
     */
    @Builder.Default
    private String examples = "";

    /**
     * Tool返回值说明
     */
    @Builder.Default
    private String returnDescription = "";

    /**
     * Tool分类标签
     */
    @Builder.Default
    private String[] tags = new String[0];

    /**
     * 构建Tool的简要描述（用于系统提示词）
     */
    public String toBriefDescription() {
        return String.format("- %s: %s", name, description);
    }

    /**
     * 构建Tool的完整描述（用于详细说明）
     */
    public String toFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool: ").append(name).append("\n");
        sb.append("Description: ").append(detailedDescription != null ? detailedDescription : description).append("\n");

        if (!parameters.isEmpty()) {
            sb.append("Parameters:\n");
            for (McpToolParameter param : parameters.values()) {
                sb.append(String.format("  - %s (%s): %s%s\n",
                        param.getName(),
                        param.getType(),
                        param.getDescription(),
                        param.isRequired() ? " (required)" : " (optional)"
                ));
                if (param.getDefaultValue() != null) {
                    sb.append(String.format("    Default: %s\n", param.getDefaultValue()));
                }
                if (param.getEnumValues() != null && !param.getEnumValues().isEmpty()) {
                    sb.append(String.format("    Enum: %s\n", param.getEnumValues()));
                }
            }
        }

        if (!examples.isEmpty()) {
            sb.append("Examples:\n").append(examples).append("\n");
        }

        if (!returnDescription.isEmpty()) {
            sb.append("Returns: ").append(returnDescription).append("\n");
        }

        return sb.toString();
    }

    /**
     * 转换为JSON Schema格式（用于Function Calling）
     */
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("description", description);

        Map<String, Object> properties = new HashMap<>();
        for (McpToolParameter param : parameters.values()) {
            Map<String, Object> prop = new HashMap<>();
            prop.put("type", param.getType());
            prop.put("description", param.getDescription());
            if (param.getEnumValues() != null && !param.getEnumValues().isEmpty()) {
                prop.put("enum", param.getEnumValues());
            }
            if (param.getDefaultValue() != null) {
                prop.put("default", param.getDefaultValue());
            }
            properties.put(param.getName(), prop);
        }
        schema.put("properties", properties);

        // 收集必填参数
        var required = parameters.values().stream()
                .filter(McpToolParameter::isRequired)
                .map(McpToolParameter::getName)
                .toList();
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        return schema;
    }
}

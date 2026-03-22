package com.kanyu.companion.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent定义
 * 符合Spring AI Alibaba规范的Agent元数据定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDefinition {

    /**
     * Agent唯一标识
     */
    private String name;

    /**
     * Agent描述
     */
    private String description;

    /**
     * Agent详细说明
     */
    private String detailedDescription;

    /**
     * Agent角色定位
     */
    private String role;

    /**
     * Agent能力列表
     */
    @Builder.Default
    private String[] capabilities = new String[0];

    /**
     * Agent系统提示词模板
     */
    private String systemPromptTemplate;

    /**
     * Agent配置参数
     */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /**
     * Agent输入参数定义
     */
    @Builder.Default
    private Map<String, AgentParameter> inputParameters = new HashMap<>();

    /**
     * Agent输出定义
     */
    @Builder.Default
    private Map<String, AgentParameter> outputParameters = new HashMap<>();

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentParameter {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private Object defaultValue;
    }

    /**
     * 构建Agent的简要描述
     */
    public String toBriefDescription() {
        return String.format("- %s: %s", name, description);
    }

    /**
     * 构建Agent的完整描述
     */
    public String toFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Agent: ").append(name).append("\n");
        sb.append("Description: ").append(detailedDescription != null ? detailedDescription : description).append("\n");
        sb.append("Role: ").append(role).append("\n");

        if (capabilities.length > 0) {
            sb.append("Capabilities:\n");
            for (String capability : capabilities) {
                sb.append("  - ").append(capability).append("\n");
            }
        }

        if (!inputParameters.isEmpty()) {
            sb.append("Input Parameters:\n");
            for (AgentParameter param : inputParameters.values()) {
                sb.append(String.format("  - %s (%s): %s%s\n",
                        param.getName(),
                        param.getType(),
                        param.getDescription(),
                        param.isRequired() ? " (required)" : " (optional)"
                ));
            }
        }

        return sb.toString();
    }

    /**
     * 渲染系统提示词
     * @param variables 变量
     * @return 渲染后的提示词
     */
    public String renderSystemPrompt(Map<String, Object> variables) {
        if (systemPromptTemplate == null) {
            return "";
        }

        String prompt = systemPromptTemplate;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            prompt = prompt.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return prompt;
    }
}

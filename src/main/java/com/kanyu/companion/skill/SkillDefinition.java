package com.kanyu.companion.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill定义
 * 符合Spring AI Alibaba规范的Skill元数据定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDefinition {

    /**
     * Skill唯一标识
     */
    private String name;

    /**
     * Skill描述
     */
    private String description;

    /**
     * Skill详细说明（用于read_skill返回）
     */
    private String detailedDescription;

    /**
     * Skill参数定义
     */
    @Builder.Default
    private Map<String, SkillParameter> parameters = new HashMap<>();

    /**
     * Skill示例
     */
    @Builder.Default
    private String examples = "";

    /**
     * Skill返回值说明
     */
    @Builder.Default
    private String returnDescription = "";

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillParameter {
        private String name;
        private String type;
        private String description;
        private boolean required;
        @Builder.Default
        private Object defaultValue = null;
    }

    /**
     * 构建Skill的简要描述（用于系统提示词）
     */
    public String toBriefDescription() {
        return String.format("- %s: %s", name, description);
    }

    /**
     * 构建Skill的完整描述（用于read_skill返回）
     */
    public String toFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Skill: ").append(name).append("\n");
        sb.append("Description: ").append(detailedDescription != null ? detailedDescription : description).append("\n");

        if (!parameters.isEmpty()) {
            sb.append("Parameters:\n");
            for (SkillParameter param : parameters.values()) {
                sb.append(String.format("  - %s (%s): %s%s\n",
                    param.getName(),
                    param.getType(),
                    param.getDescription(),
                    param.isRequired() ? " (required)" : " (optional)"
                ));
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
}

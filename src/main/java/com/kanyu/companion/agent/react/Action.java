package com.kanyu.companion.agent.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 行动定义
 *
 * 技术点：
 * 1. 封装ReAct循环中的行动调用
 * 2. 支持工具名称、参数、原因说明
 * 3. 支持多种行动类型（工具调用、知识检索、计算等）
 *
 * ：
 * - 类型安全：使用枚举定义行动类型
 * - 可扩展：支持自定义行动类型
 * - 可序列化：便于日志记录和审计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {

    /**
     * 行动类型
     */
    public enum ActionType {
        /**
         * 工具调用：调用外部工具（MCP Tool、Skill等）
         */
        TOOL_CALL,

        /**
         * 知识检索：从知识库检索信息
         */
        KNOWLEDGE_RETRIEVAL,

        /**
         * 记忆检索：从记忆系统检索信息
         */
        MEMORY_RETRIEVAL,

        /**
         * 计算：执行计算操作
         */
        CALCULATION,

        /**
         * 搜索：执行搜索操作
         */
        SEARCH,

        /**
         * 完成：生成最终答案
         */
        FINISH
    }

    /**
     * 行动类型
     */
    private ActionType type;

    /**
     * 工具/行动名称
     */
    private String name;

    /**
     * 行动参数
     */
    @Builder.Default
    private Map<String, Object> params = Map.of();

    /**
     * 行动原因（为什么执行这个行动）
     */
    private String reason;

    /**
     * 是否为最终行动
     */
    private boolean finalAction;

    /**
     * 创建工具调用行动
     */
    public static Action toolCall(String toolName, Map<String, Object> params, String reason) {
        return Action.builder()
                .type(ActionType.TOOL_CALL)
                .name(toolName)
                .params(params != null ? params : Map.of())
                .reason(reason)
                .finalAction(false)
                .build();
    }

    /**
     * 创建知识检索行动
     */
    public static Action knowledgeRetrieval(String query, String reason) {
        return Action.builder()
                .type(ActionType.KNOWLEDGE_RETRIEVAL)
                .name("knowledge_retrieval")
                .params(Map.of("query", query))
                .reason(reason)
                .finalAction(false)
                .build();
    }

    /**
     * 创建记忆检索行动
     */
    public static Action memoryRetrieval(String query, Long userId, String reason) {
        return Action.builder()
                .type(ActionType.MEMORY_RETRIEVAL)
                .name("memory_retrieval")
                .params(Map.of("query", query, "userId", userId))
                .reason(reason)
                .finalAction(false)
                .build();
    }

    /**
     * 创建计算行动
     */
    public static Action calculation(String expression, String reason) {
        return Action.builder()
                .type(ActionType.CALCULATION)
                .name("calculation")
                .params(Map.of("expression", expression))
                .reason(reason)
                .finalAction(false)
                .build();
    }

    /**
     * 创建搜索行动
     */
    public static Action search(String query, String reason) {
        return Action.builder()
                .type(ActionType.SEARCH)
                .name("search")
                .params(Map.of("query", query))
                .reason(reason)
                .finalAction(false)
                .build();
    }

    /**
     * 创建完成行动
     */
    public static Action finish(String answer) {
        return Action.builder()
                .type(ActionType.FINISH)
                .name("finish")
                .params(Map.of("answer", answer))
                .reason("任务完成，生成最终答案")
                .finalAction(true)
                .build();
    }

    /**
     * 获取参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParam(String key) {
        return (T) params.get(key);
    }

    /**
     * 格式化为字符串（用于提示词）
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");

        if (params != null && !params.isEmpty()) {
            params.forEach((key, value) -> {
                sb.append(key).append("=");
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }
                sb.append(", ");
            });
            // 移除最后的逗号和空格
            sb.setLength(sb.length() - 2);
        }

        sb.append(")");
        return sb.toString();
    }
}

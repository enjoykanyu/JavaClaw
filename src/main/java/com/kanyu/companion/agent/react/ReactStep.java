package com.kanyu.companion.agent.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * React步骤
 * 
 * 技术点：
 * 1. 记录ReAct循环中的每一个步骤
 * 2. 支持三种类型：THOUGHT(推理)、ACTION(行动)、OBSERVATION(观察)
 * 3. 包含完整的执行信息（输入、输出、耗时、Token消耗）
 * 
 *
 * - 可追溯：每个步骤都有时间戳和序号
 * * - 可观测：记录Token消耗和执行时长
 * - 可审计：完整的输入输出记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactStep {

    /**
     * 步骤类型
     */
    public enum StepType {
        /**
         * 推理步骤：分析当前情况，决定下一步行动
         */
        THOUGHT,

        /**
         * 行动步骤：执行具体的工具调用
         */
        ACTION,

        /**
         * 观察步骤：收集行动的执行结果
         */
        OBSERVATION,

        /**
         * 完成步骤：生成最终答案
         */
        FINISH
    }

    /**
     * 步骤序号
     */
    private int stepNumber;

    /**
     * 步骤类型
     */
    private StepType type;

    /**
     * 步骤内容
     */
    private String content;

    /**
     * 输入（用于ACTION类型）
     */
    private String input;

    /**
     * 输出结果
     */
    private String output;

    /**
     * 使用的工具（用于ACTION类型）
     */
    private String toolName;

    /**
     * 工具参数（用于ACTION类型）
     */
    private String toolParams;

    /**
     * Token消耗
     */
    private int tokenUsage;

    /**
     * 执行时长（毫秒）
     */
    private long executionTimeMs;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 创建思考步骤
     */
    public static ReactStep thought(int stepNumber, String content, int tokenUsage) {
        return ReactStep.builder()
                .stepNumber(stepNumber)
                .type(StepType.THOUGHT)
                .content(content)
                .tokenUsage(tokenUsage)
                .createdAt(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * 创建行动步骤
     */
    public static ReactStep action(int stepNumber, String toolName, String toolParams, String input) {
        return ReactStep.builder()
                .stepNumber(stepNumber)
                .type(StepType.ACTION)
                .toolName(toolName)
                .toolParams(toolParams)
                .input(input)
                .createdAt(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * 创建观察步骤
     */
    public static ReactStep observation(int stepNumber, String output, long executionTimeMs) {
        return ReactStep.builder()
                .stepNumber(stepNumber)
                .type(StepType.OBSERVATION)
                .output(output)
                .executionTimeMs(executionTimeMs)
                .createdAt(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * 创建完成步骤
     */
    public static ReactStep finish(int stepNumber, String answer, int tokenUsage) {
        return ReactStep.builder()
                .stepNumber(stepNumber)
                .type(StepType.FINISH)
                .content(answer)
                .tokenUsage(tokenUsage)
                .createdAt(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * 创建错误步骤
     */
    public static ReactStep error(int stepNumber, StepType type, String error) {
        return ReactStep.builder()
                .stepNumber(stepNumber)
                .type(type)
                .error(error)
                .createdAt(LocalDateTime.now())
                .success(false)
                .build();
    }

    /**
     * 格式化为字符串（用于提示词）
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();

        switch (type) {
            case THOUGHT -> sb.append("思考: ").append(content);
            case ACTION -> sb.append("行动: ").append(toolName)
                    .append("(").append(toolParams).append(")");
            case OBSERVATION -> sb.append("观察: ").append(output);
            case FINISH -> sb.append("完成: ").append(content);
        }

        return sb.toString();
    }
}

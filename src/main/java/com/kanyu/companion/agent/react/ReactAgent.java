package com.kanyu.companion.agent.react;

import com.kanyu.companion.agent.AgentDefinition;
import com.kanyu.companion.agent.AgentExecutor;
import com.kanyu.companion.agent.AgentRegistry;
import com.kanyu.graph.state.GraphState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ReactAgent - 推理-行动循环Agent
 *
 * 技术点：
 * 1. 实现ReAct模式：Thought → Action → Observation → Thought → ...
 * 2. 支持最大循环次数限制，防止无限循环
 * 3. 支持多种终止条件（完成、超时、错误）
 * 4. 完整的执行历史记录
 *
 *
 * - 可配置：最大循环次数、超时时间可配置
 * - 可观测：详细的执行日志和步骤记录
 * - 可回滚：支持中断和恢复
 * - 容错：错误处理和降级策略
 * - 性能：Token消耗和执行时长监控
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactAgent implements AgentExecutor {

    private final ThoughtGenerator thoughtGenerator;
    private final ActionExecutor actionExecutor;
    private final AgentRegistry agentRegistry;

    private AgentDefinition definition;

    // 配置参数
    private static final int MAX_ITERATIONS = 10;  // 最大循环次数
    private static final long MAX_EXECUTION_TIME_MS = 60000;  // 最大执行时间（60秒）

    @PostConstruct
    public void init() {
        // 构建Agent定义
        this.definition = AgentDefinition.builder()
                .name("react")
                .description("ReAct推理-行动循环Agent，使用工具解决复杂问题")
                .detailedDescription("""
                    这是一个基于ReAct（Reasoning + Acting）模式的智能Agent。
                    
                    工作流程：
                    1. 思考：分析问题，决定下一步行动
                    2. 行动：执行工具调用（Skill、MCP Tool、知识检索等）
                    3. 观察：收集执行结果
                    4. 重复直到问题解决或达到最大循环次数
                    
                    能力：
                    - 多步推理和工具调用
                    - 知识检索和记忆查询
                    - 数学计算和搜索
                    - 自动终止和错误处理
                    """)
                .role("智能问题解决助手")
                .capabilities(new String[]{
                        "多步推理",
                        "工具调用",
                        "知识检索",
                        "记忆查询",
                        "数学计算",
                        "自动终止"
                })
                .systemPromptTemplate(buildSystemPromptTemplate())
                .build();

        // 注册到AgentRegistry
        agentRegistry.registerAgent(definition, this);
        log.info("ReactAgent registered to AgentRegistry");
    }

    @Override
    public AgentDefinition getDefinition() {
        return definition;
    }

    @Override
    public GraphState execute(GraphState state) {
        log.info("Executing ReactAgent");

        long startTime = System.currentTimeMillis();
        Long userId = state.get("userId");
        String userInput = state.getUserInput();

        // 初始化
        List<ReactStep> history = new ArrayList<>();
        String finalAnswer = "";
        boolean finished = false;
        int iteration = 0;

        try {
            while (!finished && iteration < MAX_ITERATIONS) {
                iteration++;
                log.debug("React iteration {}/{}: {}", iteration, MAX_ITERATIONS, userInput);

                // 检查超时
                if (System.currentTimeMillis() - startTime > MAX_EXECUTION_TIME_MS) {
                    log.warn("ReactAgent execution timeout");
                    finalAnswer = "执行超时，请简化您的问题或稍后重试。";
                    break;
                }

                // Step 1: 生成思考
                ThoughtGenerator.ThoughtResult thoughtResult =
                    thoughtGenerator.generateThought(userInput, history, userId);

                if (!thoughtResult.isSuccess()) {
                    log.error("Thought generation failed: {}", thoughtResult.getError());
                    finalAnswer = "思考过程出错：" + thoughtResult.getError();
                    break;
                }

                // 记录思考步骤
                ReactStep thoughtStep = ReactStep.thought(
                    iteration,
                    thoughtResult.getThought(),
                    0  // Token消耗由ThoughtGenerator记录
                );
                history.add(thoughtStep);

                log.debug("Thought: {}", thoughtResult.getThought());

                // Step 2: 执行行动
                Action action = thoughtResult.getAction();
                if (action == null) {
                    log.error("No action generated");
                    finalAnswer = "无法确定下一步行动";
                    break;
                }

                // 记录行动步骤
                ReactStep actionStep = ReactStep.action(
                    iteration,
                    action.getName(),
                    action.getParams() != null ? action.getParams().toString() : "",
                    action.getReason()
                );
                history.add(actionStep);

                // 检查是否完成
                if (thoughtResult.isFinished() || action.isFinalAction()) {
                    finished = true;
                    finalAnswer = action.getParam("answer");
                    if (finalAnswer == null) {
                        finalAnswer = thoughtResult.getThought();
                    }

                    // 记录完成步骤
                    ReactStep finishStep = ReactStep.finish(iteration, finalAnswer, 0);
                    history.add(finishStep);

                    log.info("ReactAgent finished after {} iterations", iteration);
                    break;
                }

                // Step 3: 执行行动并观察
                long actionStartTime = System.currentTimeMillis();
                ActionExecutor.ActionResult actionResult =
                    actionExecutor.execute(action, userId);
                long actionTime = System.currentTimeMillis() - actionStartTime;

                // 记录观察步骤
                String observation = actionResult.isSuccess()
                    ? actionResult.getOutput()
                    : "错误: " + actionResult.getError();

                ReactStep observationStep = ReactStep.observation(
                    iteration,
                    observation,
                    actionTime
                );
                observationStep.setSuccess(actionResult.isSuccess());
                if (!actionResult.isSuccess()) {
                    observationStep.setError(actionResult.getError());
                }
                history.add(observationStep);

                log.debug("Observation: {} ({}ms)",
                    observation.substring(0, Math.min(50, observation.length())),
                    actionTime);

                // 更新用户输入（包含观察结果）
                userInput = observation;
            }

            // 如果达到最大循环次数仍未完成
            if (!finished && iteration >= MAX_ITERATIONS) {
                log.warn("ReactAgent reached max iterations");
                finalAnswer = "问题较复杂，已达到最大思考次数。已完成的思考：\n" +
                    generateSummary(history);
            }

        } catch (Exception e) {
            log.error("ReactAgent execution failed", e);
            finalAnswer = "执行出错：" + e.getMessage();
        }

        long totalTime = System.currentTimeMillis() - startTime;

        // 更新状态
        state.put("react_response", finalAnswer);
        state.put("react_history", history);
        state.put("react_iterations", iteration);
        state.put("react_execution_time_ms", totalTime);
        state.put("react_finished", finished);

        // 添加消息到对话历史
        if (userInput != null && !userInput.isEmpty()) {
            state.addMessage(new UserMessage(state.getUserInput()));
        }
        state.addMessage(new AssistantMessage(finalAnswer));

        log.info("ReactAgent completed in {}ms, {} iterations, finished: {}",
            totalTime, iteration, finished);

        return state;
    }

    /**
     * 生成执行摘要
     */
    private String generateSummary(List<ReactStep> history) {
        StringBuilder sb = new StringBuilder();
        for (ReactStep step : history) {
            sb.append(step.toPromptString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建系统提示词模板
     */
    private String buildSystemPromptTemplate() {
        return """
            你是一个智能助手，使用ReAct（推理-行动）模式解决问题。

            工作流程：
            1. 思考：分析问题，决定下一步行动
            2. 行动：执行工具调用
            3. 观察：收集执行结果
            4. 重复直到问题解决

            可用工具：
            - Skill和MCP Tool
            - knowledge_retrieval - 知识检索
            - memory_retrieval - 记忆检索
            - calculation - 数学计算
            - search - 搜索
            - finish - 完成任务

            输出格式：
            思考：[你的分析]
            行动：[工具名称(参数)] 或 [完成(答案)]
            """;
    }

    /**
     * 获取执行历史
     */
    @SuppressWarnings("unchecked")
    public List<ReactStep> getHistory(GraphState state) {
        Object history = state.get("react_history");
        if (history instanceof List) {
            return (List<ReactStep>) history;
        }
        return new ArrayList<>();
    }

    /**
     * 获取执行统计
     */
    public ReactStats getStats(GraphState state) {
        return new ReactStats(
            (Integer) state.getOrDefault("react_iterations", 0),
            (Long) state.getOrDefault("react_execution_time_ms", 0L),
            (Boolean) state.getOrDefault("react_finished", false)
        );
    }

    /**
     * 执行统计
     */
    public record ReactStats(int iterations, long executionTimeMs, boolean finished) {}
}

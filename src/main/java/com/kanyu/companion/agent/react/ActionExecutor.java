package com.kanyu.companion.agent.react;

import com.kanyu.companion.mcp.McpToolRegistry;
import com.kanyu.companion.service.MemoryService;
import com.kanyu.companion.service.RagService;
import com.kanyu.companion.skill.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

/**
 * 行动执行器
 *
 * 技术点：
 * 1. 根据Action类型路由到对应的执行逻辑
 * 2. 支持工具调用、知识检索、记忆检索、计算等多种行动
 * 3. 统一的结果封装和错误处理
 *
 * ：
 * - 单一职责：每个执行方法只负责一种行动类型
 * - 开闭原则：易于添加新的行动类型
 * - 容错处理：每个执行方法都有try-catch保护
 * - 可观测性：详细的日志记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActionExecutor {

    private final McpToolRegistry toolRegistry;
    private final SkillRegistry skillRegistry;
    private final MemoryService memoryService;
    private final RagService ragService;

    // JavaScript引擎用于计算
    private final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");

    /**
     * 执行行动
     *
     * @param action 行动定义
     * @param userId 用户ID
     * @return 执行结果
     */
    public ActionResult execute(Action action, Long userId) {
        log.info("Executing action: {} ({}) for user: {}",
            action.getName(), action.getType(), userId);

        long startTime = System.currentTimeMillis();

        try {
            ActionResult result = switch (action.getType()) {
                case TOOL_CALL -> executeToolCall(action);
                case KNOWLEDGE_RETRIEVAL -> executeKnowledgeRetrieval(action);
                case MEMORY_RETRIEVAL -> executeMemoryRetrieval(action, userId);
                case CALCULATION -> executeCalculation(action);
                case SEARCH -> executeSearch(action);
                case FINISH -> executeFinish(action);
            };

            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);

            log.debug("Action executed successfully in {}ms", executionTime);
            return result;

        } catch (Exception e) {
            log.error("Action execution failed: {}", action.getName(), e);
            return ActionResult.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行工具调用
     */
    private ActionResult executeToolCall(Action action) {
        String toolName = action.getName();
        Map<String, Object> params = action.getParams();

        // 优先尝试Skill
        if (skillRegistry.hasSkill(toolName)) {
            var result = skillRegistry.executeSkill(toolName, params);
            return ActionResult.success(result.getContent());
        }

        // 然后尝试MCP Tool
        if (toolRegistry.hasTool(toolName)) {
            var result = toolRegistry.executeTool(toolName, params);
            if (result.isSuccess()) {
                return ActionResult.success(result.getContent());
            } else {
                return ActionResult.error(result.getError());
            }
        }

        return ActionResult.error("工具未找到: " + toolName);
    }

    /**
     * 执行知识检索
     */
    private ActionResult executeKnowledgeRetrieval(Action action) {
        String query = action.getParam("query");
        if (query == null || query.isEmpty()) {
            return ActionResult.error("查询不能为空");
        }

        String context = ragService.queryWithContext(query, null);
        return ActionResult.success(context != null ? context : "未找到相关知识");
    }

    /**
     * 执行记忆检索
     */
    private ActionResult executeMemoryRetrieval(Action action, Long userId) {
        String query = action.getParam("query");
        if (query == null || query.isEmpty()) {
            return ActionResult.error("查询不能为空");
        }

        var memories = memoryService.retrieveRelevantMemories(userId, query, 5);

        if (memories.isEmpty()) {
            return ActionResult.success("未找到相关记忆");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("找到以下相关记忆：\n");
        for (int i = 0; i < memories.size(); i++) {
            var memory = memories.get(i);
            sb.append(i + 1).append(". ").append(memory.getContent()).append("\n");
        }

        return ActionResult.success(sb.toString());
    }

    /**
     * 执行计算
     */
    private ActionResult executeCalculation(Action action) {
        String expression = action.getParam("expression");
        if (expression == null || expression.isEmpty()) {
            return ActionResult.error("表达式不能为空");
        }

        try {
            Object result = scriptEngine.eval(expression);
            return ActionResult.success(String.valueOf(result));
        } catch (ScriptException e) {
            log.error("Calculation failed: {}", expression, e);
            return ActionResult.error("计算错误: " + e.getMessage());
        }
    }

    /**
     * 执行搜索
     */
    private ActionResult executeSearch(Action action) {
        String query = action.getParam("query");
        if (query == null || query.isEmpty()) {
            return ActionResult.error("查询不能为空");
        }

        // 这里可以集成搜索引擎API
        // 目前返回知识库搜索结果
        String context = ragService.queryWithContext(query, null);
        return ActionResult.success(context != null ? context : "未找到搜索结果");
    }

    /**
     * 执行完成行动
     */
    private ActionResult executeFinish(Action action) {
        String answer = action.getParam("answer");
        return ActionResult.success(answer != null ? answer : "");
    }

    /**
     * 行动执行结果
     */
    public static class ActionResult {
        private final boolean success;
        private final String output;
        private final String error;
        private long executionTimeMs;

        private ActionResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }

        public static ActionResult success(String output) {
            return new ActionResult(true, output, null);
        }

        public static ActionResult error(String error) {
            return new ActionResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }
    }
}

package com.kanyu.companion.agent.react;

import com.kanyu.companion.context.ContextManager;
import com.kanyu.companion.skill.SkillRegistry;
import com.kanyu.companion.mcp.McpToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 思考生成器
 *
 * 技术点：
 * 1. 根据当前状态和历史步骤生成推理（Thought）
 * 2. 解析Thought提取Action
 * 3. 支持多种思考策略（Chain-of-Thought、Tree-of-Thoughts等）
 *
 * ：
 * - 策略模式：支持多种思考策略
 * - 可配置：思考深度、Token预算可配置
 * - 可回滚：思考失败可重试
 * - 可观测：记录思考过程和Token消耗
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThoughtGenerator {

    private final ChatModel chatModel;
    private final SkillRegistry skillRegistry;
    private final McpToolRegistry toolRegistry;
    private final ContextManager contextManager;

    // Token管理
    private static final int MAX_THOUGHT_TOKENS = 1000;

    /**
     * 生成思考
     *
     * @param userInput 用户输入
     * @param history 历史步骤
     * @param userId 用户ID
     * @return 思考结果（包含Thought和Action）
     */
    public ThoughtResult generateThought(
            String userInput,
            List<ReactStep> history,
            Long userId) {

        log.debug("Generating thought for: {}", userInput);

        try {
            // 构建提示词
            List<Message> messages = buildThoughtPrompt(userInput, history, userId);

            // 调用模型
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);

            if (response == null || response.getResult() == null) {
                return ThoughtResult.error("模型返回为空");
            }

            String thoughtText = response.getResult().getOutput().getContent();

            // 解析Thought和Action
            ThoughtResult result = parseThought(thoughtText);

            log.debug("Thought generated: {}", result.getThought());
            return result;

        } catch (Exception e) {
            log.error("Thought generation failed", e);
            return ThoughtResult.error("思考生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建思考提示词
     */
    private List<Message> buildThoughtPrompt(
            String userInput,
            List<ReactStep> history,
            Long userId) {

        List<Message> messages = new ArrayList<>();

        // 系统提示词
        String systemPrompt = buildSystemPrompt(userId);
        messages.add(new SystemMessage(systemPrompt));

        // 历史步骤
        if (history != null && !history.isEmpty()) {
            StringBuilder historyText = new StringBuilder();
            historyText.append("之前的思考过程：\n");
            for (ReactStep step : history) {
                historyText.append(step.toPromptString()).append("\n");
            }
            messages.add(new SystemMessage(historyText.toString()));
        }

        // 用户输入
        messages.add(new UserMessage("用户问题：" + userInput));

        // 思考指令
        messages.add(new UserMessage("""
            请按照以下格式进行思考：

            思考：[分析当前情况，决定下一步行动]
            行动：[工具名称(参数)] 或 [完成(答案)]

            可用工具：
            """ + buildAvailableToolsPrompt()));

        return messages;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(Long userId) {
        return """
            你是一个智能助手，使用ReAct（推理-行动）模式解决问题。

            你的工作流程：
            1. 思考：分析问题，决定下一步行动
            2. 行动：执行工具调用或生成答案
            3. 观察：收集执行结果
            4. 重复直到问题解决

            思考原则：
            - 仔细分析问题，不要急于下结论
            - 如果需要外部信息，使用工具获取
            - 如果可以直接回答，使用"完成"行动
            - 保持思考的连贯性

            行动格式：
            - 工具调用：工具名称(参数1=值1, 参数2=值2)
            - 完成：完成(最终答案)
            """;
    }

    /**
     * 构建可用工具提示词
     */
    private String buildAvailableToolsPrompt() {
        StringBuilder sb = new StringBuilder();

        // 添加Skill列表
        sb.append(skillRegistry.generateSkillListPrompt(null));

        // 添加Tool列表
        sb.append(toolRegistry.generateToolListPrompt());

        // 添加内置工具
        sb.append("""
            内置工具：
            - knowledge_retrieval(query=查询内容) - 从知识库检索信息
            - memory_retrieval(query=查询内容) - 从记忆系统检索信息
            - calculation(expression=数学表达式) - 执行数学计算
            - search(query=搜索内容) - 执行搜索
            - finish(answer=最终答案) - 完成任务，生成最终答案
            """);

        return sb.toString();
    }

    /**
     * 解析Thought文本
     */
    private ThoughtResult parseThought(String thoughtText) {
        if (thoughtText == null || thoughtText.isEmpty()) {
            return ThoughtResult.error("思考内容为空");
        }

        String thought = "";
        Action action = null;

        // 解析思考部分
        if (thoughtText.contains("思考：")) {
            int start = thoughtText.indexOf("思考：") + 3;
            int end = thoughtText.indexOf("\n", start);
            if (end == -1) end = thoughtText.length();
            thought = thoughtText.substring(start, end).trim();
        }

        // 解析行动部分
        if (thoughtText.contains("行动：")) {
            int start = thoughtText.indexOf("行动：") + 3;
            int end = thoughtText.indexOf("\n", start);
            if (end == -1) end = thoughtText.length();
            String actionText = thoughtText.substring(start, end).trim();

            action = parseAction(actionText);
        }

        if (thought.isEmpty() && action == null) {
            // 如果没有明确格式，将整个文本作为思考
            thought = thoughtText;
            action = Action.finish(thought);
        }

        return ThoughtResult.success(thought, action);
    }

    /**
     * 解析Action文本
     */
    private Action parseAction(String actionText) {
        actionText = actionText.trim();

        // 检查是否是完成行动
        if (actionText.startsWith("完成(") || actionText.startsWith("finish(")) {
            String answer = extractParam(actionText, "answer");
            if (answer == null) {
                // 尝试直接提取括号内容
                int start = actionText.indexOf('(') + 1;
                int end = actionText.lastIndexOf(')');
                if (end > start) {
                    answer = actionText.substring(start, end).trim();
                }
            }
            return Action.finish(answer != null ? answer : actionText);
        }

        // 解析工具调用
        int parenIndex = actionText.indexOf('(');
        if (parenIndex > 0) {
            String toolName = actionText.substring(0, parenIndex).trim();
            java.util.Map<String, Object> params = new java.util.HashMap<>();

            // 解析参数
            int endParen = actionText.lastIndexOf(')');
            if (endParen > parenIndex) {
                String paramsText = actionText.substring(parenIndex + 1, endParen);
                String[] pairs = paramsText.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim();
                        String value = kv[1].trim();
                        // 移除引号
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        params.put(key, value);
                    }
                }
            }

            return Action.toolCall(toolName, params, "");
        }

        // 默认作为完成
        return Action.finish(actionText);
    }

    /**
     * 提取参数值
     */
    private String extractParam(String text, String paramName) {
        String pattern = paramName + "=";
        int index = text.indexOf(pattern);
        if (index >= 0) {
            int start = index + pattern.length();
            // 跳过引号
            if (text.charAt(start) == '"') {
                start++;
                int end = text.indexOf('"', start);
                if (end > start) {
                    return text.substring(start, end);
                }
            } else {
                int end = text.indexOf(',', start);
                if (end == -1) end = text.indexOf(')', start);
                if (end > start) {
                    return text.substring(start, end).trim();
                }
            }
        }
        return null;
    }

    /**
     * 思考结果
     */
    public static class ThoughtResult {
        private final boolean success;
        private final String thought;
        private final Action action;
        private final String error;

        private ThoughtResult(boolean success, String thought, Action action, String error) {
            this.success = success;
            this.thought = thought;
            this.action = action;
            this.error = error;
        }

        public static ThoughtResult success(String thought, Action action) {
            return new ThoughtResult(true, thought, action, null);
        }

        public static ThoughtResult error(String error) {
            return new ThoughtResult(false, null, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getThought() {
            return thought;
        }

        public Action getAction() {
            return action;
        }

        public String getError() {
            return error;
        }

        public boolean isFinished() {
            return action != null && action.isFinalAction();
        }
    }
}

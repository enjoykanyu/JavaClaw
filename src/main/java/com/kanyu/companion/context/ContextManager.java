package com.kanyu.companion.context;

import com.kanyu.companion.model.Memory;
import com.kanyu.companion.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 上下文管理器
 *
 * 技术点：
 * 1. 整合提示词模板引擎、Token管理器、上下文选择器
 * 2. 智能上下文组装（系统提示词 + 记忆 + 历史消息 + 用户输入）
 * 3. 动态预算分配（根据内容长度动态调整各部分预算）
 * 4. 记忆相关性排序（基于关键词匹配）
 * 5. 上下文压缩和截断
 *
 * 优化原因：
 * - 需要一个统一的入口管理所有上下文相关操作
 * - 各部分预算需要动态调整，不能固定比例
 * - 记忆注入需要考虑相关性，不是所有记忆都注入
 * - 需要监控和报告上下文使用情况
 *
 * 工作流程：
 * 1. 接收用户输入和对话历史
 * 2. 计算Token预算
 * 3. 生成系统提示词
 * 4. 选择和压缩记忆
 * 5. 选择相关历史消息
 * 6. 组装最终上下文
 * 7. 返回上下文和监控报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextManager {

    private final PromptTemplateEngine templateEngine;
    private final TokenManager tokenManager;
    private final ContextSelector contextSelector;
    private final MemoryService memoryService;

    // 默认配置
    private static final int DEFAULT_MAX_CONTEXT_TOKENS = 6000;  // 留给上下文的Token数
    private static final int MAX_MEMORY_COUNT = 10;              // 最大记忆数量
    private static final int RECENT_ROUNDS = 3;                  // 保留的最近轮数

    /**
     * 上下文构建结果
     */
    public record ContextResult(
        List<Message> messages,           // 组装好的消息列表
        int totalTokens,                  // 总Token数
        ContextReport report              // 上下文报告
    ) {}

    /**
     * 上下文报告
     */
    public record ContextReport(
        int systemPromptTokens,           // 系统提示词Token数
        int memoryTokens,                 // 记忆Token数
        int historyTokens,                // 历史消息Token数
        int userInputTokens,              // 用户输入Token数
        int totalTokens,                  // 总Token数
        int maxTokens,                    // 最大Token数
        int memoryCount,                  // 记忆数量
        int historyMessageCount,          // 历史消息数量
        boolean compressed,               // 是否被压缩
        List<String> optimizations        // 应用的优化措施
    ) {
        public double getUsagePercentage() {
            return maxTokens > 0 ? (double) totalTokens / maxTokens * 100 : 0;
        }

        public boolean isWithinBudget() {
            return totalTokens <= maxTokens;
        }
    }

    /**
     * 构建对话上下文
     *
     * @param userId 用户ID
     * @param userInput 用户输入
     * @param conversationHistory 对话历史
     * @param systemPromptTemplate 系统提示词模板
     * @param templateVariables 模板变量
     * @return 上下文结果
     */
    public ContextResult buildContext(
            Long userId,
            String userInput,
            List<Message> conversationHistory,
            String systemPromptTemplate,
            Map<String, Object> templateVariables) {

        List<String> optimizations = new ArrayList<>();

        // 1. 计算Token预算
        TokenManager.TokenBudget budget = tokenManager.createBudget(DEFAULT_MAX_CONTEXT_TOKENS);

        // 2. 生成系统提示词
        String systemPrompt = generateSystemPrompt(systemPromptTemplate, templateVariables);
        int systemPromptTokens = tokenManager.estimateTokens(systemPrompt);

        // 如果系统提示词超出预算，进行压缩
        if (systemPromptTokens > budget.systemPromptBudget()) {
            systemPrompt = tokenManager.compressText(systemPrompt, budget.systemPromptBudget());
            systemPromptTokens = tokenManager.estimateTokens(systemPrompt);
            optimizations.add("系统提示词压缩");
        }

        // 3. 获取和选择记忆
        List<Memory> relevantMemories = selectRelevantMemories(userId, userInput);
        String memoryContext = buildMemoryContext(relevantMemories);
        int memoryTokens = tokenManager.estimateTokens(memoryContext);

        // 如果记忆超出预算，进行截断
        if (memoryTokens > budget.memoryBudget()) {
            memoryContext = tokenManager.truncateToTokens(memoryContext, budget.memoryBudget(), true);
            memoryTokens = tokenManager.estimateTokens(memoryContext);
            optimizations.add("记忆截断");
        }

        // 4. 计算历史消息预算
        int remainingTokens = DEFAULT_MAX_CONTEXT_TOKENS - systemPromptTokens - memoryTokens
            - tokenManager.estimateTokens(userInput);
        int historyBudget = Math.max(0, remainingTokens);

        // 5. 选择相关历史消息
        List<Message> selectedHistory = selectHistoryMessages(
            conversationHistory, userInput, historyBudget);
        int historyTokens = selectedHistory.stream()
            .mapToInt(m -> tokenManager.estimateTokens(m.getContent()))
            .sum();

        if (selectedHistory.size() < conversationHistory.size()) {
            optimizations.add("历史消息选择（" + selectedHistory.size() + "/" + conversationHistory.size() + "）");
        }

        // 6. 组装消息列表
        List<Message> messages = new ArrayList<>();

        // 系统提示词
        if (!systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 记忆上下文（作为系统消息）
        if (!memoryContext.isEmpty()) {
            messages.add(new SystemMessage(memoryContext));
        }

        // 历史消息
        messages.addAll(selectedHistory);

        // 7. 计算总Token数
        int totalTokens = systemPromptTokens + memoryTokens + historyTokens
            + tokenManager.estimateTokens(userInput);

        // 8. 生成报告
        ContextReport report = new ContextReport(
            systemPromptTokens,
            memoryTokens,
            historyTokens,
            tokenManager.estimateTokens(userInput),
            totalTokens,
            DEFAULT_MAX_CONTEXT_TOKENS,
            relevantMemories.size(),
            selectedHistory.size(),
            !optimizations.isEmpty(),
            optimizations
        );

        log.debug("Context built: {} tokens, {} optimizations applied",
            totalTokens, optimizations.size());

        return new ContextResult(messages, totalTokens, report);
    }

    /**
     * 生成系统提示词
     */
    private String generateSystemPrompt(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        // 验证模板
        PromptTemplateEngine.ValidationResult validation = templateEngine.validate(template);
        if (!validation.valid()) {
            log.warn("Template validation failed: {}", validation.errors());
        }

        return templateEngine.render(template, variables != null ? variables : new HashMap<>());
    }

    /**
     * 选择相关记忆
     *
     * 策略：
     * 1. 获取用户的重要记忆
     * 2. 根据与查询的相关性排序
     * 3. 选择最相关的N条
     */
    private List<Memory> selectRelevantMemories(Long userId, String query) {
        // 获取用户的长期记忆和重要记忆
        List<Memory> allMemories = new ArrayList<>();
        allMemories.addAll(memoryService.retrieveRelevantMemories(userId, query, 10));

        // 根据相关性排序
        allMemories.sort((m1, m2) -> {
            double score1 = calculateMemoryRelevance(m1, query);
            double score2 = calculateMemoryRelevance(m2, query);
            return Double.compare(score2, score1); // 降序
        });

        // 返回最相关的N条
        return allMemories.subList(0, Math.min(MAX_MEMORY_COUNT, allMemories.size()));
    }

    /**
     * 计算记忆与查询的相关性
     */
    private double calculateMemoryRelevance(Memory memory, String query) {
        if (memory == null || query == null) {
            return 0.0;
        }

        double score = 0.0;

        // 1. 内容匹配
        String content = memory.getContent();
        if (content != null && !content.isEmpty()) {
            Set<String> queryKeywords = extractKeywords(query);
            Set<String> contentKeywords = extractKeywords(content);

            int matchCount = 0;
            for (String keyword : queryKeywords) {
                if (contentKeywords.contains(keyword)) {
                    matchCount++;
                }
            }

            if (!queryKeywords.isEmpty()) {
                score += (double) matchCount / queryKeywords.size() * 0.5;
            }
        }

        // 2. 重要性加权
        if (memory.getImportance() != null) {
            score += memory.getImportance() * 0.3;
        }

        // 3. 记忆类型加权
        if (memory.getType() != null) {
            score += switch (memory.getType()) {
                case IMPORTANT_EVENT -> 0.2;
                case PREFERENCE -> 0.15;
                case LONG_TERM -> 0.1;
                default -> 0.05;
            };
        }

        return score;
    }

    /**
     * 构建记忆上下文文本
     */
    private String buildMemoryContext(List<Memory> memories) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【用户记忆】\n");

        for (int i = 0; i < memories.size(); i++) {
            Memory memory = memories.get(i);
            sb.append(i + 1).append(". ").append(memory.getContent());

            if (memory.getType() != null) {
                sb.append(" (").append(memory.getType().name().toLowerCase()).append(")");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 选择历史消息
     *
     * 使用滑动窗口 + 相关性选择的组合策略
     */
    private List<Message> selectHistoryMessages(
            List<Message> history,
            String query,
            int budget) {

        if (history == null || history.isEmpty() || budget <= 0) {
            return new ArrayList<>();
        }

        // 使用滑动窗口选择
        return contextSelector.selectWithSlidingWindow(
            history, query, budget, RECENT_ROUNDS, tokenManager);
    }

    /**
     * 提取关键词
     */
    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();

        if (text == null || text.isEmpty()) {
            return keywords;
        }

        // 移除标点符号
        String cleaned = text.replaceAll("[^\\w\\s\\u4e00-\\u9fa5]", " ");
        String[] words = cleaned.split("\\s+");

        for (String word : words) {
            if (word.length() >= 2) {
                keywords.add(word.toLowerCase());
            }
        }

        return keywords;
    }

    /**
     * 快速构建上下文（使用默认配置）
     */
    public ContextResult buildContextQuick(
            Long userId,
            String userInput,
            List<Message> conversationHistory,
            String systemPrompt) {

        return buildContext(
            userId,
            userInput,
            conversationHistory,
            systemPrompt,
            new HashMap<>()
        );
    }

    /**
     * 获取Token使用报告
     */
    public TokenManager.TokenUsageReport getTokenUsageReport(
            String systemPrompt,
            List<Memory> memories,
            List<Message> history,
            String userInput) {

        Map<String, String> parts = new HashMap<>();
        parts.put("systemPrompt", systemPrompt);
        parts.put("memories", buildMemoryContext(memories));
        parts.put("history", history.stream()
            .map(Message::getContent)
            .reduce((a, b) -> a + "\n" + b)
            .orElse(""));
        parts.put("userInput", userInput);

        return tokenManager.generateUsageReport(parts, tokenManager.createDefaultBudget());
    }
}

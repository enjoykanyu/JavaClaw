package com.kanyu.companion.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Token管理器
 *
 * 技术点：
 * 1. 使用近似算法估算Token数量（基于字符数和语言类型）
 * 2. 支持中英文不同Token密度（中文约1Token/字符，英文约0.25Token/字符）
 * 3. 上下文预算分配（系统提示词、历史消息、记忆、用户输入）
 * 4. 智能截断策略（保留重要信息，截断次要信息）
 *
 * 优化原因：
 * - LLM有Token限制（如GPT-4是8K/32K/128K），需要精确管理
 * - 超出Token限制会导致请求失败或额外费用
 * - 合理分配Token预算可以最大化利用上下文窗口
 * - 不同语言的Token密度不同，需要分别计算
 *
 * 计算公式：
 * - 中文：Token数 ≈ 字符数 × 1.0
 * - 英文：Token数 ≈ 字符数 × 0.25
 * - 混合：根据字符类型分别计算后求和
 */
@Slf4j
@Component
public class TokenManager {

    // Token密度系数
    private static final double CHINESE_TOKEN_RATIO = 1.0;  // 中文每个字符约1个Token
    private static final double ENGLISH_TOKEN_RATIO = 0.25; // 英文每个字符约0.25个Token
    private static final double DEFAULT_TOKEN_RATIO = 0.5;  // 默认系数

    // 默认Token预算配置
    private static final int DEFAULT_MAX_TOKENS = 8000;           // 默认最大Token数
    private static final double SYSTEM_PROMPT_RATIO = 0.15;       // 系统提示词占比
    private static final double HISTORY_RATIO = 0.50;             // 历史消息占比
    private static final double MEMORY_RATIO = 0.20;              // 记忆占比
    private static final double USER_INPUT_RATIO = 0.15;          // 用户输入占比

    // 预留Token（用于模型生成回复）
    private static final int RESERVED_TOKENS = 1000;

    /**
     * Token预算配置
     */
    public record TokenBudget(
        int maxTokens,           // 最大Token数
        int systemPromptBudget,  // 系统提示词预算
        int historyBudget,       // 历史消息预算
        int memoryBudget,        // 记忆预算
        int userInputBudget      // 用户输入预算
    ) {
        public int getTotalBudget() {
            return systemPromptBudget + historyBudget + memoryBudget + userInputBudget;
        }
    }

    /**
     * 计算文本的Token数
     *
     * @param text 文本
     * @return 估算的Token数
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseChars = 0;
        int englishChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                chineseChars++;
            } else if (isEnglish(c)) {
                englishChars++;
            } else {
                otherChars++;
            }
        }

        double tokens = chineseChars * CHINESE_TOKEN_RATIO +
                       englishChars * ENGLISH_TOKEN_RATIO +
                       otherChars * DEFAULT_TOKEN_RATIO;

        return (int) Math.ceil(tokens);
    }

    /**
     * 批量计算Token数
     */
    public Map<String, Integer> estimateTokensBatch(Map<String, String> texts) {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, String> entry : texts.entrySet()) {
            result.put(entry.getKey(), estimateTokens(entry.getValue()));
        }
        return result;
    }

    /**
     * 创建默认Token预算
     *
     * @return Token预算
     */
    public TokenBudget createDefaultBudget() {
        return createBudget(DEFAULT_MAX_TOKENS);
    }

    /**
     * 创建Token预算
     *
     * @param maxTokens 最大Token数
     * @return Token预算
     */
    public TokenBudget createBudget(int maxTokens) {
        int availableTokens = maxTokens - RESERVED_TOKENS;

        return new TokenBudget(
            maxTokens,
            (int) (availableTokens * SYSTEM_PROMPT_RATIO),
            (int) (availableTokens * HISTORY_RATIO),
            (int) (availableTokens * MEMORY_RATIO),
            (int) (availableTokens * USER_INPUT_RATIO)
        );
    }

    /**
     * 创建自定义Token预算
     *
     * @param maxTokens 最大Token数
     * @param ratios 各部分的占比 [systemPrompt, history, memory, userInput]
     * @return Token预算
     */
    public TokenBudget createCustomBudget(int maxTokens, double[] ratios) {
        if (ratios == null || ratios.length != 4) {
            throw new IllegalArgumentException("Ratios must be an array of 4 doubles");
        }

        double totalRatio = Arrays.stream(ratios).sum();
        if (Math.abs(totalRatio - 1.0) > 0.01) {
            throw new IllegalArgumentException("Ratios must sum to 1.0");
        }

        int availableTokens = maxTokens - RESERVED_TOKENS;

        return new TokenBudget(
            maxTokens,
            (int) (availableTokens * ratios[0]),
            (int) (availableTokens * ratios[1]),
            (int) (availableTokens * ratios[2]),
            (int) (availableTokens * ratios[3])
        );
    }

    /**
     * 截断文本到指定Token数
     *
     * 策略：
     * 1. 优先截断前面的内容（保留最近的信息）
     * 2. 在句子边界处截断（避免截断单词）
     * 3. 添加截断提示
     *
     * @param text 原文本
     * @param maxTokens 最大Token数
     * @param fromEnd 是否从末尾开始保留（true=保留末尾，false=保留开头）
     * @return 截断后的文本
     */
    public String truncateToTokens(String text, int maxTokens, boolean fromEnd) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        int currentTokens = estimateTokens(text);
        if (currentTokens <= maxTokens) {
            return text;
        }

        // 需要截断
        String truncated;
        if (fromEnd) {
            // 从末尾保留（保留最近的内容）
            truncated = truncateFromEnd(text, maxTokens);
        } else {
            // 从开头保留（保留最早的内容）
            truncated = truncateFromStart(text, maxTokens);
        }

        return truncated;
    }

    /**
     * 从末尾截断（保留开头）
     */
    private String truncateFromStart(String text, int maxTokens) {
        // 估算需要保留的字符数
        int estimatedChars = (int) (maxTokens / DEFAULT_TOKEN_RATIO) - 20; // 预留空间给截断提示

        if (estimatedChars >= text.length()) {
            return text;
        }

        // 在句子边界截断
        int cutIndex = findSentenceBoundary(text, estimatedChars, false);
        String truncated = text.substring(0, cutIndex);

        return truncated + "\n...[内容已截断]";
    }

    /**
     * 从开头截断（保留末尾）
     */
    private String truncateFromEnd(String text, int maxTokens) {
        // 估算需要保留的字符数
        int estimatedChars = (int) (maxTokens / DEFAULT_TOKEN_RATIO) - 20; // 预留空间给截断提示

        if (estimatedChars >= text.length()) {
            return text;
        }

        // 在句子边界截断
        int startIndex = findSentenceBoundary(text, text.length() - estimatedChars, true);
        String truncated = text.substring(startIndex);

        return "...[前面内容已截断]\n" + truncated;
    }

    /**
     * 查找句子边界
     *
     * @param text 文本
     * @param targetIndex 目标位置
     * @param forward 是否向前查找（true=向后找，false=向前找）
     * @return 句子边界位置
     */
    private int findSentenceBoundary(String text, int targetIndex, boolean forward) {
        char[] sentenceEndings = {'。', '！', '？', '.', '!', '?', '\n'};

        if (forward) {
            // 向后查找（找下一个句子结束符）
            for (int i = targetIndex; i < text.length(); i++) {
                for (char ending : sentenceEndings) {
                    if (text.charAt(i) == ending) {
                        return i + 1;
                    }
                }
            }
            return targetIndex;
        } else {
            // 向前查找（找前一个句子结束符）
            for (int i = targetIndex; i > 0; i--) {
                for (char ending : sentenceEndings) {
                    if (text.charAt(i) == ending) {
                        return i + 1;
                    }
                }
            }
            return targetIndex;
        }
    }

    /**
     * 智能压缩文本
     *
     * 策略：
     * 1. 移除冗余空格和换行
     * 2. 合并重复标点
     * 3. 简化长列表（保留前N项，其余用"等"代替）
     *
     * @param text 原文本
     * @param targetTokens 目标Token数
     * @return 压缩后的文本
     */
    public String compressText(String text, int targetTokens) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String compressed = text;

        // 1. 移除多余空格
        compressed = compressed.replaceAll("[ \\t]+", " ");
        compressed = compressed.replaceAll("\\n\\n+", "\n");

        // 2. 合并重复标点
        compressed = compressed.replaceAll("([。！？.!?])+", "$1");

        // 3. 如果仍然超出限制，进行截断
        int currentTokens = estimateTokens(compressed);
        if (currentTokens > targetTokens) {
            compressed = truncateToTokens(compressed, targetTokens, true);
        }

        return compressed;
    }

    /**
     * 检查字符是否为中文
     */
    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
            || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    /**
     * 检查字符是否为英文
     */
    private boolean isEnglish(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * 获取Token使用报告
     */
    public TokenUsageReport generateUsageReport(Map<String, String> contextParts, TokenBudget budget) {
        Map<String, Integer> usage = new HashMap<>();
        int totalUsed = 0;

        for (Map.Entry<String, String> entry : contextParts.entrySet()) {
            int tokens = estimateTokens(entry.getValue());
            usage.put(entry.getKey(), tokens);
            totalUsed += tokens;
        }

        return new TokenUsageReport(
            budget.maxTokens(),
            totalUsed,
            budget.maxTokens() - totalUsed,
            usage,
            totalUsed > budget.maxTokens()
        );
    }

    /**
     * Token使用报告
     */
    public record TokenUsageReport(
        int maxTokens,
        int usedTokens,
        int remainingTokens,
        Map<String, Integer> usageByPart,
        boolean exceeded
    ) {
        public double getUsagePercentage() {
            return maxTokens > 0 ? (double) usedTokens / maxTokens * 100 : 0;
        }
    }
}

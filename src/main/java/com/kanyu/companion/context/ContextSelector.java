package com.kanyu.companion.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 上下文选择器
 *
 * 技术点：
 * 1. 基于关键词匹配计算相关性
 * 2. 基于时间衰减（越新的消息越重要）
 * 3. 基于消息角色（用户消息比AI消息更重要）
 * 4. 基于消息长度（太短的消息可能不重要）
 * 5. 滑动窗口机制（保留最近N轮对话）
 *
 * 优化原因：
 * - 简单取最近N条消息会包含很多不相关的信息
 * - LLM的上下文窗口有限，需要选择最相关的信息
 * - 相关性选择可以提高回答质量
 * - 时间衰减保证对话的连贯性
 *
 * 相关性计算公式：
 * relevance = keywordScore * 0.4 + timeScore * 0.3 + roleScore * 0.2 + lengthScore * 0.1
 */
@Slf4j
@Component
public class ContextSelector {

    // 权重配置
    private static final double KEYWORD_WEIGHT = 0.4;
    private static final double TIME_WEIGHT = 0.3;
    private static final double ROLE_WEIGHT = 0.2;
    private static final double LENGTH_WEIGHT = 0.1;

    // 时间衰减系数（越新分数越高）
    private static final double TIME_DECAY_FACTOR = 0.95;

    // 最小消息长度（少于这个长度可能不重要）
    private static final int MIN_MESSAGE_LENGTH = 5;

    // 最大消息长度（太长的消息可能包含噪音）
    private static final int MAX_MESSAGE_LENGTH = 500;

    /**
     * 消息相关性评分
     */
    public record MessageScore(
        Message message,
        double keywordScore,
        double timeScore,
        double roleScore,
        double lengthScore,
        double totalScore,
        int index
    ) implements Comparable<MessageScore> {
        @Override
        public int compareTo(MessageScore other) {
            return Double.compare(other.totalScore, this.totalScore); // 降序
        }
    }

    /**
     * 选择最相关的上下文消息
     *
     * @param messages 所有消息
     * @param query 用户查询
     * @param maxTokens 最大Token预算
     * @param tokenManager Token管理器
     * @return 选择后的消息列表（按时间排序）
     */
    public List<Message> selectRelevantContext(
            List<Message> messages,
            String query,
            int maxTokens,
            TokenManager tokenManager) {

        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 计算每条消息的相关性分数
        List<MessageScore> scoredMessages = scoreMessages(messages, query);

        // 2. 按分数排序
        scoredMessages.sort(Comparator.naturalOrder());

        // 3. 根据Token预算选择消息
        List<Message> selected = new ArrayList<>();
        int usedTokens = 0;

        for (MessageScore score : scoredMessages) {
            int messageTokens = tokenManager.estimateTokens(score.message.getContent());

            if (usedTokens + messageTokens <= maxTokens) {
                selected.add(score.message);
                usedTokens += messageTokens;
            } else {
                break;
            }
        }

        // 4. 按原始顺序排序（保持对话连贯性）
        selected.sort(Comparator.comparingInt(m -> messages.indexOf(m)));

        log.debug("Selected {} messages out of {} (used {} tokens)",
            selected.size(), messages.size(), usedTokens);

        return selected;
    }

    /**
     * 使用滑动窗口选择上下文
     *
     * 策略：
     * 1. 始终保留最近N轮对话（确保上下文连贯）
     * 2. 在剩余预算内选择最相关的历史消息
     *
     * @param messages 所有消息
     * @param query 用户查询
     * @param maxTokens 最大Token预算
     * @param recentRounds 保留的最近轮数
     * @param tokenManager Token管理器
     * @return 选择后的消息列表
     */
    public List<Message> selectWithSlidingWindow(
            List<Message> messages,
            String query,
            int maxTokens,
            int recentRounds,
            TokenManager tokenManager) {

        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> selected = new ArrayList<>();
        int usedTokens = 0;

        // 1. 保留最近N轮对话（每轮包含用户消息和AI回复）
        int recentMessagesCount = Math.min(recentRounds * 2, messages.size());
        List<Message> recentMessages = messages.subList(
            messages.size() - recentMessagesCount,
            messages.size()
        );

        for (Message msg : recentMessages) {
            int tokens = tokenManager.estimateTokens(msg.getContent());
            if (usedTokens + tokens <= maxTokens) {
                selected.add(msg);
                usedTokens += tokens;
            }
        }

        // 2. 计算剩余预算
        int remainingTokens = maxTokens - usedTokens;

        if (remainingTokens > 0 && messages.size() > recentMessagesCount) {
            // 3. 在历史消息中选择最相关的
            List<Message> historicalMessages = messages.subList(0, messages.size() - recentMessagesCount);
            List<Message> relevantHistorical = selectRelevantContext(
                historicalMessages, query, remainingTokens, tokenManager
            );

            selected.addAll(0, relevantHistorical);
        }

        log.debug("Sliding window selected {} messages ({} recent + {} relevant historical)",
            selected.size(), recentMessagesCount / 2, selected.size() - recentMessagesCount);

        return selected;
    }

    /**
     * 计算所有消息的相关性分数
     */
    private List<MessageScore> scoreMessages(List<Message> messages, String query) {
        List<MessageScore> scores = new ArrayList<>();

        // 提取查询关键词
        Set<String> queryKeywords = extractKeywords(query);

        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            String content = message.getContent();

            // 计算各项分数
            double keywordScore = calculateKeywordScore(content, queryKeywords);
            double timeScore = calculateTimeScore(i, messages.size());
            double roleScore = calculateRoleScore(message);
            double lengthScore = calculateLengthScore(content);

            // 计算总分
            double totalScore = keywordScore * KEYWORD_WEIGHT +
                               timeScore * TIME_WEIGHT +
                               roleScore * ROLE_WEIGHT +
                               lengthScore * LENGTH_WEIGHT;

            scores.add(new MessageScore(
                message, keywordScore, timeScore, roleScore, lengthScore, totalScore, i
            ));
        }

        return scores;
    }

    /**
     * 计算关键词匹配分数
     *
     * 基于：查询关键词在消息中出现的频率
     */
    private double calculateKeywordScore(String content, Set<String> queryKeywords) {
        if (queryKeywords.isEmpty() || content == null || content.isEmpty()) {
            return 0.5; // 默认中等分数
        }

        String lowerContent = content.toLowerCase();
        int matchCount = 0;

        for (String keyword : queryKeywords) {
            if (lowerContent.contains(keyword.toLowerCase())) {
                matchCount++;
            }
        }

        // 归一化到0-1
        return Math.min(1.0, (double) matchCount / queryKeywords.size());
    }

    /**
     * 计算时间分数
     *
     * 基于：消息位置（越新分数越高）
     * 使用指数衰减：score = decay^((total - index - 1) / 5)
     */
    private double calculateTimeScore(int index, int total) {
        int distance = total - index - 1; // 距离最新消息的距离
        return Math.pow(TIME_DECAY_FACTOR, distance / 5.0);
    }

    /**
     * 计算角色分数
     *
     * 基于：用户消息比AI消息更重要
     */
    private double calculateRoleScore(Message message) {
        String role = message.getMessageType().getValue();
        return switch (role.toLowerCase()) {
            case "user" -> 1.0;
            case "system" -> 0.8;
            case "assistant" -> 0.6;
            default -> 0.5;
        };
    }

    /**
     * 计算长度分数
     *
     * 基于：消息长度在合理范围内
     * 太短可能不重要，太长可能包含噪音
     */
    private double calculateLengthScore(String content) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }

        int length = content.length();

        if (length < MIN_MESSAGE_LENGTH) {
            return 0.3; // 太短
        } else if (length > MAX_MESSAGE_LENGTH) {
            return 0.7; // 太长
        } else {
            // 理想长度，归一化到0.8-1.0
            return 0.8 + 0.2 * (length - MIN_MESSAGE_LENGTH) /
                   (MAX_MESSAGE_LENGTH - MIN_MESSAGE_LENGTH);
        }
    }

    /**
     * 提取关键词
     *
     * 简单实现：提取长度大于2的中文词和长度大于3的英文词
     */
    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();

        if (text == null || text.isEmpty()) {
            return keywords;
        }

        // 移除标点符号
        String cleaned = text.replaceAll("[^\\w\\s\\u4e00-\\u9fa5]", " ");

        // 分词（简单实现）
        String[] words = cleaned.split("\\s+");

        for (String word : words) {
            if (word.length() >= 2 && containsChinese(word)) {
                keywords.add(word);
            } else if (word.length() >= 4 && isEnglishWord(word)) {
                keywords.add(word.toLowerCase());
            }
        }

        return keywords;
    }

    /**
     * 检查是否包含中文
     */
    private boolean containsChinese(String text) {
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为英文单词（简单判断）
     */
    private boolean isEnglishWord(String text) {
        return text.matches("[a-zA-Z]+");
    }

    /**
     * 获取选择报告
     */
    public SelectionReport generateSelectionReport(
            List<Message> original,
            List<Message> selected,
            String query) {

        List<MessageScore> allScores = scoreMessages(original, query);

        // 计算平均分
        double avgScore = allScores.stream()
            .mapToDouble(MessageScore::totalScore)
            .average()
            .orElse(0.0);

        // 计算选中消息的平均分
        double selectedAvgScore = selected.stream()
            .mapToDouble(msg -> allScores.stream()
                .filter(s -> s.message == msg)
                .findFirst()
                .map(MessageScore::totalScore)
                .orElse(0.0))
            .average()
            .orElse(0.0);

        return new SelectionReport(
            original.size(),
            selected.size(),
            avgScore,
            selectedAvgScore,
            (double) selected.size() / original.size() * 100
        );
    }

    /**
     * 选择报告
     */
    public record SelectionReport(
        int originalCount,
        int selectedCount,
        double averageScore,
        double selectedAverageScore,
        double selectionRate
    ) {
        public double getQualityImprovement() {
            return averageScore > 0 ?
                (selectedAverageScore - averageScore) / averageScore * 100 : 0;
        }
    }
}

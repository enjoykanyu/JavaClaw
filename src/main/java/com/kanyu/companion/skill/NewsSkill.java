package com.kanyu.companion.skill;

import com.kanyu.companion.service.NewsApiService;
import com.kanyu.graph.state.GraphState;
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
 * 新闻技能
 * 使用 MCP Tool 获取真实新闻数据
 */
@Slf4j
@Component
public class NewsSkill implements Skill {

    private final ChatModel chatModel;
    private final NewsApiService newsApiService;
    private final SkillConfig config;

    // 触发词 - 支持多种表达方式
    private static final String[] NEWS_TRIGGERS = {
        "新闻", "新鲜事", "资讯", "热点", "头条",
        "最近发生了什么", "有什么新闻", "分享个新闻",
        "有趣的新闻", "今天有什么新闻", "最新消息",
        "国内外大事", "时事", "报道"
    };

    // 分类关键词映射
    private static final String[][] CATEGORY_KEYWORDS = {
        {"科技", "tech", "technology", "科技新闻", "互联网", "AI", "人工智能"},
        {"商业", "business", "财经", "经济", "股票", "金融", "市场"},
        {"体育", "sports", "足球", "篮球", "比赛", "奥运", "世界杯"},
        {"娱乐", "entertainment", "明星", "电影", "电视剧", "综艺", "八卦"},
        {"健康", "health", "医疗", "养生", "疾病", "健康资讯"},
        {"科学", "science", "科学发现", "研究", "太空", "宇宙"}
    };

    public NewsSkill(ChatModel chatModel, NewsApiService newsApiService) {
        this.chatModel = chatModel;
        this.newsApiService = newsApiService;
        this.config = SkillConfig.defaultConfig("news");
        config.setPriority(80);
        config.setEnabled(true);
    }

    @Override
    public String getName() {
        return "news";
    }

    @Override
    public String getDescription() {
        return "新闻获取技能，从真实新闻源获取最新资讯并智能呈现";
    }

    @Override
    public boolean canHandle(String input, GraphState state) {
        if (!config.isEnabled()) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        // 检查触发词
        for (String trigger : NEWS_TRIGGERS) {
            if (lowerInput.contains(trigger.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public GraphState execute(GraphState state) {
        Object userId = state.get("userId");
        log.info("Executing NewsSkill for user: {}", userId);

        try {
            String userInput = state.getUserInput();

            // 1. 分析用户意图，提取分类和关键词
            String category = extractCategory(userInput);
            String query = extractQuery(userInput);

            log.info("NewsSkill - Category: {}, Query: {}", category, query);

            // 2. 调用新闻 API 服务获取真实新闻
            String newsContent;
            if (query != null && !query.isEmpty()) {
                newsContent = newsApiService.searchNews(query);
            } else {
                newsContent = newsApiService.getNewsByCategory(category);
            }

            // 3. 使用 AI 润色输出
            String responseText = enhanceNewsOutput(newsContent, userInput);

            // 4. 更新状态
            state.put("skill_response", responseText);
            state.put("skill_used", "news");
            state.put("news_category", category);
            state.put("news_query", query);

            log.debug("NewsSkill completed successfully");

        } catch (Exception e) {
            log.error("NewsSkill execution failed", e);
            state.put("skill_response", "抱歉，获取新闻时出了点问题。你可以直接访问新浪新闻或腾讯新闻查看最新资讯。");
            state.put("skill_used", "news");
            state.put("skill_error", e.getMessage());
        }

        return state;
    }

    /**
     * 提取新闻分类
     */
    private String extractCategory(String input) {
        String lowerInput = input.toLowerCase();

        for (String[] category : CATEGORY_KEYWORDS) {
            String categoryName = category[0];
            for (int i = 1; i < category.length; i++) {
                if (lowerInput.contains(category[i].toLowerCase())) {
                    return categoryName;
                }
            }
        }

        return "general";
    }

    /**
     * 提取搜索关键词
     */
    private String extractQuery(String input) {
        String lowerInput = input.toLowerCase();

        // 如果包含"关于"、"搜索"等词，提取后面的内容
        String[] queryIndicators = {"关于", "搜索", "查找", "查询"};
        for (String indicator : queryIndicators) {
            int index = lowerInput.indexOf(indicator);
            if (index != -1 && index + indicator.length() < input.length()) {
                return input.substring(index + indicator.length()).trim();
            }
        }

        return null;
    }

    /**
     * 使用 AI 润色新闻输出
     */
    private String enhanceNewsOutput(String newsContent, String userInput) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("""
                你是一个专业的新闻播报助手。请根据提供的新闻数据，用友好、口语化的方式呈现给用户。
                
                要求：
                1. 保持新闻的真实性和客观性，不要编造内容
                2. 用轻松自然的语气，像朋友聊天一样
                3. 可以适当使用 emoji 增加亲和力
                4. 如果新闻内容较少或服务不可用，诚实告知用户
                5. 最后可以引导用户继续对话
                """));

            messages.add(new UserMessage(String.format("""
                用户说：%s

                获取到的新闻数据：
                %s

                请用友好自然的方式把这些新闻分享给用户。
                """, userInput, newsContent)));

            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);

            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getContent();
            }

        } catch (Exception e) {
            log.error("Failed to enhance news output", e);
        }

        // 如果 AI 润色失败，直接返回原始新闻
        return newsContent;
    }

    @Override
    public SkillConfig getConfig() {
        return config;
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    @Override
    public String[] getTriggers() {
        return NEWS_TRIGGERS;
    }
}

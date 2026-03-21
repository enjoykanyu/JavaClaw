package com.kanyu.companion.skill;

import com.kanyu.companion.service.NewsService;
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

@Slf4j
@Component
public class NewsSkill implements Skill {

    private final ChatModel chatModel;
    private final NewsService newsService;
    private final SkillConfig config;

    private static final String[] NEWS_TRIGGERS = {
        "新闻", "新鲜事", "资讯", "热点", "头条",
        "最近发生了什么", "有什么新闻", "分享个新闻",
        "有趣的新闻", "今天有什么新闻", "最新消息"
    };

    public NewsSkill(ChatModel chatModel, NewsService newsService) {
        this.chatModel = chatModel;
        this.newsService = newsService;
        this.config = SkillConfig.defaultConfig("news");
        config.setPriority(80);
    }

    @Override
    public String getName() {
        return "news";
    }

    @Override
    public String getDescription() {
        return "新闻分享技能，可以获取最新新闻并以有趣的方式分享给用户";
    }

    @Override
    public boolean canHandle(String input, GraphState state) {
        if (!config.isEnabled()) {
            return false;
        }

        String lowerInput = input.toLowerCase();
        for (String trigger : NEWS_TRIGGERS) {
            if (lowerInput.contains(trigger)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public GraphState execute(GraphState state) {
        log.info("Executing NewsSkill");

        try {
            String userInput = state.getUserInput();

            // 获取最新新闻
            List<NewsService.NewsItem> newsItems = newsService.fetchLatestNews();

            if (newsItems.isEmpty()) {
                state.put("skill_response", "抱歉，暂时没有找到新闻资讯。");
                state.put("skill_used", "news");
                return state;
            }

            // 构建新闻内容
            StringBuilder newsContent = new StringBuilder();
            for (int i = 0; i < Math.min(newsItems.size(), 3); i++) {
                NewsService.NewsItem item = newsItems.get(i);
                newsContent.append(i + 1).append(". ").append(item.title()).append("\n");
                newsContent.append("   ").append(item.summary()).append("\n\n");
            }

            // 使用 AI 生成有趣的新闻分享
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("""
                你是一个有趣的新闻播报员，擅长用轻松幽默的方式分享新闻。
                请根据提供的新闻内容，用口语化、生动有趣的方式分享给用户。
                可以适当加入一些emoji和互动性的语言，让用户感觉亲切自然。
                """));
            messages.add(new UserMessage(String.format("""
                用户说：%s

                以下是今天的新闻：
                %s

                请用有趣的方式分享这些新闻，并自然地引导用户继续聊天。
                """, userInput, newsContent.toString())));

            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);

            String responseText = "";
            if (response != null && response.getResult() != null) {
                responseText = response.getResult().getOutput().getContent();
            }

            state.put("skill_response", responseText);
            state.put("skill_used", "news");

        } catch (Exception e) {
            log.error("NewsSkill execution failed", e);
            state.put("skill_response", "抱歉，获取新闻时出了点小问题，我们聊点别的吧！");
            state.put("skill_used", "news");
        }

        return state;
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

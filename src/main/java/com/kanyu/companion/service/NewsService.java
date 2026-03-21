package com.kanyu.companion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {
    
    private final ChatModel chatModel;
    
    private static final String NEWS_SUMMARY_PROMPT = """
        请总结以下新闻内容，提取关键信息：
        
        %s
        
        请按以下格式输出：
        1. 标题摘要（一句话概括）
        2. 关键要点（3-5条）
        3. 影响分析（简短分析）
        """;
    
    @Scheduled(cron = "${companion.news.push-cron:0 0 * * * ?}")
    public void pushNewsToAllUsers() {
        log.info("Starting scheduled news push");
        
        List<NewsItem> news = fetchLatestNews();
        
        log.info("Pushed {} news items", news.size());
    }
    
    public List<NewsItem> fetchLatestNews() {
        List<NewsItem> news = new ArrayList<>();
        
        news.add(new NewsItem(
            "国内经济持续向好，一季度GDP增长5.3%",
            "国家统计局发布数据显示，一季度国内生产总值同比增长5.3%，经济运行开局良好...",
            "经济",
            "https://example.com/news/1"
        ));
        
        news.add(new NewsItem(
            "科技创新成果显著，多项技术取得突破",
            "我国在人工智能、量子计算、新能源等领域取得重要进展...",
            "科技",
            "https://example.com/news/2"
        ));
        
        news.add(new NewsItem(
            "春季旅游市场火热，出行人数创新高",
            "清明假期期间，全国旅游市场迎来小高峰，出行人数较去年同期增长...",
            "生活",
            "https://example.com/news/3"
        ));
        
        return news;
    }
    
    public String summarizeNews(String newsContent) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的新闻摘要助手，擅长提取新闻关键信息。"));
            messages.add(new UserMessage(String.format(NEWS_SUMMARY_PROMPT, newsContent)));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getContent();
            }
            
        } catch (Exception e) {
            log.error("Failed to summarize news", e);
        }
        
        return newsContent;
    }
    
    public String generateNewsDigest(List<NewsItem> newsItems) {
        StringBuilder digest = new StringBuilder();
        digest.append("📰 今日新闻摘要\n\n");
        
        for (int i = 0; i < newsItems.size(); i++) {
            NewsItem item = newsItems.get(i);
            digest.append(String.format("%d. 【%s】%s\n", 
                i + 1, item.category(), item.title()));
            digest.append(String.format("   %s\n\n", item.summary()));
        }
        
        return digest.toString();
    }
    
    public record NewsItem(
        String title,
        String summary,
        String category,
        String url
    ) {}
}

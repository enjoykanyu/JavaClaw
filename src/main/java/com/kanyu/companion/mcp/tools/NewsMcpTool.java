package com.kanyu.companion.mcp.tools;

import com.kanyu.companion.mcp.McpToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 新闻获取 MCP Tool
 * 接入 NewsAPI 获取真实新闻数据
 */
@Slf4j
@Component
public class NewsMcpTool implements McpToolRegistry.McpTool {

    @Value("${news.api.key:}")
    private String apiKey;

    @Value("${news.api.url:https://newsapi.org/v2}")
    private String apiUrl;

    @Value("${news.api.timeout:10000}")
    private int timeout;

    private final RestTemplate restTemplate;
    private final McpToolRegistry toolRegistry;

    public NewsMcpTool(McpToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        toolRegistry.registerTool(this);
        log.info("NewsMcpTool registered");
    }

    @Override
    public String getName() {
        return "get_news";
    }

    @Override
    public String getDescription() {
        return "获取最新新闻资讯，支持按分类、关键词搜索";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new LinkedHashMap<>();

        Map<String, Object> category = new LinkedHashMap<>();
        category.put("type", "string");
        category.put("description", "新闻分类：business(商业), entertainment(娱乐), general(综合), health(健康), science(科学), sports(体育), technology(科技)");
        category.put("enum", Arrays.asList("business", "entertainment", "general", "health", "science", "sports", "technology"));
        params.put("category", category);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("type", "string");
        query.put("description", "搜索关键词");
        params.put("query", query);

        Map<String, Object> pageSize = new LinkedHashMap<>();
        pageSize.put("type", "integer");
        pageSize.put("description", "返回新闻数量，默认5条，最大20条");
        pageSize.put("default", 5);
        params.put("pageSize", pageSize);

        return params;
    }

    @Override
    public McpToolRegistry.OldMcpToolResult execute(Map<String, Object> parameters) {
        log.info("Executing NewsMcpTool with params: {}", parameters);

        try {
            // 检查 API Key
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("News API key not configured, using fallback data");
                return McpToolRegistry.OldMcpToolResult.success(getFallbackNews());
            }

            // 构建请求参数
            String category = (String) parameters.getOrDefault("category", "general");
            String query = (String) parameters.getOrDefault("query", "");
            int pageSize = (int) parameters.getOrDefault("pageSize", 5);
            pageSize = Math.min(pageSize, 20); // 限制最大20条

            // 构建 URL
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/top-headlines")
                .queryParam("apiKey", apiKey)
                .queryParam("country", "cn")  // 中国新闻
                .queryParam("pageSize", pageSize);

            if (!category.equals("general")) {
                uriBuilder.queryParam("category", category);
            }

            if (query != null && !query.isEmpty()) {
                uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(apiUrl + "/everything")
                    .queryParam("apiKey", apiKey)
                    .queryParam("q", query)
                    .queryParam("language", "zh")
                    .queryParam("sortBy", "publishedAt")
                    .queryParam("pageSize", pageSize);
            }

            // 调用 API
            ResponseEntity<NewsApiResponse> response = restTemplate.getForEntity(
                uriBuilder.toUriString(),
                NewsApiResponse.class
            );

            if (response.getBody() == null || !"ok".equals(response.getBody().getStatus())) {
                log.error("News API returned error: {}", response.getBody());
                return McpToolRegistry.OldMcpToolResult.success(getFallbackNews());
            }

            // 格式化结果
            String formattedNews = formatNews(response.getBody());
            return McpToolRegistry.OldMcpToolResult.success(formattedNews);

        } catch (Exception e) {
            log.error("Failed to fetch news from News API", e);
            return McpToolRegistry.OldMcpToolResult.success(getFallbackNews());
        }
    }

    /**
     * 格式化新闻数据
     */
    private String formatNews(NewsApiResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("📰 最新新闻资讯\n\n");

        List<Article> articles = response.getArticles();
        if (articles == null || articles.isEmpty()) {
            return "暂无最新新闻";
        }

        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            sb.append(String.format("%d. %s\n", i + 1, article.getTitle()));

            if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                sb.append(String.format("   %s\n", article.getDescription()));
            }

            if (article.getPublishedAt() != null) {
                String time = formatTime(article.getPublishedAt());
                sb.append(String.format("   📅 %s", time));
            }

            if (article.getSource() != null && article.getSource().getName() != null) {
                sb.append(String.format(" | 📡 %s", article.getSource().getName()));
            }

            sb.append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 格式化时间
     */
    private String formatTime(String publishedAt) {
        try {
            LocalDateTime time = LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();

            if (time.toLocalDate().equals(now.toLocalDate())) {
                return "今天 " + time.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                return time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
            }
        } catch (Exception e) {
            return publishedAt;
        }
    }

    /**
     * 降级数据 - 当 API 不可用时使用
     */
    private String getFallbackNews() {
        return """
            📰 新闻服务暂时不可用

            抱歉，实时新闻服务暂时无法访问。以下是一些通用资讯：

            1. 建议关注官方新闻网站获取最新资讯
            2. 可以通过搜索引擎查询感兴趣的话题
            3. 各大新闻客户端也提供实时推送服务

            请稍后再试，或告诉我你想了解什么具体话题，我可以帮你分析讨论。
            """;
    }

    // NewsAPI 响应对象
    public static class NewsApiResponse {
        private String status;
        private int totalResults;
        private List<Article> articles;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getTotalResults() { return totalResults; }
        public void setTotalResults(int totalResults) { this.totalResults = totalResults; }
        public List<Article> getArticles() { return articles; }
        public void setArticles(List<Article> articles) { this.articles = articles; }
    }

    public static class Article {
        private Source source;
        private String author;
        private String title;
        private String description;
        private String url;
        private String urlToImage;
        private String publishedAt;
        private String content;

        public Source getSource() { return source; }
        public void setSource(Source source) { this.source = source; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUrlToImage() { return urlToImage; }
        public void setUrlToImage(String urlToImage) { this.urlToImage = urlToImage; }
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class Source {
        private String id;
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}

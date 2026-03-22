package com.kanyu.companion.mcp.tools;

import com.kanyu.companion.mcp.McpToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * GNews MCP Tool
 * 使用 GNews API 获取真实新闻数据（国内可访问）
 * 官网：https://gnews.io/
 */
@Slf4j
public class GNewsMcpTool implements McpToolRegistry.McpTool {

    @Value("${gnews.api.key:}")
    private String apiKey;

    @Value("${gnews.api.url:https://gnews.io/api/v4}")
    private String apiUrl;

    @Value("${gnews.api.timeout:10000}")
    private int timeout;

    private final RestTemplate restTemplate;

    public GNewsMcpTool() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getName() {
        return "get_news";
    }

    @Override
    public String getDescription() {
        return "获取最新新闻资讯，支持按分类、关键词搜索（使用 GNews API）";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new LinkedHashMap<>();

        Map<String, Object> category = new LinkedHashMap<>();
        category.put("type", "string");
        category.put("description", "新闻分类：general(综合), world(国际), nation(国内), business(商业), technology(科技), entertainment(娱乐), sports(体育), science(科学), health(健康)");
        category.put("enum", Arrays.asList("general", "world", "nation", "business", "technology", "entertainment", "sports", "science", "health"));
        params.put("category", category);

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("type", "string");
        query.put("description", "搜索关键词");
        params.put("query", query);

        Map<String, Object> pageSize = new LinkedHashMap<>();
        pageSize.put("type", "integer");
        pageSize.put("description", "返回新闻数量，默认5条，最大10条");
        pageSize.put("default", 5);
        params.put("pageSize", pageSize);

        return params;
    }

    @Override
    public McpToolRegistry.OldMcpToolResult execute(Map<String, Object> parameters) {
        log.info("Executing GNewsMcpTool with params: {}", parameters);

        try {
            // 检查 API Key
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("GNews API key not configured, using fallback data");
                return McpToolRegistry.OldMcpToolResult.success(getFallbackNews());
            }

            // 构建请求参数
            String category = (String) parameters.getOrDefault("category", "general");
            String query = (String) parameters.getOrDefault("query", "");
            int pageSize = (int) parameters.getOrDefault("pageSize", 5);
            pageSize = Math.min(pageSize, 10); // GNews 免费版限制

            String url;
            if (query != null && !query.isEmpty()) {
                // 搜索模式
                url = UriComponentsBuilder
                    .fromHttpUrl(apiUrl + "/search")
                    .queryParam("q", query)
                    .queryParam("lang", "zh")  // 中文
                    .queryParam("country", "cn")  // 中国
                    .queryParam("max", pageSize)
                    .queryParam("apikey", apiKey)
                    .toUriString();
            } else {
                // 分类头条模式
                url = UriComponentsBuilder
                    .fromHttpUrl(apiUrl + "/top-headlines")
                    .queryParam("category", category)
                    .queryParam("lang", "zh")
                    .queryParam("country", "cn")
                    .queryParam("max", pageSize)
                    .queryParam("apikey", apiKey)
                    .toUriString();
            }

            log.debug("Calling GNews API: {}", url.replace(apiKey, "***"));

            // 调用 API
            ResponseEntity<GNewsResponse> response = restTemplate.getForEntity(
                url,
                GNewsResponse.class
            );

            if (response.getBody() == null || response.getBody().getArticles() == null) {
                log.error("GNews API returned empty response");
                return McpToolRegistry.OldMcpToolResult.success(getFallbackNews());
            }

            // 格式化结果
            String formattedNews = formatNews(response.getBody());
            return McpToolRegistry.OldMcpToolResult.success(formattedNews);

        } catch (Exception e) {
            log.error("Failed to fetch news from GNews API", e);
            // 降级到提示信息
            return McpToolRegistry.OldMcpToolResult.success(getFallbackNews());
        }
    }

    /**
     * 格式化新闻数据
     */
    private String formatNews(GNewsResponse response) {
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
                // 截断过长的描述
                String desc = article.getDescription();
                if (desc.length() > 200) {
                    desc = desc.substring(0, 200) + "...";
                }
                sb.append(String.format("   %s\n", desc));
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
            // GNews 时间格式：2024-01-15T08:30:00Z
            LocalDateTime time = LocalDateTime.parse(publishedAt.replace("Z", ""), 
                DateTimeFormatter.ISO_DATE_TIME);
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

            抱歉，实时新闻服务暂时无法访问。可能原因：
            - API 服务异常或网络问题
            - API Key 未配置或已过期
            - 已达到 API 调用限制

            建议：
            1. 检查网络连接
            2. 访问 https://gnews.io/ 获取免费 API Key
            3. 直接访问新闻网站：
               - 新浪新闻: https://news.sina.com.cn
               - 腾讯新闻: https://news.qq.com
               - 网易新闻: https://news.163.com

            你也可以告诉我你想了解什么话题，我可以基于已有知识与你讨论。
            """;
    }

    // GNews API 响应对象
    public static class GNewsResponse {
        private int totalArticles;
        private List<Article> articles;

        public int getTotalArticles() { return totalArticles; }
        public void setTotalArticles(int totalArticles) { this.totalArticles = totalArticles; }
        public List<Article> getArticles() { return articles; }
        public void setArticles(List<Article> articles) { this.articles = articles; }
    }

    public static class Article {
        private String title;
        private String description;
        private String content;
        private String url;
        private String image;
        private String publishedAt;
        private Source source;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        public Source getSource() { return source; }
        public void setSource(Source source) { this.source = source; }
    }

    public static class Source {
        private String name;
        private String url;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}

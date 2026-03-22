package com.kanyu.companion.mcp.tools;

import com.kanyu.companion.mcp.*;
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
 * GNews MCP Tool 执行器
 * 符合Spring AI Alibaba规范的新闻获取Tool
 * 使用 GNews API 获取真实新闻数据
 */
@Slf4j
@Component
public class GNewsToolExecutor implements McpToolExecutor {

    @Value("${gnews.api.key:}")
    private String apiKey;

    @Value("${gnews.api.url:https://gnews.io/api/v4}")
    private String apiUrl;

    @Value("${gnews.api.timeout:10000}")
    private int timeout;

    private final RestTemplate restTemplate;
    private final McpToolRegistry toolRegistry;
    private McpToolDefinition definition;

    public GNewsToolExecutor(McpToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        // 构建参数定义
        Map<String, McpToolParameter> parameters = new HashMap<>();
        parameters.put("category", McpToolParameter.builder()
                .name("category")
                .type("string")
                .description("新闻分类")
                .required(false)
                .defaultValue("general")
                .enumValues(Arrays.asList("general", "world", "nation", "business", "technology", "entertainment", "sports", "science", "health"))
                .build());
        parameters.put("query", McpToolParameter.builder()
                .name("query")
                .type("string")
                .description("搜索关键词")
                .required(false)
                .build());
        parameters.put("pageSize", McpToolParameter.builder()
                .name("pageSize")
                .type("integer")
                .description("返回新闻数量，默认5条，最大10条")
                .required(false)
                .defaultValue(5)
                .build());

        // 构建Tool定义
        this.definition = McpToolDefinition.builder()
                .name("get_news")
                .description("获取最新新闻资讯，支持按分类、关键词搜索")
                .detailedDescription("""
                    使用GNews API获取真实新闻数据，支持多种新闻分类和关键词搜索。
                    
                    功能：
                    1. 按分类获取头条新闻（综合、国际、国内、商业、科技、娱乐、体育、科学、健康）
                    2. 按关键词搜索新闻
                    3. 自定义返回新闻数量
                    
                    注意：需要配置GNews API Key才能使用真实数据
                    """)
                .parameters(parameters)
                .examples("""
                    获取科技新闻：
                    get_news({"category": "technology", "pageSize": 5})
                    
                    搜索人工智能新闻：
                    get_news({"query": "人工智能", "pageSize": 3})
                    """)
                .returnDescription("返回格式化的新闻列表，包含标题、摘要、发布时间、来源")
                .tags(new String[]{"news", "search", "api"})
                .build();

        // 注册到McpToolRegistry
        toolRegistry.registerTool(definition, this);
        log.info("GNewsToolExecutor registered to McpToolRegistry");
    }

    @Override
    public McpToolDefinition getDefinition() {
        return definition;
    }

    @Override
    public McpToolResult execute(Map<String, Object> parameters) {
        log.info("Executing GNewsTool with params: {}", parameters);

        try {
            // 检查 API Key
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("GNews API key not configured, using fallback data");
                return McpToolResult.success(getFallbackNews());
            }

            // 提取参数
            String category = (String) parameters.getOrDefault("category", "general");
            String query = (String) parameters.getOrDefault("query", "");
            int pageSize = parameters.get("pageSize") instanceof Number
                    ? ((Number) parameters.get("pageSize")).intValue()
                    : 5;
            pageSize = Math.min(pageSize, 10); // GNews 免费版限制

            // 构建请求URL
            String url = buildApiUrl(query, category, pageSize);
            log.debug("Calling GNews API: {}", url.replace(apiKey, "***"));

            // 调用 API
            ResponseEntity<GNewsResponse> response = restTemplate.getForEntity(url, GNewsResponse.class);

            if (response.getBody() == null || response.getBody().getArticles() == null) {
                log.error("GNews API returned empty response");
                return McpToolResult.success(getFallbackNews());
            }

            // 格式化结果
            String formattedNews = formatNews(response.getBody());

            Map<String, Object> metadata = Map.of(
                    "category", category,
                    "query", query != null ? query : "",
                    "count", response.getBody().getArticles().size()
            );

            return McpToolResult.success(formattedNews, metadata);

        } catch (Exception e) {
            log.error("Failed to fetch news from GNews API", e);
            return McpToolResult.error("新闻获取失败: " + e.getMessage(), getFallbackNews());
        }
    }

    /**
     * 构建API URL
     */
    private String buildApiUrl(String query, String category, int pageSize) {
        if (query != null && !query.isEmpty()) {
            // 搜索模式
            return UriComponentsBuilder
                    .fromHttpUrl(apiUrl + "/search")
                    .queryParam("q", query)
                    .queryParam("lang", "zh")
                    .queryParam("country", "cn")
                    .queryParam("max", pageSize)
                    .queryParam("apikey", apiKey)
                    .toUriString();
        } else {
            // 分类头条模式
            return UriComponentsBuilder
                    .fromHttpUrl(apiUrl + "/top-headlines")
                    .queryParam("category", category)
                    .queryParam("lang", "zh")
                    .queryParam("country", "cn")
                    .queryParam("max", pageSize)
                    .queryParam("apikey", apiKey)
                    .toUriString();
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
     * 降级数据
     */
    private String getFallbackNews() {
        return """
            📰 新闻服务暂时不可用

            可能的原因：
            1. GNews API Key 未配置
            2. 网络连接问题
            3. API 调用次数已达上限

            请检查配置或稍后重试。
            """;
    }

    // ==================== GNews API 响应类 ====================

    @lombok.Data
    public static class GNewsResponse {
        private int totalArticles;
        private List<Article> articles;
    }

    @lombok.Data
    public static class Article {
        private String title;
        private String description;
        private String content;
        private String url;
        private String image;
        private String publishedAt;
        private Source source;
    }

    @lombok.Data
    public static class Source {
        private String name;
        private String url;
    }
}

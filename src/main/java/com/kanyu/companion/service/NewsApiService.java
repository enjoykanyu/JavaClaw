package com.kanyu.companion.service;

import com.kanyu.companion.mcp.McpToolRegistry;
import com.kanyu.companion.mcp.tools.GNewsMcpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 新闻 API 服务
 * 带缓存和容错机制的企业级实现
 */
@Slf4j
@Service
public class NewsApiService {

    @Value("${gnews.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${gnews.cache.ttl:300}")
    private long cacheTtl;

    @Autowired
    @Lazy
    private GNewsMcpTool gNewsMcpTool;

    private final RedisTemplate<String, String> redisTemplate;

    private static final String CACHE_PREFIX = "news:";

    public NewsApiService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取最新新闻（带缓存）
     */
    public String getLatestNews(String category, String query, int pageSize) {
        String cacheKey = buildCacheKey(category, query, pageSize);

        // 1. 尝试从缓存获取
        if (cacheEnabled) {
            String cachedNews = getFromCache(cacheKey);
            if (cachedNews != null) {
                log.info("Returning cached news for key: {}", cacheKey);
                return cachedNews;
            }
        }

        // 2. 调用 MCP Tool 获取新闻
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("category", category != null ? category : "general");
                params.put("query", query);
                params.put("pageSize", pageSize);

                McpToolRegistry.OldMcpToolResult result = gNewsMcpTool.execute(params);

                if (result.isSuccess() && result.getContent() != null) {
                    String news = result.getContent();

                    // 3. 存入缓存
                    if (cacheEnabled) {
                        putToCache(cacheKey, news);
                    }

                    return news;
                } else {
                    log.error("GNews MCP tool returned error: {}", result.getError());
                    return getFallbackNews();
                }

            } catch (Exception e) {
                log.error("Failed to fetch news", e);
                return getFallbackNews();
            }
    }

    /**
     * 获取热门新闻（简化版）
     */
    public String getTopNews() {
        return getLatestNews("general", null, 5);
    }

    /**
     * 按分类获取新闻
     */
    public String getNewsByCategory(String category) {
        return getLatestNews(category, null, 5);
    }

    /**
     * 搜索新闻
     */
    public String searchNews(String query) {
        return getLatestNews("general", query, 5);
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        if (redisTemplate != null) {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} news cache entries", keys.size());
            }
        }
    }

    /**
     * 从缓存获取
     */
    private String getFromCache(String key) {
        try {
            if (redisTemplate != null) {
                return redisTemplate.opsForValue().get(key);
            }
        } catch (Exception e) {
            log.warn("Failed to get from cache: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 存入缓存
     */
    private void putToCache(String key, String value) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(cacheTtl));
                log.debug("Cached news with key: {}, ttl: {}s", key, cacheTtl);
            }
        } catch (Exception e) {
            log.warn("Failed to put to cache: {}", e.getMessage());
        }
    }

    /**
     * 构建缓存 Key
     */
    private String buildCacheKey(String category, String query, int pageSize) {
        StringBuilder sb = new StringBuilder(CACHE_PREFIX);
        sb.append(category != null ? category : "general");
        if (query != null && !query.isEmpty()) {
            sb.append(":").append(query.hashCode());
        }
        sb.append(":").append(pageSize);
        return sb.toString();
    }

    /**
     * 降级数据
     */
    private String getFallbackNews() {
        return """
            📰 新闻服务暂时不可用

            抱歉，实时新闻服务暂时无法访问。可能原因：
            - 新闻 API 服务异常
            - 网络连接问题
            - API 调用次数已达上限

            建议：
            1. 稍后再试
            2. 直接访问新闻网站获取资讯
            3. 告诉我你想了解什么话题，我可以基于已有知识讨论

            热门新闻网站推荐：
            - 新浪新闻 (news.sina.com.cn)
            - 腾讯新闻 (news.qq.com)
            - 网易新闻 (news.163.com)
            """;
    }
}

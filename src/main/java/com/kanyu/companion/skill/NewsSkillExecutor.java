package com.kanyu.companion.skill;

import com.kanyu.companion.mcp.McpToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新闻Skill执行器
 * 符合Spring AI Alibaba规范的新闻查询技能
 */
@Slf4j
@Component
public class NewsSkillExecutor implements SkillExecutor {

    private final ChatModel chatModel;
    private final SkillRegistry skillRegistry;
    private final McpToolRegistry mcpToolRegistry;
    private SkillDefinition definition;

    public NewsSkillExecutor(ChatModel chatModel, SkillRegistry skillRegistry, McpToolRegistry mcpToolRegistry) {
        this.chatModel = chatModel;
        this.skillRegistry = skillRegistry;
        this.mcpToolRegistry = mcpToolRegistry;
    }

    @PostConstruct
    public void init() {
        // 构建参数
        Map<String, SkillDefinition.SkillParameter> parameters = new HashMap<>();
        parameters.put("query", SkillDefinition.SkillParameter.builder()
                .name("query")
                .type("string")
                .description("新闻查询关键词，如：科技、财经、体育等")
                .required(false)
                .defaultValue("热门新闻")
                .build());
        parameters.put("input", SkillDefinition.SkillParameter.builder()
                .name("input")
                .type("string")
                .description("用户的原始输入，用于提取查询意图")
                .required(true)
                .build());

        // 构建Skill定义
        this.definition = SkillDefinition.builder()
                .name("news")
                .description("新闻查询技能，可以获取最新新闻资讯")
                .detailedDescription("""
                    这是一个新闻查询技能，可以帮助用户获取最新新闻资讯。
                    
                    功能：
                    1. 查询指定类别的新闻
                    2. 搜索特定关键词的新闻
                    3. 获取热门新闻
                    4. 提供新闻摘要和链接
                    
                    支持的新闻类别包括：科技、财经、体育、娱乐、国际、国内等
                    """)
                .parameters(parameters)
                .examples("""
                    用户：今天有什么科技新闻？
                    调用：execute_skill("news", {"query": "科技", "input": "今天有什么科技新闻？"})
                    
                    用户：最新的财经资讯
                    调用：execute_skill("news", {"query": "财经", "input": "最新的财经资讯"})
                    """)
                .returnDescription("返回包含新闻列表、摘要的友好回复文本")
                .build();

        // 注册到SkillRegistry
        skillRegistry.registerSkill(definition, this);
        log.info("NewsSkillExecutor registered to SkillRegistry");
    }

    @Override
    public SkillDefinition getDefinition() {
        return definition;
    }

    @Override
    public SkillResult execute(Map<String, Object> params) {
        log.info("Executing NewsSkill with params: {}", params);

        try {
            // 提取参数
            String userInput = (String) params.getOrDefault("input", "");
            String query = (String) params.get("query");

            // 如果没有提供查询词，从输入中提取
            if (query == null || query.isEmpty()) {
                query = extractQuery(userInput);
            }

            // 获取新闻信息
            String newsInfo = getNewsInfo(query);

            // 构建提示词
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("""
                你是一个新闻助手，需要整理新闻信息给用户友好的回复。
                回复时：
                1. 用简洁清晰的语气
                2. 按重要性排序新闻
                3. 提供新闻摘要
                4. 可以适当评论
                """));
            messages.add(new UserMessage("查询：" + query + "\n新闻信息：" + newsInfo + "\n用户问题：" + userInput));

            // 调用模型生成回复
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);

            String responseText = "";
            if (response != null && response.getResult() != null) {
                responseText = response.getResult().getOutput().getContent();
            }

            // 构建结果
            Map<String, Object> metadata = Map.of(
                    "query", query,
                    "news_info", newsInfo,
                    "skill_name", "news"
            );

            log.info("NewsSkill executed successfully for query: {}", query);
            return SkillResult.success(responseText, metadata);

        } catch (Exception e) {
            log.error("NewsSkill execution failed", e);
            return SkillResult.error("新闻查询失败: " + e.getMessage());
        }
    }

    /**
     * 从输入中提取查询关键词
     */
    private String extractQuery(String input) {
        if (input == null || input.isEmpty()) {
            return "热门新闻";
        }

        String[] categories = {"科技", "财经", "体育", "娱乐", "国际", "国内", "社会", "健康"};

        for (String category : categories) {
            if (input.contains(category)) {
                return category;
            }
        }

        // 默认返回热门新闻
        return "热门新闻";
    }

    /**
     * 获取新闻信息（模拟数据，实际可接入MCP Tool或真实新闻API）
     */
    private String getNewsInfo(String query) {
        // 这里可以调用MCP Tool获取真实新闻
        // 目前返回模拟数据
        return String.format("""
            %s相关新闻：
            1. 人工智能技术在医疗领域取得重大突破
               摘要：最新研究显示AI诊断准确率达到95%以上...
            2. 新能源汽车销量创新高
               摘要：今年前三季度新能源汽车销量同比增长40%...
            3. 全球气候大会达成新协议
               摘要：各国承诺在2030年前减少碳排放30%...
            """, query);
    }
}

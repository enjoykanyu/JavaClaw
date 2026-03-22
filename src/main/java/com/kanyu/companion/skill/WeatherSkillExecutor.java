package com.kanyu.companion.skill;

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
 * 天气Skill执行器
 * 符合Spring AI Alibaba规范的天气查询技能
 */
@Slf4j
@Component
public class WeatherSkillExecutor implements SkillExecutor {

    private final ChatModel chatModel;
    private final SkillRegistry skillRegistry;
    private SkillDefinition definition;

    public WeatherSkillExecutor(ChatModel chatModel, SkillRegistry skillRegistry) {
        this.chatModel = chatModel;
        this.skillRegistry = skillRegistry;
    }

    @PostConstruct
    public void init() {
        // 构建参数
        Map<String, SkillDefinition.SkillParameter> parameters = new HashMap<>();
        parameters.put("city", SkillDefinition.SkillParameter.builder()
                .name("city")
                .type("string")
                .description("城市名称，如：北京、上海、广州等")
                .required(false)
                .defaultValue("北京")
                .build());
        parameters.put("input", SkillDefinition.SkillParameter.builder()
                .name("input")
                .type("string")
                .description("用户的原始输入，用于提取城市和意图")
                .required(true)
                .build());

        // 构建Skill定义
        this.definition = SkillDefinition.builder()
                .name("weather")
                .description("天气查询与提醒技能，可以查询城市天气并给出穿衣建议")
                .detailedDescription("""
                    这是一个天气查询技能，可以帮助用户获取指定城市的天气信息，并提供穿衣建议。
                    
                    功能：
                    1. 查询指定城市的当前天气状况
                    2. 提供温度、湿度、风力等详细信息
                    3. 根据天气给出穿衣建议
                    4. 提醒是否需要带伞
                    
                    支持的城市包括：北京、上海、广州、深圳、杭州、南京、成都、武汉、西安、重庆等
                    """)
                .parameters(parameters)
                .examples("""
                    用户：今天北京天气怎么样？
                    调用：execute_skill("weather", {"city": "北京", "input": "今天北京天气怎么样？"})
                    
                    用户：上海明天需要带伞吗？
                    调用：execute_skill("weather", {"city": "上海", "input": "上海明天需要带伞吗？"})
                    """)
                .returnDescription("返回包含天气信息、穿衣建议的友好回复文本")
                .build();

        // 注册到SkillRegistry
        skillRegistry.registerSkill(definition, this);
        log.info("WeatherSkillExecutor registered to SkillRegistry");
    }

    @Override
    public SkillDefinition getDefinition() {
        return definition;
    }

    @Override
    public SkillResult execute(Map<String, Object> params) {
        log.info("Executing WeatherSkill with params: {}", params);

        try {
            // 提取参数
            String userInput = (String) params.getOrDefault("input", "");
            String city = (String) params.get("city");

            // 如果没有提供城市，从输入中提取
            if (city == null || city.isEmpty()) {
                city = extractCity(userInput);
            }

            // 获取天气信息
            String weatherInfo = getWeatherInfo(city);

            // 构建提示词
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("""
                你是一个天气助手，需要根据天气信息给用户友好的回复。
                回复时：
                1. 用轻松友好的语气
                2. 给出穿衣建议
                3. 提醒是否需要带伞
                4. 可以适当幽默
                """));
            messages.add(new UserMessage("城市：" + city + "\n天气信息：" + weatherInfo + "\n用户问题：" + userInput));

            // 调用模型生成回复
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);

            String responseText = "";
            if (response != null && response.getResult() != null) {
                responseText = response.getResult().getOutput().getContent();
            }

            // 构建结果
            Map<String, Object> metadata = Map.of(
                    "city", city,
                    "weather_info", weatherInfo,
                    "skill_name", "weather"
            );

            log.info("WeatherSkill executed successfully for city: {}", city);
            return SkillResult.success(responseText, metadata);

        } catch (Exception e) {
            log.error("WeatherSkill execution failed", e);
            return SkillResult.error("天气查询失败: " + e.getMessage());
        }
    }

    /**
     * 从输入中提取城市名称
     */
    private String extractCity(String input) {
        if (input == null || input.isEmpty()) {
            return "北京";
        }

        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京",
                "成都", "武汉", "西安", "重庆", "苏州", "天津", "青岛", "大连"};

        for (String city : cities) {
            if (input.contains(city)) {
                return city;
            }
        }

        // 默认返回北京
        return "北京";
    }

    /**
     * 获取天气信息（模拟数据，实际可接入真实天气API）
     */
    private String getWeatherInfo(String city) {
        return String.format("""
            %s今日天气：
            - 天气状况：晴转多云
            - 温度：18°C - 26°C
            - 湿度：65%%
            - 风力：东南风3级
            - 空气质量：良好
            - 紫外线：中等
            - 降水概率：20%%
            """, city);
    }
}

package com.kanyu.companion.skill;

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
import java.util.Map;

@Slf4j
@Component
public class WeatherSkill implements Skill {
    
    private final ChatModel chatModel;
    private final SkillConfig config;
    
    public WeatherSkill(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.config = SkillConfig.defaultConfig("weather");
    }
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "天气查询与提醒技能，可以查询城市天气并给出穿衣建议";
    }
    
    @Override
    public boolean canHandle(String input, GraphState state) {
        if (!config.isEnabled()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("天气") || lowerInput.contains("温度")
            || lowerInput.contains("下雨") || lowerInput.contains("穿什么")
            || lowerInput.contains("带伞") || lowerInput.contains("冷不冷")
            || lowerInput.contains("热不热");
    }
    
    @Override
    public GraphState execute(GraphState state) {
        log.info("Executing WeatherSkill");
        
        try {
            String userInput = state.getUserInput();
            String city = extractCity(userInput);
            
            String weatherInfo = getWeatherInfo(city);
            
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
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            String responseText = "";
            if (response != null && response.getResult() != null) {
                responseText = response.getResult().getOutput().getText();
            }
            
            state.put("skill_response", responseText);
            state.put("skill_used", "weather");
            state.put("weather_city", city);
            
        } catch (Exception e) {
            log.error("WeatherSkill execution failed", e);
            state.put("skill_error", e.getMessage());
        }
        
        return state;
    }
    
    @Override
    public SkillConfig getConfig() {
        return config;
    }
    
    @Override
    public int getPriority() {
        return 90;
    }
    
    @Override
    public String[] getTriggers() {
        return new String[]{"天气", "温度", "下雨", "穿什么", "带伞"};
    }
    
    private String extractCity(String input) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京", 
                          "成都", "武汉", "西安", "重庆", "苏州", "天津"};
        for (String city : cities) {
            if (input.contains(city)) {
                return city;
            }
        }
        return "北京";
    }
    
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

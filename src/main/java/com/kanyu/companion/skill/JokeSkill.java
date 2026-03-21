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
public class JokeSkill implements Skill {
    
    private final ChatModel chatModel;
    private final SkillConfig config;
    
    private static final String[] JOKE_TRIGGERS = {
        "讲个笑话", "说个笑话", "来个笑话", "逗我笑", "开心一下",
        "无聊", "心情不好", "难过", "不开心", "郁闷"
    };
    
    public JokeSkill(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.config = SkillConfig.defaultConfig("joke");
        config.setPriority(50);
    }
    
    @Override
    public String getName() {
        return "joke";
    }
    
    @Override
    public String getDescription() {
        return "讲笑话技能，在用户心情低落或主动要求时讲笑话逗乐";
    }
    
    @Override
    public boolean canHandle(String input, GraphState state) {
        if (!config.isEnabled()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        for (String trigger : JOKE_TRIGGERS) {
            if (lowerInput.contains(trigger)) {
                return true;
            }
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> emotion = (Map<String, Object>) state.get("emotion_analysis");
        if (emotion != null) {
            String primaryEmotion = (String) emotion.get("primary_emotion");
            Boolean needsSupport = (Boolean) emotion.get("needs_support");
            
            if (("sad".equals(primaryEmotion) || "anxious".equals(primaryEmotion)) 
                && Boolean.TRUE.equals(needsSupport)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public GraphState execute(GraphState state) {
        log.info("Executing JokeSkill");
        
        try {
            String userInput = state.getUserInput();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> emotion = (Map<String, Object>) state.get("emotion_analysis");
            boolean isSad = emotion != null && "sad".equals(emotion.get("primary_emotion"));
            
            List<Message> messages = new ArrayList<>();
            
            if (isSad) {
                messages.add(new SystemMessage("""
                    用户现在心情不太好，你需要讲一个轻松有趣的小笑话来逗用户开心。
                    要求：
                    1. 笑话要温馨、积极、正能量
                    2. 不要有任何负面或敏感内容
                    3. 讲完笑话后，给用户一些温暖的安慰和鼓励
                    4. 语气要温柔、真诚
                    """));
            } else {
                messages.add(new SystemMessage("""
                    请讲一个有趣的笑话。
                    要求：
                    1. 笑话要轻松有趣
                    2. 不要有任何负面或敏感内容
                    3. 可以适当加入一些俏皮的表达
                    """));
            }
            
            messages.add(new UserMessage(userInput));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            String responseText = "";
            if (response != null && response.getResult() != null) {
                responseText = response.getResult().getOutput().getContent();
            }
            
            state.put("skill_response", responseText);
            state.put("skill_used", "joke");
            
        } catch (Exception e) {
            log.error("JokeSkill execution failed", e);
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
        return 50;
    }
    
    @Override
    public String[] getTriggers() {
        return JOKE_TRIGGERS;
    }
}

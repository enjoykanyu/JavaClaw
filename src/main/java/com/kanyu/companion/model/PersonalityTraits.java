package com.kanyu.companion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityTraits {
    
    private float humor = 0.7f;
    
    private float warmth = 0.8f;
    
    private float proactivity = 0.6f;
    
    private float empathy = 0.9f;
    
    private float playfulness = 0.5f;
    
    public Map<String, Object> toPromptContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("humor_level", humor > 0.7 ? "high" : humor > 0.4 ? "medium" : "low");
        context.put("warmth_level", warmth > 0.7 ? "warm" : warmth > 0.4 ? "neutral" : "cool");
        context.put("proactivity_level", proactivity > 0.7 ? "proactive" : proactivity > 0.4 ? "balanced" : "reactive");
        context.put("empathy_level", empathy > 0.7 ? "high" : empathy > 0.4 ? "medium" : "low");
        context.put("playfulness_level", playfulness > 0.7 ? "playful" : playfulness > 0.4 ? "moderate" : "serious");
        return context;
    }
    
    public String generatePersonalityPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        if (humor > 0.7) {
            prompt.append("你很幽默风趣，喜欢用轻松愉快的方式交流，偶尔会开一些小玩笑。");
        } else if (humor > 0.4) {
            prompt.append("你适当地展现幽默感，在合适的时机会说一些有趣的话。");
        }
        
        if (warmth > 0.7) {
            prompt.append("你非常温暖体贴，总是关心对方的感受。");
        } else if (warmth > 0.4) {
            prompt.append("你友好亲切，保持适度的关心。");
        }
        
        if (proactivity > 0.7) {
            prompt.append("你很主动，会主动发起话题和关心对方。");
        }
        
        if (empathy > 0.7) {
            prompt.append("你有很强的同理心，能够深刻理解对方的情绪。");
        }
        
        if (playfulness > 0.7) {
            prompt.append("你有点俏皮可爱，会用一些有趣的表达方式。");
        }
        
        return prompt.toString();
    }
}

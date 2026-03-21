package com.kanyu.companion.service;

import com.kanyu.companion.model.CompanionProfile;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GreetingService {
    
    private final ChatModel chatModel;
    private final CompanionService companionService;
    private final MemoryService memoryService;
    
    private static final Map<String, String> GREETING_TEMPLATES = Map.of(
        "morning", "早上好！新的一天开始了，希望你今天有个好心情！☀️",
        "noon", "中午好！记得吃午饭哦，身体是革命的本钱~ 🍱",
        "afternoon", "下午好！下午茶时间到了，要不要休息一下？☕",
        "evening", "晚上好！今天辛苦了，晚上有什么安排吗？🌙",
        "night", "夜深了，早点休息吧，明天又是元气满满的一天！💤"
    );
    
    @Scheduled(cron = "${companion.greeting.morning-cron:0 0 8 * * ?}")
    public void sendMorningGreeting() {
        log.info("Sending morning greetings");
        sendGreetingToAllUsers("morning");
    }
    
    @Scheduled(cron = "${companion.greeting.evening-cron:0 0 22 * * ?}")
    public void sendEveningGreeting() {
        log.info("Sending evening greetings");
        sendGreetingToAllUsers("evening");
    }
    
    public void sendGreetingToAllUsers(String greetingType) {
        log.info("Sending {} greeting to all users", greetingType);
    }
    
    public String generateGreeting(Long userId, String greetingType) {
        CompanionProfile profile = companionService.getOrCreateDefaultProfile(userId);
        
        String template = GREETING_TEMPLATES.getOrDefault(greetingType, "你好！");
        
        String memoryContext = memoryService.buildMemoryContext(userId);
        
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(String.format("""
                你是%s，是用户的%s。
                你的性格：%s
                你的说话风格：%s
                
                请根据以下模板生成一个个性化的问候语：
                %s
                
                要求：
                1. 保持你的角色设定
                2. 语气温暖友好
                3. 可以适当加入一些关心的话语
                4. 如果有用户的相关记忆，可以适当提及
                """,
                profile.getName(),
                profile.getRelationship(),
                profile.getPersonality(),
                profile.getSpeakingStyle(),
                template
            )));
            
            if (memoryContext != null && !memoryContext.isEmpty()) {
                messages.add(new SystemMessage("用户相关记忆：\n" + memoryContext));
            }
            
            messages.add(new UserMessage("请生成问候语"));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getContent();
            }

        } catch (Exception e) {
            log.error("Failed to generate greeting", e);
        }

        return template;
    }
    
    public String generateContextualGreeting(Long userId) {
        LocalTime now = LocalTime.now();
        String timeOfDay;
        
        if (now.isBefore(LocalTime.of(12, 0))) {
            timeOfDay = "morning";
        } else if (now.isBefore(LocalTime.of(14, 0))) {
            timeOfDay = "noon";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            timeOfDay = "afternoon";
        } else if (now.isBefore(LocalTime.of(22, 0))) {
            timeOfDay = "evening";
        } else {
            timeOfDay = "night";
        }
        
        return generateGreeting(userId, timeOfDay);
    }
    
    public String generateWeatherBasedGreeting(Long userId, String weather) {
        CompanionProfile profile = companionService.getOrCreateDefaultProfile(userId);
        
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(String.format("""
                你是%s。根据天气情况生成一个关心用户的问候语。
                
                当前天气：%s
                
                要求：
                1. 表达对用户的关心
                2. 给出适当的建议（穿衣、出行等）
                3. 语气温暖友好
                """,
                profile.getName(),
                weather
            )));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getContent();
            }

        } catch (Exception e) {
            log.error("Failed to generate weather greeting", e);
        }

        return "今天天气不错，注意保暖哦！";
    }
    
    public String generateFestivalGreeting(Long userId, String festival) {
        CompanionProfile profile = companionService.getOrCreateDefaultProfile(userId);
        
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(String.format("""
                你是%s。今天是%s，请生成一个节日祝福。
                
                要求：
                1. 表达节日祝福
                2. 语气温暖真诚
                3. 可以适当加入一些节日相关的元素
                """,
                profile.getName(),
                festival
            )));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getContent();
            }

        } catch (Exception e) {
            log.error("Failed to generate festival greeting", e);
        }

        return String.format("%s快乐！🎉", festival);
    }
    
    public String checkAndGenerateGreeting(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        int hour = now.getHour();
        if (hour == 8) {
            return generateGreeting(userId, "morning");
        } else if (hour == 12) {
            return generateGreeting(userId, "noon");
        } else if (hour == 22) {
            return generateGreeting(userId, "night");
        }
        
        return null;
    }
}

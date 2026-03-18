package com.kanyu.companion.skill;

import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ReminderSkill implements Skill {
    
    private final SkillConfig config;
    private final Map<Long, Map<String, LocalDateTime>> userReminders = new HashMap<>();
    
    public ReminderSkill() {
        this.config = SkillConfig.defaultConfig("reminder");
        config.setPriority(85);
    }
    
    @Override
    public String getName() {
        return "reminder";
    }
    
    @Override
    public String getDescription() {
        return "提醒服务技能，可以设置、查询和取消提醒";
    }
    
    @Override
    public boolean canHandle(String input, GraphState state) {
        if (!config.isEnabled()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("提醒") || lowerInput.contains("闹钟")
            || lowerInput.contains("定时") || lowerInput.contains("备忘");
    }
    
    @Override
    public GraphState execute(GraphState state) {
        log.info("Executing ReminderSkill");
        
        try {
            Long userId = state.get("userId");
            String userInput = state.getUserInput();
            
            String response;
            
            if (userInput.contains("设置") || userInput.contains("添加") || userInput.contains("创建")) {
                response = handleSetReminder(userId, userInput);
            } else if (userInput.contains("查询") || userInput.contains("查看") || userInput.contains("列表")) {
                response = handleListReminders(userId);
            } else if (userInput.contains("取消") || userInput.contains("删除")) {
                response = handleCancelReminder(userId, userInput);
            } else {
                response = handleGeneralReminder(userId, userInput);
            }
            
            state.put("skill_response", response);
            state.put("skill_used", "reminder");
            
        } catch (Exception e) {
            log.error("ReminderSkill execution failed", e);
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
        return 85;
    }
    
    @Override
    public String[] getTriggers() {
        return new String[]{"提醒", "闹钟", "定时", "备忘"};
    }
    
    private String handleSetReminder(Long userId, String input) {
        String reminderName = extractReminderName(input);
        LocalDateTime reminderTime = extractReminderTime(input);
        
        userReminders.computeIfAbsent(userId, k -> new HashMap<>())
            .put(reminderName, reminderTime);
        
        return String.format("好的，我已经为你设置了提醒：\n" +
            "- 提醒事项：%s\n" +
            "- 提醒时间：%s\n\n" +
            "到时候我会提醒你的！",
            reminderName,
            reminderTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }
    
    private String handleListReminders(Long userId) {
        Map<String, LocalDateTime> reminders = userReminders.get(userId);
        
        if (reminders == null || reminders.isEmpty()) {
            return "你目前没有设置任何提醒。需要我帮你设置一个吗？";
        }
        
        StringBuilder sb = new StringBuilder("你目前的提醒列表：\n\n");
        int index = 1;
        for (Map.Entry<String, LocalDateTime> entry : reminders.entrySet()) {
            sb.append(index++).append(". ")
              .append(entry.getKey())
              .append(" - ")
              .append(entry.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
              .append("\n");
        }
        
        return sb.toString();
    }
    
    private String handleCancelReminder(Long userId, String input) {
        String reminderName = extractReminderName(input);
        Map<String, LocalDateTime> reminders = userReminders.get(userId);
        
        if (reminders == null || !reminders.containsKey(reminderName)) {
            return "没有找到这个提醒，可能已经被取消了。";
        }
        
        reminders.remove(reminderName);
        return String.format("好的，已经取消了提醒：%s", reminderName);
    }
    
    private String handleGeneralReminder(Long userId, String input) {
        return "我可以帮你设置提醒、查看提醒列表或取消提醒。\n" +
            "比如你可以说：\n" +
            "- \"提醒我明天早上8点开会\"\n" +
            "- \"查看我的提醒\"\n" +
            "- \"取消开会的提醒\"";
    }
    
    private String extractReminderName(String input) {
        if (input.contains("提醒我")) {
            int start = input.indexOf("提醒我") + 3;
            int end = input.indexOf("在");
            if (end == -1) {
                end = input.indexOf("明天");
            }
            if (end == -1) {
                end = input.indexOf("下午");
            }
            if (end == -1) {
                end = input.indexOf("上午");
            }
            if (end == -1) {
                end = input.length();
            }
            return input.substring(start, end).trim();
        }
        return "未命名提醒";
    }
    
    private LocalDateTime extractReminderTime(String input) {
        LocalDateTime now = LocalDateTime.now();
        
        if (input.contains("明天")) {
            now = now.plusDays(1);
        }
        
        if (input.contains("早上") || input.contains("上午")) {
            now = now.withHour(8).withMinute(0);
        } else if (input.contains("下午")) {
            now = now.withHour(14).withMinute(0);
        } else if (input.contains("晚上")) {
            now = now.withHour(20).withMinute(0);
        }
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)点");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            now = now.withHour(hour).withMinute(0);
        }
        
        pattern = java.util.regex.Pattern.compile("(\\d+):?(\\d+)");
        matcher = pattern.matcher(input);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = matcher.groupCount() > 1 ? Integer.parseInt(matcher.group(2)) : 0;
            now = now.withHour(hour).withMinute(minute);
        }
        
        return now;
    }
}

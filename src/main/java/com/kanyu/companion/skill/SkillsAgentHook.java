package com.kanyu.companion.skill;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skills Agent Hook
 * 符合Spring AI Alibaba规范的Skills钩子
 * 负责注册read_skill工具并注入技能列表到系统提示
 */
@Slf4j
@Data
@Builder
public class SkillsAgentHook {

    /**
     * Skill注册表
     */
    private SkillRegistry skillRegistry;

    /**
     * 是否自动重载
     */
    @Builder.Default
    private boolean autoReload = false;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * read_skill工具名称
     */
    private static final String READ_SKILL_TOOL = "read_skill";

    /**
     * read_skill调用模式
     */
    private static final Pattern READ_SKILL_PATTERN = Pattern.compile(
        "read_skill\\s*\\(\\s*[\"']?([^\"']+)[\"']?\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 处理系统提示词，注入Skill列表
     * @param originalPrompt 原始系统提示词
     * @return 增强后的系统提示词
     */
    public String enhanceSystemPrompt(String originalPrompt) {
        if (skillRegistry == null || userId == null) {
            return originalPrompt;
        }

        String skillListPrompt = skillRegistry.generateSkillListPrompt(userId);

        if (originalPrompt == null || originalPrompt.isEmpty()) {
            return skillListPrompt;
        }

        return originalPrompt + "\n" + skillListPrompt;
    }

    /**
     * 处理用户输入，检测并执行read_skill调用
     * @param userInput 用户输入
     * @return read_skill执行结果，如果没有调用则返回null
     */
    public String processReadSkillCall(String userInput) {
        if (skillRegistry == null || userInput == null) {
            return null;
        }

        Matcher matcher = READ_SKILL_PATTERN.matcher(userInput);
        if (matcher.find()) {
            String skillName = matcher.group(1).trim();
            log.info("Detected read_skill call for skill: {}", skillName);
            return skillRegistry.readSkill(skillName);
        }

        return null;
    }

    /**
     * 检查是否包含read_skill调用
     * @param text 文本内容
     * @return 是否包含
     */
    public boolean containsReadSkillCall(String text) {
        if (text == null) {
            return false;
        }
        return READ_SKILL_PATTERN.matcher(text).find();
    }

    /**
     * 提取read_skill调用的Skill名称
     * @param text 文本内容
     * @return Skill名称列表
     */
    public List<String> extractReadSkillCalls(String text) {
        List<String> skillNames = new ArrayList<>();
        if (text == null) {
            return skillNames;
        }

        Matcher matcher = READ_SKILL_PATTERN.matcher(text);
        while (matcher.find()) {
            skillNames.add(matcher.group(1).trim());
        }

        return skillNames;
    }

    /**
     * 获取read_skill工具描述
     * @return 工具描述
     */
    public String getReadSkillToolDescription() {
        return """
            read_skill(skill_name) - 获取指定技能的详细信息
            
            参数:
            - skill_name: 技能名称
            
            使用场景:
            当你需要使用某个技能但不确定具体用法时，先调用此工具获取技能详情。
            
            示例:
            read_skill("weather")
            read_skill("news")
            """;
    }

    /**
     * 构建完整的系统提示词（包含read_skill工具说明）
     * @param basePrompt 基础系统提示词
     * @return 完整系统提示词
     */
    public String buildCompleteSystemPrompt(String basePrompt) {
        StringBuilder sb = new StringBuilder();

        if (basePrompt != null && !basePrompt.isEmpty()) {
            sb.append(basePrompt).append("\n\n");
        }

        // 添加read_skill工具说明
        sb.append("【工具说明】\n");
        sb.append(getReadSkillToolDescription()).append("\n\n");

        // 添加Skill列表
        sb.append(enhanceSystemPrompt(""));

        return sb.toString();
    }

    /**
     * 执行Skill（通过名称）
     * @param skillName Skill名称
     * @param params 执行参数
     * @return 执行结果
     */
    public SkillResult executeSkill(String skillName, Map<String, Object> params) {
        if (skillRegistry == null) {
            return SkillResult.error("SkillRegistry not initialized");
        }

        if (!skillRegistry.hasSkill(skillName)) {
            return SkillResult.error("Skill not found: " + skillName);
        }

        return skillRegistry.executeSkill(skillName, params);
    }

    /**
     * 尝试自动检测并执行合适的Skill
     * @param userInput 用户输入
     * @return 执行结果，如果没有匹配的Skill则返回null
     */
    public SkillResult tryAutoExecuteSkill(String userInput) {
        if (skillRegistry == null || userInput == null) {
            return null;
        }

        // 获取所有启用的Skill
        List<SkillDefinition> skills = skillRegistry.getEnabledSkillDefinitionsForUser(userId);

        // 这里可以实现智能匹配逻辑
        // 简单实现：根据关键词匹配
        for (SkillDefinition skill : skills) {
            String skillName = skill.getName().toLowerCase();
            String description = skill.getDescription().toLowerCase();
            String input = userInput.toLowerCase();

            // 检查是否包含Skill名称或描述关键词
            if (input.contains(skillName) || description.contains(input)) {
                log.info("Auto-matched skill: {} for input: {}", skill.getName(), userInput);
                return skillRegistry.executeSkill(skill.getName(), Map.of("input", userInput));
            }
        }

        return null;
    }
}

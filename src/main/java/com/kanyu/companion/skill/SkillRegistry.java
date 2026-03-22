package com.kanyu.companion.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Skill注册表
 * 符合Spring AI Alibaba规范的Skill注册中心
 * 管理所有Skill的定义和执行器
 */
@Slf4j
@Component
public class SkillRegistry {
    
    /**
     * Skill定义映射
     */
    private final Map<String, SkillDefinition> skillDefinitions = new ConcurrentHashMap<>();
    
    /**
     * Skill执行器映射
     */
    private final Map<String, SkillExecutor> skillExecutors = new ConcurrentHashMap<>();
    
    /**
     * 用户启用的Skill映射
     */
    private final Map<Long, Set<String>> userEnabledSkills = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("SkillRegistry initialized");
    }
    
    /**
     * 注册Skill
     * @param definition Skill定义
     * @param executor Skill执行器
     */
    public void registerSkill(SkillDefinition definition, SkillExecutor executor) {
        String skillName = definition.getName();
        skillDefinitions.put(skillName, definition);
        skillExecutors.put(skillName, executor);
        log.info("Registered skill: {}", skillName);
    }
    
    /**
     * 注册Skill（仅定义，用于read_skill查询）
     * @param definition Skill定义
     */
    public void registerSkillDefinition(SkillDefinition definition) {
        skillDefinitions.put(definition.getName(), definition);
        log.info("Registered skill definition: {}", definition.getName());
    }
    
    /**
     * 获取Skill定义
     * @param skillName Skill名称
     * @return Skill定义
     */
    public SkillDefinition getSkillDefinition(String skillName) {
        return skillDefinitions.get(skillName);
    }
    
    /**
     * 获取所有Skill定义
     * @return 所有Skill定义列表
     */
    public List<SkillDefinition> getAllSkillDefinitions() {
        return new ArrayList<>(skillDefinitions.values());
    }
    
    /**
     * 获取所有Skill名称
     * @return Skill名称列表
     */
    public List<String> getAllSkillNames() {
        return new ArrayList<>(skillDefinitions.keySet());
    }
    
    /**
     * 获取Skill执行器
     * @param skillName Skill名称
     * @return Skill执行器
     */
    public SkillExecutor getSkillExecutor(String skillName) {
        return skillExecutors.get(skillName);
    }
    
    /**
     * 检查Skill是否存在
     * @param skillName Skill名称
     * @return 是否存在
     */
    public boolean hasSkill(String skillName) {
        return skillDefinitions.containsKey(skillName);
    }
    
    /**
     * 检查Skill是否有执行器
     * @param skillName Skill名称
     * @return 是否有执行器
     */
    public boolean hasExecutor(String skillName) {
        return skillExecutors.containsKey(skillName);
    }
    
    /**
     * 执行Skill
     * @param skillName Skill名称
     * @param params 执行参数
     * @return 执行结果
     */
    public SkillResult executeSkill(String skillName, Map<String, Object> params) {
        SkillExecutor executor = skillExecutors.get(skillName);
        if (executor == null) {
            log.error("Skill executor not found: {}", skillName);
            return SkillResult.error("Skill not found: " + skillName);
        }
        
        try {
            return executor.execute(params);
        } catch (Exception e) {
            log.error("Skill execution failed: {}", skillName, e);
            return SkillResult.error("Execution failed: " + e.getMessage());
        }
    }
    
    /**
     * 为用户启用Skill
     * @param userId 用户ID
     * @param skillName Skill名称
     */
    public void enableSkillForUser(Long userId, String skillName) {
        userEnabledSkills.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(skillName);
        log.info("Enabled skill {} for user {}", skillName, userId);
    }
    
    /**
     * 为用户禁用Skill
     * @param userId 用户ID
     * @param skillName Skill名称
     */
    public void disableSkillForUser(Long userId, String skillName) {
        Set<String> enabled = userEnabledSkills.get(userId);
        if (enabled != null) {
            enabled.remove(skillName);
            log.info("Disabled skill {} for user {}", skillName, userId);
        }
    }
    
    /**
     * 获取用户启用的Skill列表
     * @param userId 用户ID
     * @return Skill名称列表
     */
    public List<String> getEnabledSkillsForUser(Long userId) {
        Set<String> enabled = userEnabledSkills.get(userId);
        if (enabled == null || enabled.isEmpty()) {
            // 默认返回所有Skill
            return getAllSkillNames();
        }
        return new ArrayList<>(enabled);
    }
    
    /**
     * 获取用户启用的Skill定义列表
     * @param userId 用户ID
     * @return Skill定义列表
     */
    public List<SkillDefinition> getEnabledSkillDefinitionsForUser(Long userId) {
        List<String> skillNames = getEnabledSkillsForUser(userId);
        return skillNames.stream()
                .map(skillDefinitions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查Skill是否对用户启用
     * @param userId 用户ID
     * @param skillName Skill名称
     * @return 是否启用
     */
    public boolean isSkillEnabledForUser(Long userId, String skillName) {
        Set<String> enabled = userEnabledSkills.get(userId);
        if (enabled == null) {
            return true; // 默认启用
        }
        return enabled.contains(skillName);
    }
    
    /**
     * 重置用户Skill设置
     * @param userId 用户ID
     */
    public void resetUserSkills(Long userId) {
        userEnabledSkills.remove(userId);
        log.info("Reset skills for user {}", userId);
    }
    
    /**
     * 生成系统提示词中的Skill列表
     * @param userId 用户ID
     * @return Skill列表文本
     */
    public String generateSkillListPrompt(Long userId) {
        List<SkillDefinition> skills = getEnabledSkillDefinitionsForUser(userId);
        if (skills.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n【可用技能】\n");
        sb.append("你可以使用以下技能来帮助用户，如需使用某个技能，请先调用 read_skill(skill_name) 获取详细信息：\n\n");
        
        for (SkillDefinition skill : skills) {
            sb.append(skill.toBriefDescription()).append("\n");
        }
        
        sb.append("\n如需了解某个技能的详细信息，请调用 read_skill(\"技能名称\")\n");
        
        return sb.toString();
    }
    
    /**
     * read_skill工具实现
     * @param skillName Skill名称
     * @return Skill详细描述
     */
    public String readSkill(String skillName) {
        SkillDefinition definition = skillDefinitions.get(skillName);
        if (definition == null) {
            return String.format("Skill '%s' not found. Available skills: %s", 
                skillName, String.join(", ", getAllSkillNames()));
        }
        return definition.toFullDescription();
    }
}

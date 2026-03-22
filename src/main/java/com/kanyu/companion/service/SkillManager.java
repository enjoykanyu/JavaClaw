package com.kanyu.companion.service;

import com.kanyu.companion.skill.*;
import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill管理器
 * 集成Spring AI Alibaba规范的SkillRegistry，提供统一的Skill管理接口
 */
@Slf4j
@Service
public class SkillManager {

    private final SkillRegistry skillRegistry;
    private final SkillsAgentHook skillsAgentHook;

    public SkillManager(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
        this.skillsAgentHook = SkillsAgentHook.builder()
                .skillRegistry(skillRegistry)
                .autoReload(false)
                .build();
    }

    @PostConstruct
    public void init() {
        log.info("SkillManager initialized with {} skills", skillRegistry.getAllSkillNames().size());
    }

    /**
     * 获取所有Skill定义
     * @return Skill定义列表
     */
    public List<SkillDefinition> getAllSkills() {
        return skillRegistry.getAllSkillDefinitions();
    }

    /**
     * 获取用户启用的Skill列表
     * @param userId 用户ID
     * @return Skill定义列表
     */
    public List<SkillDefinition> getEnabledSkills(Long userId) {
        return skillRegistry.getEnabledSkillDefinitionsForUser(userId);
    }

    /**
     * 获取Skill定义
     * @param skillName Skill名称
     * @return Skill定义
     */
    public SkillDefinition getSkillDefinition(String skillName) {
        return skillRegistry.getSkillDefinition(skillName);
    }

    /**
     * 执行Skill
     * @param skillName Skill名称
     * @param params 执行参数
     * @return 执行结果
     */
    public SkillResult executeSkill(String skillName, Map<String, Object> params) {
        return skillRegistry.executeSkill(skillName, params);
    }

    /**
     * 从状态中提取参数并执行Skill
     * @param skillName Skill名称
     * @param state 图状态
     * @return 执行结果
     */
    public SkillResult executeSkillFromState(String skillName, GraphState state) {
        Map<String, Object> params = new HashMap<>();
        params.put("input", state.getUserInput());
        params.put("userId", state.get("userId"));

        // 将state中的其他数据也作为参数传递
        if (state.get("city") != null) {
            params.put("city", state.get("city"));
        }
        if (state.get("category") != null) {
            params.put("category", state.get("category"));
        }
        if (state.get("query") != null) {
            params.put("query", state.get("query"));
        }

        return executeSkill(skillName, params);
    }

    /**
     * 为用户启用Skill
     * @param userId 用户ID
     * @param skillName Skill名称
     */
    public void enableSkill(Long userId, String skillName) {
        skillRegistry.enableSkillForUser(userId, skillName);
    }

    /**
     * 为用户禁用Skill
     * @param userId 用户ID
     * @param skillName Skill名称
     */
    public void disableSkill(Long userId, String skillName) {
        skillRegistry.disableSkillForUser(userId, skillName);
    }

    /**
     * 检查Skill是否对用户启用
     * @param userId 用户ID
     * @param skillName Skill名称
     * @return 是否启用
     */
    public boolean isSkillEnabled(Long userId, String skillName) {
        return skillRegistry.isSkillEnabledForUser(userId, skillName);
    }

    /**
     * 重置用户Skill设置
     * @param userId 用户ID
     */
    public void resetUserSkills(Long userId) {
        skillRegistry.resetUserSkills(userId);
    }

    /**
     * 获取Skill名称到定义的映射
     * @return Skill映射
     */
    public Map<String, SkillDefinition> getSkillMap() {
        return skillRegistry.getAllSkillDefinitions().stream()
                .collect(Collectors.toMap(SkillDefinition::getName, s -> s));
    }

    /**
     * 生成系统提示词中的Skill列表
     * @param userId 用户ID
     * @return Skill列表文本
     */
    public String generateSkillListPrompt(Long userId) {
        skillsAgentHook.setUserId(userId);
        return skillsAgentHook.enhanceSystemPrompt("");
    }

    /**
     * 构建完整的系统提示词（包含read_skill工具说明和Skill列表）
     * @param basePrompt 基础系统提示词
     * @param userId 用户ID
     * @return 完整系统提示词
     */
    public String buildCompleteSystemPrompt(String basePrompt, Long userId) {
        skillsAgentHook.setUserId(userId);
        return skillsAgentHook.buildCompleteSystemPrompt(basePrompt);
    }

    /**
     * 处理read_skill调用
     * @param userInput 用户输入
     * @return read_skill执行结果，如果没有调用则返回null
     */
    public String processReadSkillCall(String userInput) {
        return skillsAgentHook.processReadSkillCall(userInput);
    }

    /**
     * 检查是否包含read_skill调用
     * @param text 文本内容
     * @return 是否包含
     */
    public boolean containsReadSkillCall(String text) {
        return skillsAgentHook.containsReadSkillCall(text);
    }

    /**
     * read_skill工具实现
     * @param skillName Skill名称
     * @return Skill详细描述
     */
    public String readSkill(String skillName) {
        return skillRegistry.readSkill(skillName);
    }

    /**
     * 尝试自动检测并执行合适的Skill
     * @param userId 用户ID
     * @param userInput 用户输入
     * @return 执行结果，如果没有匹配的Skill则返回null
     */
    public SkillResult tryAutoExecuteSkill(Long userId, String userInput) {
        skillsAgentHook.setUserId(userId);
        return skillsAgentHook.tryAutoExecuteSkill(userInput);
    }

    /**
     * 检查Skill是否存在
     * @param skillName Skill名称
     * @return 是否存在
     */
    public boolean hasSkill(String skillName) {
        return skillRegistry.hasSkill(skillName);
    }

    /**
     * 获取已注册Skill数量
     * @return Skill数量
     */
    public int getSkillCount() {
        return skillRegistry.getAllSkillNames().size();
    }

    /**
     * 获取所有Skill名称
     * @return Skill名称列表
     */
    public List<String> getAllSkillNames() {
        return skillRegistry.getAllSkillNames();
    }

    // ==================== 兼容旧接口的方法 ====================

    /**
     * 执行Skills（兼容旧接口）
     * 根据用户输入自动检测并执行合适的Skill
     * @param state 图状态
     * @return 更新后的状态
     * @deprecated 建议使用 tryAutoExecuteSkill 或 executeSkill
     */
    @Deprecated
    public GraphState executeSkills(GraphState state) {
        String userInput = state.getUserInput();
        Long userId = state.get("userId");

        if (userInput == null || userInput.isEmpty()) {
            return state;
        }

        // 尝试自动执行Skill
        SkillResult result = tryAutoExecuteSkill(userId, userInput);

        if (result != null) {
            state.put("skill_response", result.getContent());
            state.put("skill_used", result.getMetadata() != null ?
                    result.getMetadata().get("skill_name") : "unknown");
            state.put("skill_success", result.isSuccess());
            if (!result.isSuccess()) {
                state.put("skill_error", result.getError());
            }
        }

        return state;
    }
}

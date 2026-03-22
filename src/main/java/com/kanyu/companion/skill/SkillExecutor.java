package com.kanyu.companion.skill;

import java.util.Map;

/**
 * Skill执行器接口
 * 符合Spring AI Alibaba规范的Skill执行器
 */
public interface SkillExecutor {
    
    /**
     * 执行Skill
     * @param params 执行参数
     * @return 执行结果
     */
    SkillResult execute(Map<String, Object> params);
    
    /**
     * 获取Skill定义
     * @return Skill定义
     */
    SkillDefinition getDefinition();
}

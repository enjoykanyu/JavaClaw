package com.kanyu.companion.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill执行结果
 * 符合Spring AI Alibaba规范的Skill执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResult {
    
    /**
     * 是否成功
     */
    @Builder.Default
    private boolean success = true;
    
    /**
     * 结果内容
     */
    @Builder.Default
    private String content = "";
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 额外数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 创建成功结果
     * @param content 结果内容
     * @return SkillResult
     */
    public static SkillResult success(String content) {
        return SkillResult.builder()
                .success(true)
                .content(content)
                .build();
    }
    
    /**
     * 创建成功结果（带元数据）
     * @param content 结果内容
     * @param metadata 元数据
     * @return SkillResult
     */
    public static SkillResult success(String content, Map<String, Object> metadata) {
        return SkillResult.builder()
                .success(true)
                .content(content)
                .metadata(metadata)
                .build();
    }
    
    /**
     * 创建错误结果
     * @param error 错误信息
     * @return SkillResult
     */
    public static SkillResult error(String error) {
        return SkillResult.builder()
                .success(false)
                .error(error)
                .content("执行失败: " + error)
                .build();
    }
    
    /**
     * 转换为字符串
     * @return 结果字符串
     */
    public String toResultString() {
        if (success) {
            return content;
        } else {
            return "Error: " + error;
        }
    }
}

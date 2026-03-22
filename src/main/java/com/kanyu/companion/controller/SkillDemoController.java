package com.kanyu.companion.controller;

import com.kanyu.companion.skill.SkillDefinition;
import com.kanyu.companion.skill.SkillResult;
import com.kanyu.companion.service.SkillManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill演示控制器
 * 展示Spring AI Alibaba规范的Skill系统用法
 */
@Slf4j
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillDemoController {

    private final SkillManager skillManager;

    /**
     * 获取所有Skill列表
     */
    @GetMapping
    public ResponseEntity<List<SkillDefinition>> getAllSkills() {
        return ResponseEntity.ok(skillManager.getAllSkills());
    }

    /**
     * 获取指定Skill的详细信息（read_skill功能）
     */
    @GetMapping("/{skillName}")
    public ResponseEntity<String> getSkillDetail(@PathVariable String skillName) {
        String detail = skillManager.readSkill(skillName);
        return ResponseEntity.ok(detail);
    }

    /**
     * 执行指定Skill
     */
    @PostMapping("/{skillName}/execute")
    public ResponseEntity<SkillResult> executeSkill(
            @PathVariable String skillName,
            @RequestBody ExecuteSkillRequest request) {

        log.info("Executing skill: {} with params: {}", skillName, request.getParams());

        Map<String, Object> params = request.getParams() != null ?
                request.getParams() : new HashMap<>();

        // 确保有input参数
        if (!params.containsKey("input")) {
            params.put("input", request.getInput() != null ? request.getInput() : "");
        }

        SkillResult result = skillManager.executeSkill(skillName, params);
        return ResponseEntity.ok(result);
    }

    /**
     * 自动检测并执行Skill
     */
    @PostMapping("/auto-execute")
    public ResponseEntity<SkillResult> autoExecuteSkill(@RequestBody AutoExecuteRequest request) {
        log.info("Auto-executing skill for input: {}", request.getInput());

        SkillResult result = skillManager.tryAutoExecuteSkill(request.getUserId(), request.getInput());

        if (result == null) {
            return ResponseEntity.ok(SkillResult.error("没有匹配的Skill可以处理此请求"));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取系统提示词（包含Skill列表）
     */
    @GetMapping("/system-prompt/{userId}")
    public ResponseEntity<String> getSystemPrompt(@PathVariable Long userId) {
        String prompt = skillManager.buildCompleteSystemPrompt(
                "你是一个智能助手，可以帮助用户完成各种任务。", userId);
        return ResponseEntity.ok(prompt);
    }

    /**
     * 为用户启用/禁用Skill
     */
    @PostMapping("/{skillName}/toggle/{userId}")
    public ResponseEntity<Map<String, Object>> toggleSkill(
            @PathVariable String skillName,
            @PathVariable Long userId,
            @RequestParam boolean enable) {

        if (enable) {
            skillManager.enableSkill(userId, skillName);
        } else {
            skillManager.disableSkill(userId, skillName);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("skillName", skillName);
        response.put("userId", userId);
        response.put("enabled", enable);
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    /**
     * 处理read_skill调用
     */
    @PostMapping("/read-skill")
    public ResponseEntity<String> readSkill(@RequestBody ReadSkillRequest request) {
        String result = skillManager.processReadSkillCall(request.getInput());

        if (result == null) {
            return ResponseEntity.ok("输入中没有检测到read_skill调用");
        }

        return ResponseEntity.ok(result);
    }

    // ==================== 请求DTO ====================

    @Data
    public static class ExecuteSkillRequest {
        private String input;
        private Map<String, Object> params;
    }

    @Data
    public static class AutoExecuteRequest {
        private Long userId;
        private String input;
    }

    @Data
    public static class ReadSkillRequest {
        private String input;
    }
}

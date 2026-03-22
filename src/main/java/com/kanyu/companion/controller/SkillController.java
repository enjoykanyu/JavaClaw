package com.kanyu.companion.controller;

import com.kanyu.companion.service.SkillManager;
import com.kanyu.companion.skill.SkillDefinition;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Skill管理控制器
 * 用于管理用户Skill的启用/禁用
 */
@Slf4j
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillManager skillManager;

    /**
     * 获取所有Skill列表
     */
    @GetMapping
    public ResponseEntity<List<SkillInfo>> listSkills() {
        List<SkillDefinition> skills = skillManager.getAllSkills();

        List<SkillInfo> skillInfos = skills.stream()
                .map(this::toSkillInfo)
                .toList();

        return ResponseEntity.ok(skillInfos);
    }

    /**
     * 获取用户启用的Skill列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SkillInfo>> getUserSkills(@PathVariable Long userId) {
        List<SkillDefinition> skills = skillManager.getEnabledSkills(userId);

        List<SkillInfo> skillInfos = skills.stream()
                .map(skill -> {
                    SkillInfo info = toSkillInfo(skill);
                    info.setEnabled(skillManager.isSkillEnabled(userId, skill.getName()));
                    return info;
                })
                .toList();

        return ResponseEntity.ok(skillInfos);
    }

    /**
     * 为用户启用Skill
     */
    @PostMapping("/enable")
    public ResponseEntity<String> enableSkill(@RequestBody SkillRequest request) {
        skillManager.enableSkill(request.getUserId(), request.getSkillName());
        return ResponseEntity.ok("Skill enabled: " + request.getSkillName());
    }

    /**
     * 为用户禁用Skill
     */
    @PostMapping("/disable")
    public ResponseEntity<String> disableSkill(@RequestBody SkillRequest request) {
        skillManager.disableSkill(request.getUserId(), request.getSkillName());
        return ResponseEntity.ok("Skill disabled: " + request.getSkillName());
    }

    /**
     * 重置用户Skill设置
     */
    @PostMapping("/reset/{userId}")
    public ResponseEntity<String> resetUserSkills(@PathVariable Long userId) {
        skillManager.resetUserSkills(userId);
        return ResponseEntity.ok("Skills reset for user: " + userId);
    }

    /**
     * 获取Skill详细信息（read_skill功能）
     */
    @GetMapping("/{skillName}")
    public ResponseEntity<String> getSkillDetail(@PathVariable String skillName) {
        String detail = skillManager.readSkill(skillName);
        return ResponseEntity.ok(detail);
    }

    private SkillInfo toSkillInfo(SkillDefinition skill) {
        SkillInfo info = new SkillInfo();
        info.setName(skill.getName());
        info.setDescription(skill.getDescription());
        info.setEnabled(true);
        info.setParameters(skill.getParameters() != null ? skill.getParameters().size() : 0);
        return info;
    }

    @Data
    public static class SkillInfo {
        private String name;
        private String description;
        private boolean enabled;
        private int parameters;
    }

    @Data
    public static class SkillRequest {
        private Long userId;
        private String skillName;
    }
}

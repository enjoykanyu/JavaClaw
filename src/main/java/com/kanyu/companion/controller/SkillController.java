package com.kanyu.companion.controller;

import com.kanyu.companion.service.SkillManager;
import com.kanyu.companion.skill.Skill;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {
    
    private final SkillManager skillManager;
    
    @GetMapping
    public ResponseEntity<List<SkillInfo>> listSkills() {
        List<Skill> skills = skillManager.getAllSkills();
        
        List<SkillInfo> skillInfos = skills.stream()
            .map(this::toSkillInfo)
            .toList();
        
        return ResponseEntity.ok(skillInfos);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SkillInfo>> getUserSkills(@PathVariable Long userId) {
        List<Skill> skills = skillManager.getEnabledSkills(userId);
        
        List<SkillInfo> skillInfos = skills.stream()
            .map(skill -> {
                SkillInfo info = toSkillInfo(skill);
                info.setEnabled(skillManager.isSkillEnabled(userId, skill.getName()));
                return info;
            })
            .toList();
        
        return ResponseEntity.ok(skillInfos);
    }
    
    @PostMapping("/enable")
    public ResponseEntity<String> enableSkill(@RequestBody SkillRequest request) {
        skillManager.enableSkill(request.getUserId(), request.getSkillName());
        return ResponseEntity.ok("Skill enabled: " + request.getSkillName());
    }
    
    @PostMapping("/disable")
    public ResponseEntity<String> disableSkill(@RequestBody SkillRequest request) {
        skillManager.disableSkill(request.getUserId(), request.getSkillName());
        return ResponseEntity.ok("Skill disabled: " + request.getSkillName());
    }
    
    @PostMapping("/reset/{userId}")
    public ResponseEntity<String> resetUserSkills(@PathVariable Long userId) {
        skillManager.resetUserSkills(userId);
        return ResponseEntity.ok("Skills reset for user: " + userId);
    }
    
    private SkillInfo toSkillInfo(Skill skill) {
        SkillInfo info = new SkillInfo();
        info.setName(skill.getName());
        info.setDescription(skill.getDescription());
        info.setEnabled(true);
        info.setPriority(skill.getPriority());
        info.setTriggers(skill.getTriggers());
        return info;
    }
    
    @Data
    public static class SkillInfo {
        private String name;
        private String description;
        private boolean enabled;
        private int priority;
        private String[] triggers;
    }
    
    @Data
    public static class SkillRequest {
        private Long userId;
        private String skillName;
    }
}

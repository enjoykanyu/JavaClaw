package com.kanyu.companion.service;

import com.kanyu.companion.skill.Skill;
import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SkillManager {
    
    private final List<Skill> allSkills = new ArrayList<>();
    private final Map<Long, Set<String>> userEnabledSkills = new HashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("SkillManager initialized with {} skills", allSkills.size());
    }
    
    public void registerSkill(Skill skill) {
        allSkills.add(skill);
        log.info("Registered skill: {}", skill.getName());
    }
    
    public void registerSkills(List<Skill> skills) {
        allSkills.addAll(skills);
        log.info("Registered {} skills", skills.size());
    }
    
    public List<Skill> getAllSkills() {
        return new ArrayList<>(allSkills);
    }
    
    public List<Skill> getEnabledSkills(Long userId) {
        Set<String> enabledSkillNames = userEnabledSkills.getOrDefault(userId, 
            allSkills.stream().map(Skill::getName).collect(Collectors.toSet()));
        
        return allSkills.stream()
            .filter(skill -> enabledSkillNames.contains(skill.getName()))
            .sorted(Comparator.comparingInt(Skill::getPriority).reversed())
            .collect(Collectors.toList());
    }
    
    public GraphState executeSkills(GraphState state) {
        String input = state.getUserInput();
        Long userId = state.get("userId");
        
        if (input == null || input.isEmpty()) {
            return state;
        }
        
        List<Skill> enabledSkills = getEnabledSkills(userId);
        
        for (Skill skill : enabledSkills) {
            try {
                if (skill.canHandle(input, state)) {
                    log.info("Executing skill: {} for user: {}", skill.getName(), userId);
                    state = skill.execute(state);
                    
                    if (state.get("skill_response") != null) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Skill execution failed: {}", skill.getName(), e);
            }
        }
        
        return state;
    }
    
    public void enableSkill(Long userId, String skillName) {
        userEnabledSkills.computeIfAbsent(userId, k -> new HashSet<>()).add(skillName);
        log.info("Enabled skill {} for user {}", skillName, userId);
    }
    
    public void disableSkill(Long userId, String skillName) {
        userEnabledSkills.computeIfAbsent(userId, k -> new HashSet<>()).remove(skillName);
        log.info("Disabled skill {} for user {}", skillName, userId);
    }
    
    public boolean isSkillEnabled(Long userId, String skillName) {
        Set<String> enabled = userEnabledSkills.get(userId);
        if (enabled == null) {
            return true;
        }
        return enabled.contains(skillName);
    }
    
    public void resetUserSkills(Long userId) {
        userEnabledSkills.remove(userId);
        log.info("Reset skills for user {}", userId);
    }
    
    public Map<String, Skill> getSkillMap() {
        return allSkills.stream()
            .collect(Collectors.toMap(Skill::getName, skill -> skill));
    }
}

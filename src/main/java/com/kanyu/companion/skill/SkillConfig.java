package com.kanyu.companion.skill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillConfig {
    
    private String name;
    
    private boolean enabled = true;
    
    private int priority = 100;
    
    private Map<String, Object> parameters = new HashMap<>();
    
    public static SkillConfig defaultConfig(String name) {
        return new SkillConfig(name, true, 100, new HashMap<>());
    }
    
    public static SkillConfig disabled(String name) {
        return new SkillConfig(name, false, 100, new HashMap<>());
    }
}

package com.kanyu.companion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillInfo {
    
    private String name;
    
    private String description;
    
    private boolean enabled;
    
    private int priority;
    
    private Map<String, Object> parameters;
    
    private String[] triggers;
}

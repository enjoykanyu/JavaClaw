package com.kanyu.companion.skill;

import com.kanyu.graph.state.GraphState;

import java.util.Map;

public interface Skill {
    
    String getName();
    
    String getDescription();
    
    boolean canHandle(String input, GraphState state);
    
    GraphState execute(GraphState state);
    
    SkillConfig getConfig();
    
    default int getPriority() {
        return 100;
    }
    
    default String[] getTriggers() {
        return new String[0];
    }
}

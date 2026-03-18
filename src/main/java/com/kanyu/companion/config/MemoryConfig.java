package com.kanyu.companion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "companion.memory")
public class MemoryConfig {
    
    private int shortTermLimit = 10;
    
    private float longTermThreshold = 0.8f;
    
    private long consolidationInterval = 3600000;
    
    private int maxMemoryAge = 30;
    
    private float importanceThreshold = 0.7f;
}

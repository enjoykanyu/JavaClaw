package com.kanyu.companion.mcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "mcp")
public class McpConfig {
    
    private boolean enabled = true;
    
    private Map<String, ServerConfig> servers = new HashMap<>();
    
    @Data
    public static class ServerConfig {
        private boolean enabled = true;
        private String type;
        private String command;
        private String args;
        private String url;
        private Map<String, String> env = new HashMap<>();
    }
    
    public boolean isServerEnabled(String serverName) {
        ServerConfig config = servers.get(serverName);
        return config != null && config.isEnabled();
    }
}

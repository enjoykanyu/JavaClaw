package com.example.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图编排配置类 - Ollama 本地部署
 */
@Configuration
public class GraphConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    /**
     * 配置 Ollama ChatModel
     */
    @Bean
    public ChatModel chatModel() {
        OllamaApi ollamaApi = new OllamaApi(baseUrl);
        return new OllamaChatModel(ollamaApi);
    }
}

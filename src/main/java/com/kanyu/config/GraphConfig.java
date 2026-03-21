package com.kanyu.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
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

    @Value("${spring.ai.ollama.chat.model:qwen3:1.7b}")
    private String model;

    /**
     * 配置 Ollama ChatModel
     */
    @Bean
    public ChatModel chatModel() {
        OllamaApi ollamaApi = new OllamaApi(baseUrl);
        OllamaOptions options = OllamaOptions.builder()
                .withModel(model)
                .build();
        return new OllamaChatModel(ollamaApi, options);
    }
}

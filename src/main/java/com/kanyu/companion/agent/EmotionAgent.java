package com.kanyu.companion.agent;

import com.kanyu.graph.state.GraphState;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 情感分析Agent
 * 符合Spring AI Alibaba规范的情感分析Agent
 */
@Slf4j
@Component
public class EmotionAgent implements AgentExecutor {

    private final ChatModel chatModel;
    private final AgentRegistry agentRegistry;

    private AgentDefinition definition;

    private static final String EMOTION_ANALYSIS_PROMPT = """
        分析以下文本的情感状态，返回JSON格式的结果。

        文本：%s

        请返回以下格式的JSON：
        {
            "primary_emotion": "主要情感（happy/sad/angry/anxious/calm/neutral）",
            "emotion_score": 情感强度（0.0-1.0）,
            "sentiment": "情感倾向（positive/negative/neutral）",
            "keywords": ["关键词1", "关键词2"],
            "needs_support": 是否需要情感支持（true/false）,
            "suggested_response_type": "建议的回应类型（empathetic/cheerful/encouraging/neutral）"
        }

        只返回JSON，不要其他内容。
        """;

    public EmotionAgent(ChatModel chatModel, AgentRegistry agentRegistry) {
        this.chatModel = chatModel;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        // 构建Agent定义
        this.definition = AgentDefinition.builder()
                .name("emotion_analyzer")
                .description("情感分析Agent，分析用户输入的情感状态")
                .detailedDescription("""
                    这是一个专业的情感分析Agent，擅长分析文本中的情感状态。

                    能力：
                    1. 识别主要情感（开心、悲伤、愤怒、焦虑、平静、中性）
                    2. 评估情感强度
                    3. 判断情感倾向（正面/负面/中性）
                    4. 提取情感关键词
                    5. 判断是否需要情感支持
                    6. 建议回应类型
                    """)
                .role("情感分析专家")
                .capabilities(new String[]{
                        "情感识别",
                        "情感强度评估",
                        "关键词提取",
                        "支持需求判断"
                })
                .systemPromptTemplate("你是一个专业的情感分析助手，擅长分析文本中的情感状态。")
                .build();

        // 注册到AgentRegistry
        agentRegistry.registerAgent(definition, this);
        log.info("EmotionAgent registered to AgentRegistry");
    }

    @Override
    public AgentDefinition getDefinition() {
        return definition;
    }

    @Override
    public GraphState execute(GraphState state) {
        log.info("Executing EmotionAgent");

        try {
            String userInput = state.getUserInput();
            if (userInput == null || userInput.isEmpty()) {
                return state;
            }

            String analysisPrompt = String.format(EMOTION_ANALYSIS_PROMPT, userInput);

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的情感分析助手，擅长分析文本中的情感状态。"));
            messages.add(new SystemMessage(analysisPrompt));

            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);

            if (response != null && response.getResult() != null) {
                String responseText = response.getResult().getOutput().getContent();

                Map<String, Object> emotionResult = parseEmotionResult(responseText);
                state.put("emotion_analysis", emotionResult);

                log.debug("Emotion analysis result: {}", emotionResult);
            }

        } catch (Exception e) {
            log.error("EmotionAgent execution failed", e);
            state.put("emotion_analysis", getDefaultEmotionResult());
        }

        return state;
    }

    /**
     * 解析情感分析结果
     */
    private Map<String, Object> parseEmotionResult(String responseText) {
        Map<String, Object> result = new HashMap<>();

        try {
            String json = responseText;
            if (responseText.contains("```json")) {
                json = responseText.substring(responseText.indexOf("{"), responseText.lastIndexOf("}") + 1);
            } else if (responseText.contains("{")) {
                json = responseText.substring(responseText.indexOf("{"), responseText.lastIndexOf("}") + 1);
            }

            json = json.trim();

            result.put("primary_emotion", extractValue(json, "primary_emotion", "neutral"));
            result.put("emotion_score", extractFloatValue(json, "emotion_score", 0.5f));
            result.put("sentiment", extractValue(json, "sentiment", "neutral"));
            result.put("needs_support", extractBooleanValue(json, "needs_support", false));
            result.put("suggested_response_type", extractValue(json, "suggested_response_type", "neutral"));

        } catch (Exception e) {
            log.warn("Failed to parse emotion result, using default", e);
            return getDefaultEmotionResult();
        }

        return result;
    }

    /**
     * 获取默认情感结果
     */
    private Map<String, Object> getDefaultEmotionResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("primary_emotion", "neutral");
        result.put("emotion_score", 0.5f);
        result.put("sentiment", "neutral");
        result.put("needs_support", false);
        result.put("suggested_response_type", "neutral");
        return result;
    }

    private String extractValue(String json, String key, String defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.warn("Failed to extract value for key: {}", key);
        }
        return defaultValue;
    }

    private float extractFloatValue(String json, String key, float defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return Float.parseFloat(m.group(1));
            }
        } catch (Exception e) {
            log.warn("Failed to extract float value for key: {}", key);
        }
        return defaultValue;
    }

    private boolean extractBooleanValue(String json, String key, boolean defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(1));
            }
        } catch (Exception e) {
            log.warn("Failed to extract boolean value for key: {}", key);
        }
        return defaultValue;
    }
}

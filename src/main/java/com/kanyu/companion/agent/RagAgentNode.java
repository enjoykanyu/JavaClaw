package com.kanyu.companion.agent;

import com.kanyu.companion.service.RagService;
import com.kanyu.graph.state.GraphState;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG知识检索Agent
 * 符合Spring AI Alibaba规范的RAG Agent
 */
@Slf4j
@Component
public class RagAgentNode implements AgentExecutor {

    private final ChatModel chatModel;
    private final RagService ragService;
    private final AgentRegistry agentRegistry;

    private AgentDefinition definition;

    private static final String RAG_PROMPT_TEMPLATE = """
        基于以下参考资料回答用户的问题。如果参考资料中没有相关信息，请根据你自己的知识回答。

        参考资料：
        %s

        用户问题：%s

        请用友好、符合角色设定的方式回答问题。如果引用了参考资料，可以适当提及。
        """;

    public RagAgentNode(ChatModel chatModel, RagService ragService, AgentRegistry agentRegistry) {
        this.chatModel = chatModel;
        this.ragService = ragService;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        // 构建Agent定义
        this.definition = AgentDefinition.builder()
                .name("rag_retriever")
                .description("RAG知识检索Agent，基于知识库回答问题")
                .detailedDescription("""
                    这是一个基于检索增强生成(RAG)的知识检索Agent。

                    能力：
                    1. 从知识库中检索相关信息
                    2. 基于检索到的资料回答问题
                    3. 融合外部知识和内部知识
                    4. 提供准确的参考资料引用
                    """)
                .role("知识检索专家")
                .capabilities(new String[]{
                        "知识库检索",
                        "上下文理解",
                        "答案生成",
                        "资料引用"
                })
                .systemPromptTemplate("你是一个知识渊博的助手，擅长基于提供的参考资料回答问题。")
                .build();

        // 注册到AgentRegistry
        agentRegistry.registerAgent(definition, this);
        log.info("RagAgentNode registered to AgentRegistry");
    }

    @Override
    public AgentDefinition getDefinition() {
        return definition;
    }

    @Override
    public GraphState execute(GraphState state) {
        log.info("Executing RagAgentNode");

        try {
            Long userId = state.get("userId");
            String userInput = state.getUserInput();

            String context = ragService.queryWithContext(userInput, userId);

            String systemPrompt = definition.getSystemPromptTemplate();

            String userPrompt;
            if (context != null && !context.isEmpty()) {
                userPrompt = String.format(RAG_PROMPT_TEMPLATE, context, userInput);
            } else {
                userPrompt = userInput;
            }

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(userPrompt));

            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);

            String responseText = "";
            if (response != null && response.getResult() != null) {
                responseText = response.getResult().getOutput().getContent();

                state.addMessage(new UserMessage(userInput));
                state.addMessage(new AssistantMessage(responseText));
            }

            state.put("rag_response", responseText);
            state.put("rag_used", context != null && !context.isEmpty());

            log.debug("RagAgentNode response: {}", responseText);

        } catch (Exception e) {
            log.error("RagAgentNode execution failed", e);
            state.setError("RAG agent failed: " + e.getMessage());
        }

        return state;
    }
}

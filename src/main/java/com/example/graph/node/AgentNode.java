package com.example.graph.node;

import com.example.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体节点
 * 使用 ChatModel 处理对话逻辑
 */
@Slf4j
public class AgentNode implements Node {

    private final String id;
    private final String name;
    private final ChatModel chatModel;
    private final String systemPrompt;
    private final List<ToolCallback> tools;

    public AgentNode(String id, String name, ChatModel chatModel, String systemPrompt) {
        this(id, name, chatModel, systemPrompt, new ArrayList<>());
    }

    public AgentNode(String id, String name, ChatModel chatModel, String systemPrompt, List<ToolCallback> tools) {
        this.id = id;
        this.name = name;
        this.chatModel = chatModel;
        this.systemPrompt = systemPrompt;
        this.tools = tools != null ? tools : new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GraphState execute(GraphState state) {
        log.info("执行智能体节点: {}", name);

        try {
            // 构建消息列表
            List<Message> messages = new ArrayList<>();

            // 添加系统提示
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }

            // 添加历史消息
            messages.addAll(state.getMessages());

            // 添加当前用户输入
            String userInput = state.getUserInput();
            if (userInput == null || userInput.isEmpty()) {
                userInput = state.get("input");
            }

            if (userInput != null && !userInput.isEmpty()) {
                messages.add(new UserMessage(userInput));
            }

            // 创建 Prompt
            Prompt prompt = new Prompt(messages);

            // 调用模型
            ChatResponse response;
            if (!tools.isEmpty()) {
                // 如果有工具，使用工具调用
                response = chatModel.call(prompt, tools.toArray(new ToolCallback[0]));
            } else {
                response = chatModel.call(prompt);
            }

            // 获取响应内容
            String responseText = "";
            if (response != null && response.getResult() != null) {
                AssistantMessage assistantMessage = response.getResult().getOutput();
                responseText = assistantMessage.getText();

                // 添加到消息历史
                state.addMessage(new UserMessage(userInput));
                state.addMessage(assistantMessage);
            }

            // 存储结果到状态
            state.put("agent_response", responseText);
            state.put("last_agent", id);

            log.debug("智能体节点完成，响应: {}", responseText);

        } catch (Exception e) {
            log.error("智能体节点执行失败", e);
            state.setError("Agent execution failed: " + e.getMessage());
        }

        return state;
    }

    @Override
    public NodeType getType() {
        return NodeType.AGENT;
    }

    /**
     * 添加工具
     */
    public void addTool(ToolCallback tool) {
        this.tools.add(tool);
    }
}

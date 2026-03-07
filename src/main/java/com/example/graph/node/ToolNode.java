package com.example.graph.node;

import com.example.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolContext;

import java.util.Map;

/**
 * 工具节点
 * 用于调用外部工具/函数
 */
@Slf4j
public class ToolNode implements Node {

    private final String id;
    private final String name;
    private final ToolCallback tool;
    private final ToolInputProvider inputProvider;

    /**
     * 工具输入提供者接口
     */
    @FunctionalInterface
    public interface ToolInputProvider {
        String provide(GraphState state);
    }

    public ToolNode(String id, String name, ToolCallback tool, ToolInputProvider inputProvider) {
        this.id = id;
        this.name = name;
        this.tool = tool;
        this.inputProvider = inputProvider;
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
        log.info("执行工具节点: {}", name);

        try {
            // 获取工具输入
            String input = inputProvider != null ? inputProvider.provide(state) : state.getUserInput();

            // 创建工具上下文
            ToolContext toolContext = new ToolContext(Map.of());

            // 调用工具
            String result = tool.call(input, toolContext);

            // 存储结果
            state.put("tool_result", result);
            state.put("tool_name", name);

            log.debug("工具节点完成，结果: {}", result);

        } catch (Exception e) {
            log.error("工具节点执行失败", e);
            state.setError("Tool execution failed: " + e.getMessage());
        }

        return state;
    }

    @Override
    public NodeType getType() {
        return NodeType.TOOL;
    }
}

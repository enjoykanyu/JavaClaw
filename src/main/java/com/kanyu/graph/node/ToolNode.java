package com.kanyu.graph.node;

import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

/**
 * 工具节点
 * 用于调用外部工具/函数
 */
@Slf4j
public class ToolNode implements Node {

    private final String id;
    private final String name;
    private final Function<String, String> toolFunction;
    private final ToolInputProvider inputProvider;

    /**
     * 工具输入提供者接口
     */
    @FunctionalInterface
    public interface ToolInputProvider {
        String provide(GraphState state);
    }

    public ToolNode(String id, String name, Function<String, String> toolFunction, ToolInputProvider inputProvider) {
        this.id = id;
        this.name = name;
        this.toolFunction = toolFunction;
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

            // 调用工具函数
            String result = toolFunction.apply(input);

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

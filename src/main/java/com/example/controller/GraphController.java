package com.example.controller;

import com.example.graph.core.StateGraph;
import com.example.graph.node.*;
import com.example.graph.state.GraphState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.BiFunction;

/**
 * 图编排 REST API - Ollama 本地部署版
 */
@Slf4j
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final ChatModel chatModel;

    public GraphController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 简单图编排执行
     * Start -> Agent -> End
     */
    @PostMapping("/execute")
    public ResponseEntity<GraphResponse> executeGraph(@RequestBody GraphRequest request) {
        log.info("执行图编排请求: {}", request.getInput());

        try {
            // 创建图
            StateGraph graph = new StateGraph();

            // 创建节点
            StartNode startNode = new StartNode("start", "开始节点");
            AgentNode agentNode = new AgentNode(
                "agent",
                "智能体节点",
                chatModel,
                request.getSystemPrompt() != null ? request.getSystemPrompt() : "你是一个友好的助手，用中文回答。"
            );
            EndNode endNode = new EndNode("end", "结束节点");

            // 构建图
            graph.addNode(startNode)
                 .addNode(agentNode)
                 .addNode(endNode)
                 .setStartNode("start")
                 .setEndNode("end")
                 .addEdge("start", "agent")
                 .addEdge("agent", "end");

            // 执行
            GraphState state = graph.execute(request.getInput());

            // 构建响应
            GraphResponse response = new GraphResponse();
            response.setSuccess(state.getError() == null);
            response.setResult(state.getResult());
            response.setError(state.getError());
            response.setFinished(state.isFinished());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("图编排执行失败", e);
            GraphResponse response = new GraphResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 带工具的图编排执行
     */
    @PostMapping("/execute-with-tools")
    public ResponseEntity<GraphResponse> executeGraphWithTools(@RequestBody GraphRequest request) {
        log.info("执行带工具的图编排请求: {}", request.getInput());

        try {
            // 创建工具
            ToolCallback weatherTool = createWeatherTool();

            // 创建图
            StateGraph graph = new StateGraph();

            AgentNode agentNode = new AgentNode(
                "weather_agent",
                "天气助手",
                chatModel,
                """
                你是天气助手。当用户询问天气时，使用 get_weather 工具查询。
                用中文回复，可以适当幽默。
                """,
                java.util.List.of(weatherTool)
            );

            graph.addNode(new StartNode("start", "开始"))
                 .addNode(agentNode)
                 .addNode(new EndNode("end", "结束"))
                 .setStartNode("start")
                 .setEndNode("end")
                 .addEdge("start", "weather_agent")
                 .addEdge("weather_agent", "end");

            GraphState state = graph.execute(request.getInput());

            GraphResponse response = new GraphResponse();
            response.setSuccess(state.getError() == null);
            response.setResult(state.getResult());
            response.setError(state.getError());
            response.setFinished(state.isFinished());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("图编排执行失败", e);
            GraphResponse response = new GraphResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取图结构信息
     */
    @GetMapping("/structure")
    public ResponseEntity<String> getGraphStructure() {
        StateGraph graph = new StateGraph();

        // 构建示例图
        graph.addNode(new StartNode("start", "开始"))
             .addNode(new AgentNode("agent1", "Agent A", chatModel, "助手A"))
             .addNode(new AgentNode("agent2", "Agent B", chatModel, "助手B"))
             .addNode(new EndNode("end", "结束"))
             .setStartNode("start")
             .setEndNode("end")
             .addEdge("start", "agent1")
             .addEdge("agent1", "agent2")
             .addEdge("agent2", "end");

        StringBuilder sb = new StringBuilder();
        sb.append("图结构信息:\n");
        sb.append("节点:\n");
        for (var node : graph.getNodes()) {
            sb.append("  - ").append(node.getId())
              .append(" (").append(node.getType()).append(")\n");
        }
        sb.append("边:\n");
        for (var edge : graph.getAllEdges()) {
            sb.append("  - ").append(edge.getSourceNodeId())
              .append(" -> ").append(edge.getTargetNodeId());
            if (edge.getName() != null) {
                sb.append(" [").append(edge.getName()).append("]");
            }
            sb.append("\n");
        }

        return ResponseEntity.ok(sb.toString());
    }

    /**
     * 创建天气工具
     */
    private ToolCallback createWeatherTool() {
        BiFunction<WeatherRequest, ToolContext, String> weatherFunction = (request, context) -> {
            log.info("[工具调用] 查询天气: 城市={}", request.city());
            return String.format("%s 今天天气晴朗，温度 25°C，适宜出行", request.city());
        };

        return FunctionToolCallback.builder("get_weather", weatherFunction)
            .description("查询指定城市的天气信息")
            .inputType(WeatherRequest.class)
            .build();
    }

    // 请求/响应类
    @Data
    public static class GraphRequest {
        private String input;
        private String systemPrompt;
    }

    @Data
    public static class GraphResponse {
        private boolean success;
        private String result;
        private String error;
        private boolean finished;
    }

    public record WeatherRequest(
        @ToolParam(description = "城市名称") String city
    ) {}
}

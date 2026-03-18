package com.kanyu.controller;

import com.kanyu.graph.core.StateGraph;
import com.kanyu.graph.node.*;
import com.kanyu.graph.state.GraphState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * 意图识别测试接口
     * 测试输入扩展和意图识别功能
     */
    @PostMapping("/intent")
    public ResponseEntity<IntentResponse> recognizeIntent(@RequestBody GraphRequest request) {
        log.info("意图识别请求: {}", request.getInput());

        try {
            // 1. 输入扩展
            String expandedInput = expandInput(request.getInput());
            log.info("扩展后的输入: {}", expandedInput);

            // 2. 意图识别
            IntentResult intentResult = performIntentRecognition(expandedInput);
            log.info("识别结果: intent={}, confidence={}", intentResult.intent(), intentResult.confidence());

            IntentResponse response = new IntentResponse();
            response.setSuccess(true);
            response.setOriginalInput(request.getInput());
            response.setExpandedInput(expandedInput);
            response.setIntent(intentResult.intent());
            response.setConfidence(intentResult.confidence());
            response.setDetails(intentResult.details());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("意图识别失败", e);
            IntentResponse response = new IntentResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 带意图识别的完整图编排流程
     */
    @PostMapping("/execute-with-intent")
    public ResponseEntity<GraphResponse> executeWithIntent(@RequestBody GraphRequest request) {
        log.info("执行意图驱动图编排: {}", request.getInput());

        try {
            // 1. 输入扩展
            String expandedInput = expandInput(request.getInput());
            
            // 2. 意图识别
            IntentResult intentResult = performIntentRecognition(expandedInput);
            log.info("识别到意图: {}", intentResult.intent());

            // 3. 根据意图路由到不同处理流程
            StateGraph graph = new StateGraph();
            String systemPrompt;
            
            switch (intentResult.intent()) {
                case "rag":
                    systemPrompt = "你是知识库助手，基于检索到的知识回答问题。";
                    break;
                case "creative":
                    systemPrompt = "你是创作助手，帮助用户进行内容创作。";
                    break;
                case "report":
                    systemPrompt = "你是报表分析助手，帮助用户分析数据并生成报表。";
                    break;
                case "weather":
                    systemPrompt = "你是天气助手，回答天气相关问题。";
                    break;
                default:
                    systemPrompt = "你是一个友好的助手，用中文回答用户问题。";
            }

            // 构建图
            graph.addNode(new StartNode("start", "开始"))
                 .addNode(new AgentNode("agent", "智能体", chatModel, systemPrompt))
                 .addNode(new EndNode("end", "结束"))
                 .setStartNode("start")
                 .setEndNode("end")
                 .addEdge("start", "agent")
                 .addEdge("agent", "end");

            GraphState state = graph.execute(expandedInput);

            GraphResponse response = new GraphResponse();
            response.setSuccess(state.getError() == null);
            response.setResult(state.getResult());
            response.setError(state.getError());
            response.setFinished(state.isFinished());
            response.setIntent(intentResult.intent());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("意图驱动图编排执行失败", e);
            GraphResponse response = new GraphResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 输入扩展 - 增强用户输入
     */
    private String expandInput(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 简单的输入扩展逻辑
        // 实际项目中可以调用 LLM 进行更复杂的扩展
        StringBuilder expanded = new StringBuilder();
        expanded.append("用户原始问题: ").append(input).append("\n");
        expanded.append("上下文: 这是一个AI助手对话场景\n");
        expanded.append("时间: ").append(java.time.LocalDateTime.now()).append("\n");
        
        return expanded.toString();
    }

    /**
     * 执行意图识别
     */
    private IntentResult performIntentRecognition(String input) {
        // 基于关键词的简单意图识别
        // 实际项目中应该调用 LLM 进行意图识别
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("天气") || lowerInput.contains("温度") || lowerInput.contains("下雨")) {
            return new IntentResult("weather", 0.95, 
                java.util.Map.of("entities", java.util.List.of("天气查询")));
        }
        
        if (lowerInput.contains("知识") || lowerInput.contains("查询") || lowerInput.contains("什么是")) {
            return new IntentResult("rag", 0.88,
                java.util.Map.of("entities", java.util.List.of("知识查询")));
        }
        
        if (lowerInput.contains("创作") || lowerInput.contains("写作") || lowerInput.contains("生成")) {
            return new IntentResult("creative", 0.92,
                java.util.Map.of("entities", java.util.List.of("内容创作")));
        }
        
        if (lowerInput.contains("报表") || lowerInput.contains("分析") || lowerInput.contains("统计")) {
            return new IntentResult("report", 0.90,
                java.util.Map.of("entities", java.util.List.of("数据分析")));
        }
        
        return new IntentResult("general", 0.80,
            java.util.Map.of("entities", java.util.List.of("通用对话")));
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
        private String intent;
    }

    @Data
    public static class IntentResponse {
        private boolean success;
        private String originalInput;
        private String expandedInput;
        private String intent;
        private double confidence;
        private java.util.Map<String, Object> details;
        private String error;
    }

    // 意图识别结果记录
    private record IntentResult(String intent, double confidence, java.util.Map<String, Object> details) {}
}

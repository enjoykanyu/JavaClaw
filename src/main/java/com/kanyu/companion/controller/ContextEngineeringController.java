package com.kanyu.companion.controller;

import com.kanyu.companion.context.*;
import com.kanyu.companion.model.Memory;
import com.kanyu.companion.service.MemoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 上下文工程测试控制器
 * 
 * 用于测试和演示增强的上下文工程能力
 */
@Slf4j
@RestController
@RequestMapping("/api/context")
@RequiredArgsConstructor
public class ContextEngineeringController {

    private final PromptTemplateEngine templateEngine;
    private final TokenManager tokenManager;
    private final ContextSelector contextSelector;
    private final ContextManager contextManager;
    private final MemoryService memoryService;

    /**
     * 测试提示词模板引擎
     */
    @PostMapping("/template")
    public ResponseEntity<TemplateResponse> testTemplate(@RequestBody TemplateRequest request) {
        log.info("Testing template engine");

        try {
            // 验证模板
            PromptTemplateEngine.ValidationResult validation = 
                templateEngine.validate(request.getTemplate());

            // 渲染模板
            String rendered = templateEngine.render(
                request.getTemplate(), 
                request.getVariables()
            );

            TemplateResponse response = new TemplateResponse();
            response.setValid(validation.valid());
            response.setErrors(validation.errors());
            response.setWarnings(validation.warnings());
            response.setRendered(rendered);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Template rendering failed", e);
            TemplateResponse response = new TemplateResponse();
            response.setValid(false);
            response.setErrors(List.of(e.getMessage()));
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 测试Token估算
     */
    @PostMapping("/tokens")
    public ResponseEntity<TokenResponse> estimateTokens(@RequestBody TokenRequest request) {
        log.info("Estimating tokens");

        Map<String, Integer> results = tokenManager.estimateTokensBatch(request.getTexts());
        int total = results.values().stream().mapToInt(Integer::intValue).sum();

        TokenResponse response = new TokenResponse();
        response.setResults(results);
        response.setTotal(total);

        return ResponseEntity.ok(response);
    }

    /**
     * 测试Token预算
     */
    @GetMapping("/budget")
    public ResponseEntity<BudgetResponse> getTokenBudget(
            @RequestParam(defaultValue = "8000") int maxTokens) {
        log.info("Getting token budget for {} tokens", maxTokens);

        TokenManager.TokenBudget budget = tokenManager.createBudget(maxTokens);

        BudgetResponse response = new BudgetResponse();
        response.setMaxTokens(budget.maxTokens());
        response.setSystemPromptBudget(budget.systemPromptBudget());
        response.setHistoryBudget(budget.historyBudget());
        response.setMemoryBudget(budget.memoryBudget());
        response.setUserInputBudget(budget.userInputBudget());
        response.setTotalBudget(budget.getTotalBudget());

        return ResponseEntity.ok(response);
    }

    /**
     * 测试上下文选择
     */
    @PostMapping("/select")
    public ResponseEntity<SelectionResponse> testContextSelection(
            @RequestBody SelectionRequest request) {
        log.info("Testing context selection");

        // 构建测试消息列表
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < request.getMessageContents().size(); i++) {
            String content = request.getMessageContents().get(i);
            if (i % 2 == 0) {
                messages.add(new UserMessage(content));
            } else {
                messages.add(new AssistantMessage(content));
            }
        }

        // 选择相关上下文
        List<Message> selected = contextSelector.selectRelevantContext(
            messages,
            request.getQuery(),
            request.getMaxTokens(),
            tokenManager
        );

        // 生成报告
        ContextSelector.SelectionReport report = contextSelector.generateSelectionReport(
            messages, selected, request.getQuery()
        );

        SelectionResponse response = new SelectionResponse();
        response.setOriginalCount(report.originalCount());
        response.setSelectedCount(report.selectedCount());
        response.setSelectionRate(report.selectionRate());
        response.setAverageScore(report.averageScore());
        response.setSelectedAverageScore(report.selectedAverageScore());
        response.setQualityImprovement(report.getQualityImprovement());
        response.setSelectedMessages(selected.stream()
            .map(Message::getContent)
            .toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 测试完整上下文构建
     */
    @PostMapping("/build")
    public ResponseEntity<ContextBuildResponse> buildContext(
            @RequestBody ContextBuildRequest request) {
        log.info("Building context");

        // 构建对话历史
        List<Message> history = new ArrayList<>();
        if (request.getHistory() != null) {
            for (int i = 0; i < request.getHistory().size(); i++) {
                String content = request.getHistory().get(i);
                if (i % 2 == 0) {
                    history.add(new UserMessage(content));
                } else {
                    history.add(new AssistantMessage(content));
                }
            }
        }

        // 构建上下文
        ContextManager.ContextResult result = contextManager.buildContext(
            request.getUserId(),
            request.getUserInput(),
            history,
            request.getSystemPromptTemplate(),
            request.getTemplateVariables()
        );

        ContextManager.ContextReport report = result.report();

        ContextBuildResponse response = new ContextBuildResponse();
        response.setTotalTokens(result.totalTokens());
        response.setMaxTokens(report.maxTokens());
        response.setUsagePercentage(report.getUsagePercentage());
        response.setSystemPromptTokens(report.systemPromptTokens());
        response.setMemoryTokens(report.memoryTokens());
        response.setHistoryTokens(report.historyTokens());
        response.setUserInputTokens(report.userInputTokens());
        response.setMemoryCount(report.memoryCount());
        response.setHistoryMessageCount(report.historyMessageCount());
        response.setCompressed(report.compressed());
        response.setOptimizations(report.optimizations());
        response.setWithinBudget(report.isWithinBudget());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取Token使用报告
     */
    @PostMapping("/usage-report")
    public ResponseEntity<TokenManager.TokenUsageReport> getUsageReport(
            @RequestBody UsageReportRequest request) {
        log.info("Generating usage report");

        Map<String, String> parts = new HashMap<>();
        parts.put("systemPrompt", request.getSystemPrompt());
        parts.put("memories", request.getMemories());
        parts.put("history", String.join("\n", request.getHistory()));
        parts.put("userInput", request.getUserInput());

        TokenManager.TokenUsageReport report = tokenManager.generateUsageReport(
            parts,
            tokenManager.createBudget(request.getMaxTokens())
        );

        return ResponseEntity.ok(report);
    }

    // ========== Request/Response Classes ==========

    @Data
    public static class TemplateRequest {
        private String template;
        private Map<String, Object> variables;
    }

    @Data
    public static class TemplateResponse {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private String rendered;
    }

    @Data
    public static class TokenRequest {
        private Map<String, String> texts;
    }

    @Data
    public static class TokenResponse {
        private Map<String, Integer> results;
        private int total;
    }

    @Data
    public static class BudgetResponse {
        private int maxTokens;
        private int systemPromptBudget;
        private int historyBudget;
        private int memoryBudget;
        private int userInputBudget;
        private int totalBudget;
    }

    @Data
    public static class SelectionRequest {
        private List<String> messageContents;
        private String query;
        private int maxTokens;
    }

    @Data
    public static class SelectionResponse {
        private int originalCount;
        private int selectedCount;
        private double selectionRate;
        private double averageScore;
        private double selectedAverageScore;
        private double qualityImprovement;
        private List<String> selectedMessages;
    }

    @Data
    public static class ContextBuildRequest {
        private Long userId;
        private String userInput;
        private List<String> history;
        private String systemPromptTemplate;
        private Map<String, Object> templateVariables;
    }

    @Data
    public static class ContextBuildResponse {
        private int totalTokens;
        private int maxTokens;
        private double usagePercentage;
        private int systemPromptTokens;
        private int memoryTokens;
        private int historyTokens;
        private int userInputTokens;
        private int memoryCount;
        private int historyMessageCount;
        private boolean compressed;
        private List<String> optimizations;
        private boolean withinBudget;
    }

    @Data
    public static class UsageReportRequest {
        private String systemPrompt;
        private String memories;
        private List<String> history;
        private String userInput;
        private int maxTokens;
    }
}

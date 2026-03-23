package com.kanyu.companion.controller;

import com.kanyu.companion.agent.react.ReactAgent;
import com.kanyu.companion.agent.react.ReactStep;
import com.kanyu.graph.state.GraphState;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ReactAgent测试控制器
 *
 * 用于测试和演示ReAct推理-行动循环模式
 */
@Slf4j
@RestController
@RequestMapping("/api/react")
@RequiredArgsConstructor
public class ReactAgentController {

    private final ReactAgent reactAgent;

    /**
     * 执行ReactAgent
     */
    @PostMapping("/execute")
    public ResponseEntity<ReactResponse> execute(@RequestBody ReactRequest request) {
        log.info("Executing ReactAgent: {}", request.getInput());

        try {
            // 构建初始状态
            GraphState state = new GraphState();
            state.put("userId", request.getUserId());
            state.setUserInput(request.getInput());

            // 执行Agent
            GraphState result = reactAgent.execute(state);

            // 构建响应
            ReactResponse response = new ReactResponse();
            response.setSuccess(true);
            response.setAnswer((String) result.get("react_response"));

            @SuppressWarnings("unchecked")
            List<ReactStep> history = (List<ReactStep>) result.get("react_history");
            response.setSteps(convertSteps(history));

            response.setIterations((Integer) result.get("react_iterations"));
            response.setExecutionTimeMs((Long) result.get("react_execution_time_ms"));
            response.setFinished((Boolean) result.get("react_finished"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("ReactAgent execution failed", e);
            ReactResponse response = new ReactResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 转换步骤为DTO
     */
    private List<StepDTO> convertSteps(List<ReactStep> steps) {
        if (steps == null) return new ArrayList<>();

        List<StepDTO> result = new ArrayList<>();
        for (ReactStep step : steps) {
            StepDTO dto = new StepDTO();
            dto.setStepNumber(step.getStepNumber());
            dto.setType(step.getType().name());
            dto.setContent(step.getContent());
            dto.setToolName(step.getToolName());
            dto.setToolParams(step.getToolParams());
            dto.setOutput(step.getOutput());
            dto.setExecutionTimeMs(step.getExecutionTimeMs());
            dto.setSuccess(step.isSuccess());
            dto.setError(step.getError());
            result.add(dto);
        }
        return result;
    }

    // ========== Request/Response Classes ==========

    @Data
    public static class ReactRequest {
        private Long userId;
        private String input;
    }

    @Data
    public static class ReactResponse {
        private boolean success;
        private String answer;
        private List<StepDTO> steps;
        private int iterations;
        private long executionTimeMs;
        private boolean finished;
        private String error;
    }

    @Data
    public static class StepDTO {
        private int stepNumber;
        private String type;
        private String content;
        private String toolName;
        private String toolParams;
        private String output;
        private long executionTimeMs;
        private boolean success;
        private String error;
    }
}

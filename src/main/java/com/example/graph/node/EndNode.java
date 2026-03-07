package com.example.graph.node;

import com.example.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;

/**
 * 结束节点
 * 作为图编排的出口点，整理最终结果
 */
@Slf4j
public class EndNode implements Node {

    private final String id;
    private final String name;

    public EndNode() {
        this("end", "End Node");
    }

    public EndNode(String id, String name) {
        this.id = id;
        this.name = name;
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
        log.info("执行结束节点: {}", name);

        // 如果没有结果，尝试从数据中获取
        if (state.getResult() == null) {
            String result = state.get("agent_response");
            if (result != null) {
                state.setResult(result);
            } else {
                state.setResult("执行完成");
            }
        }

        state.setFinished(true);
        state.put("endTime", System.currentTimeMillis());

        // 计算执行时间
        Long startTime = state.get("startTime");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            state.put("duration", duration);
            log.debug("执行耗时: {}ms", duration);
        }

        log.info("结束节点完成，结果: {}", state.getResult());
        return state;
    }

    @Override
    public NodeType getType() {
        return NodeType.END;
    }
}

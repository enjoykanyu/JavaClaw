package com.kanyu.graph.node;

import com.kanyu.graph.state.GraphState;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

/**
 * 条件节点
 * 根据条件判断执行不同的分支
 */
@Slf4j
public class ConditionNode implements Node {

    private final String id;
    private final String name;
    private final Function<GraphState, String> condition;

    public ConditionNode(String id, String name, Function<GraphState, String> condition) {
        this.id = id;
        this.name = name;
        this.condition = condition;
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
        log.info("执行条件节点: {}", name);

        try {
            // 执行条件判断
            String result = condition.apply(state);
            state.put("condition_result", result);

            log.debug("条件节点完成，结果: {}", result);
        } catch (Exception e) {
            log.error("条件节点执行失败", e);
            state.setError("Condition execution failed: " + e.getMessage());
        }

        return state;
    }

    @Override
    public NodeType getType() {
        return NodeType.CONDITION;
    }
}

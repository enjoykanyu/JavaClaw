# EinoAI - Spring AI Alibaba 图编排演示 (Ollama 本地部署版)

基于 Spring AI Alibaba 框架的图编排（Graph Orchestration）演示项目，使用 **Ollama 本地部署** 替代云端 API。

## 前置要求

### 1. 安装 Ollama

```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh

# Windows
# 下载安装包: https://ollama.com/download
```

### 2. 拉取模型

```bash
# 推荐模型
ollama pull qwen2.5:7b

# 或其他模型
ollama pull llama3.2
ollama pull deepseek-r1:7b
```

### 3. 启动 Ollama 服务

```bash
# 默认端口 11434
ollama serve
```

## 项目结构

```
EinoAI/
├── pom.xml                              # Maven 配置 (Spring AI Ollama)
├── src/main/
│   ├── java/com/example/
│   │   ├── EinoAiApplication.java       # 主应用入口
│   │   ├── config/
│   │   │   └── GraphConfig.java         # Ollama 配置
│   │   ├── controller/
│   │   │   └── GraphController.java     # REST API
│   │   ├── demo/
│   │   │   ├── GraphOrchestrationDemo.java  # 图编排演示
│   │   │   └── ReactAgentDemo.java      # ReactAgent 风格演示
│   │   └── graph/
│   │       ├── core/StateGraph.java     # 状态图核心类
│   │       ├── edge/Edge.java           # 边定义
│   │       ├── node/                    # 5种节点类型
│   │       │   ├── Node.java            # 节点接口
│   │       │   ├── StartNode.java       # 起始节点
│   │       │   ├── EndNode.java         # 结束节点
│   │       │   ├── AgentNode.java       # 智能体节点
│   │       │   ├── ToolNode.java        # 工具节点
│   │       │   └── ConditionNode.java   # 条件节点
│   │       └── state/GraphState.java    # 图状态
│   └── resources/
│       └── application.yml              # Ollama 配置
└── README.md
```

## 核心概念

### 1. StateGraph（状态图）
状态图是图编排的核心，用于定义节点和边的流转关系：

```java
StateGraph graph = new StateGraph();
graph.addNode(startNode)
     .addNode(agentNode)
     .addNode(endNode)
     .setStartNode("start")
     .setEndNode("end")
     .addEdge("start", "agent")
     .addEdge("agent", "end");
```

### 2. Node（节点）

| 节点类型 | 说明 | 用途 |
|---------|------|------|
| StartNode | 起始节点 | 图编排入口，初始化状态 |
| EndNode | 结束节点 | 图编排出口，整理结果 |
| AgentNode | 智能体节点 | 调用 LLM 处理对话 |
| ToolNode | 工具节点 | 调用外部工具/函数 |
| ConditionNode | 条件节点 | 根据条件分支流转 |

### 3. Edge（边）
边控制节点间的流转：

```java
// 普通边
graph.addEdge("start", "agent");

// 带名称的边
graph.addEdge("agent", "end", "完成");

// 条件边
graph.addEdge("condition", "agentA", "条件A", 
    state -> "A".equals(state.get("condition_result")));
```

### 4. GraphState（状态）
全局状态在节点间传递：

```java
public class GraphState {
    private String currentNodeId;    // 当前节点
    private String userInput;        // 用户输入
    private List<Message> messages;  // 对话历史
    private Map<String, Object> data; // 共享数据
    private boolean finished;        // 是否结束
    private String result;           // 最终结果
}
```

## 流程示例

### 简单线性流程
```
Start -> Agent -> End
```

### 带工具的 Agent 流程
```
Start -> Agent(with tools) -> End
```

### 条件分支流程
```
Start -> Condition -> AgentA/AgentB -> End
```

### ReactAgent 风格流程
```
Start -> Router -> [Tool/Agent] -> End
```

## 快速开始

### 1. 配置 Ollama

编辑 `application.yml`：

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434  # Ollama 服务地址
      chat:
        model: qwen2.5:7b               # 使用的模型
        options:
          temperature: 0.7
```

或通过环境变量配置：

```bash
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen2.5:7b
```

### 2. 运行项目

```bash
# 编译运行
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/eino-ai-graph-1.0.0.jar
```

### 3. 测试 API

```bash
# 简单图编排执行
curl -X POST http://localhost:8080/api/graph/execute \
  -H "Content-Type: application/json" \
  -d '{"input": "你好，请介绍一下自己"}'

# 带工具的图编排执行
curl -X POST http://localhost:8080/api/graph/execute-with-tools \
  -H "Content-Type: application/json" \
  -d '{"input": "北京今天天气怎么样？"}'

# 获取图结构信息
curl http://localhost:8080/api/graph/structure
```

## Ollama 常用命令

```bash
# 列出已下载的模型
ollama list

# 运行模型（交互式）
ollama run qwen2.5:7b

# 删除模型
ollama rm qwen2.5:7b

# 查看模型信息
ollama show qwen2.5:7b

# 拉取新模型
ollama pull llama3.2
```

## 推荐模型

| 模型 | 大小 | 特点 | 适用场景 |
|-----|------|------|---------|
| qwen2.5:7b | 4.7GB | 中文优秀 | 中文对话、工具调用 |
| llama3.2 | 2.0GB | 轻量快速 | 简单对话、快速响应 |
| deepseek-r1:7b | 4.7GB | 推理能力强 | 复杂推理任务 |
| qwen2.5:14b | 9.0GB | 性能更强 | 高质量回复 |

## 依赖版本

- Spring Boot: 3.3.0
- Spring AI: 1.0.0-M1
- Spring AI Alibaba: 1.1.2.0
- Java: 17+

## 参考文档

- [Spring AI Alibaba 快速开始](https://java2ai.com/docs/quick-start)
- [Spring AI Ollama 文档](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [Ollama 官方文档](https://github.com/ollama/ollama)

# JavaClaw - AI 智能助手系统

基于 Spring AI Alibaba 框架构建的 AI 智能助手应用，提供情感陪伴、记忆管理、知识检索和技能扩展等核心能力。支持 Ollama 本地部署，具备完整的用户个性化配置和定时互动功能。

## ✨ 核心特性

### 🧠 智能伴侣核心
- **个性化人设**：可自定义助手名称、性格、说话风格、与用户关系
- **情感交互**：基于大模型的自然对话，支持情感识别与回应
- **记忆系统**：短期对话记忆 + 长期重要记忆存储

### 💾 记忆管理
- **短期记忆**：维护最近对话上下文（默认10条）
- **长期记忆**：自动提取重要信息持久化存储
- **记忆检索**：对话时自动检索相关历史记忆

### 📚 RAG 知识检索
- **向量存储**：基于 Milvus 向量数据库存储知识
- **语义检索**：支持相似度搜索，召回相关知识
- **PDF 解析**：支持上传 PDF 文档构建知识库

### 🛠️ 技能系统
- **插件化架构**：可扩展的技能接口设计
- **内置技能**：天气查询、笑话、提醒等
- **MCP 工具**：支持 Model Context Protocol 工具注册

### ⏰ 定时互动
- **早晚问候**：可配置的定时问候推送
- **新闻推送**：定时获取并推送最新资讯

## 🏗️ 项目架构

```
EinoAI/
├── pom.xml                              # Maven 配置
├── src/main/
│   ├── java/com/kanyu/
│   │   ├── companion/                   # 伴侣系统核心
│   │   │   ├── agent/                   # 智能代理节点
│   │   │   │   ├── CompanionAgent.java  # 主伴侣代理
│   │   │   │   ├── EmotionAgent.java    # 情感分析代理
│   │   │   │   ├── MemoryAgent.java     # 记忆管理代理
│   │   │   │   ├── RagAgentNode.java    # RAG检索代理
│   │   │   │   └── McpToolNode.java     # MCP工具代理
│   │   │   ├── config/                  # 配置类
│   │   │   ├── controller/              # REST API
│   │   │   │   ├── CompanionController.java
│   │   │   │   ├── FullCompanionController.java
│   │   │   │   ├── MemoryController.java
│   │   │   │   ├── RagController.java
│   │   │   │   ├── SkillController.java
│   │   │   │   ├── TaskController.java
│   │   │   │   └── PdfController.java
│   │   │   ├── mcp/                     # MCP工具框架
│   │   │   │   ├── McpToolRegistry.java # 工具注册中心
│   │   │   │   ├── McpToolDefinition.java
│   │   │   │   └── McpConfig.java
│   │   │   ├── model/                   # 数据模型
│   │   │   │   ├── CompanionProfile.java
│   │   │   │   ├── Memory.java
│   │   │   │   ├── ChatMessage.java
│   │   │   │   ├── ChatSummary.java
│   │   │   │   ├── PersonalityTraits.java
│   │   │   │   ├── ScheduledTask.java
│   │   │   │   └── SkillInfo.java
│   │   │   ├── repository/              # 数据访问层
│   │   │   ├── scheduler/               # 定时任务
│   │   │   │   ├── GreetingScheduler.java
│   │   │   │   └── NewsPushScheduler.java
│   │   │   ├── service/                 # 业务逻辑
│   │   │   │   ├── CompanionService.java
│   │   │   │   ├── MemoryService.java
│   │   │   │   ├── RagService.java
│   │   │   │   ├── GreetingService.java
│   │   │   │   ├── ChatSummaryService.java
│   │   │   │   ├── NewsService.java
│   │   │   │   ├── PdfService.java
│   │   │   │   ├── TaskService.java
│   │   │   │   └── SkillManager.java
│   │   │   └── skill/                   # 技能系统
│   │   │       ├── Skill.java           # 技能接口
│   │   │       ├── SkillConfig.java
│   │   │       ├── WeatherSkill.java
│   │   │       ├── JokeSkill.java
│   │   │       └── ReminderSkill.java
│   │   └── graph/                       # 图编排引擎
│   │       ├── core/StateGraph.java     # 状态图核心
│   │       ├── edge/Edge.java           # 边定义
│   │       ├── node/                    # 节点类型
│   │       │   ├── Node.java
│   │       │   ├── StartNode.java
│   │       │   ├── EndNode.java
│   │       │   ├── AgentNode.java
│   │       │   ├── ToolNode.java
│   │       │   └── ConditionNode.java
│   │       └── state/GraphState.java    # 图状态
│   └── resources/
│       └── application.yml              # 应用配置
└── README.md
```

## 🚀 快速开始

### 前置要求

- Java 17+
- Maven 3.6+
- Ollama 服务
- Milvus 向量数据库（可选）
- Redis（可选）

### 1. 安装 Ollama 并拉取模型

```bash
# macOS
brew install ollama

# 拉取推荐模型
ollama pull qwen2.5:7b

# 启动服务
ollama serve
```

### 2. 配置应用

编辑 `application.yml`：

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5:7b

milvus:
  host: localhost
  port: 19530
```

### 3. 运行项目

```bash
mvn spring-boot:run
```

## 📡 API 接口

### 伴侣对话
```bash
curl -X POST http://localhost:8080/api/companion/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "message": "你好，今天过得怎么样？"
  }'
```

### 更新伴侣人设
```bash
curl -X PUT http://localhost:8080/api/companion/profile/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "小助手",
    "personality": "温柔体贴，善于倾听",
    "speakingStyle": "亲切自然，用词温暖",
    "relationship": "朋友"
  }'
```

### 上传知识文档
```bash
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@document.pdf" \
  -F "userId=1"
```

### 获取可用技能
```bash
curl http://localhost:8080/api/skills
```

## ⚙️ 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `companion.memory.short-term-limit` | 短期记忆条数 | 10 |
| `companion.memory.long-term-threshold` | 长期记忆阈值 | 0.8 |
| `companion.greeting.morning-cron` | 早安问候定时 | 0 0 8 * * ? |
| `companion.greeting.evening-cron` | 晚安问候定时 | 0 0 22 * * ? |
| `companion.news.push-cron` | 新闻推送定时 | 0 0 * * * ? |

## 🛠️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.0 | 基础框架 |
| Spring AI Alibaba | 1.1.2.0 | AI 框架 |
| Spring AI Ollama | 1.0.0-M2 | 本地模型 |
| Milvus SDK | 2.3.4 | 向量数据库 |
| Spring Data JPA | - | ORM |
| H2 Database | - | 开发数据库 |
| Redis | - | 缓存 |
| Lombok | 1.18.30 | 代码简化 |

## 📝 系统流程

```
用户输入
    ↓
[CompanionAgent] 构建系统提示（人设+记忆）
    ↓
[EmotionAgent] 情感分析（可选）
    ↓
[MemoryAgent] 检索相关记忆
    ↓
[RagAgentNode] 检索知识库（可选）
    ↓
[McpToolNode] 执行工具调用（可选）
    ↓
LLM 生成回复
    ↓
[MemoryAgent] 保存对话记忆
    ↓
返回给用户
```

## 🔮 未来规划

- [ ] 语音交互支持
- [ ] 多模态输入（图片、文件）
- [ ] 更丰富的技能插件生态
- [ ] 情感模型微调
- [ ] 多端同步（Web/移动端）

## 📄 许可证

MIT License

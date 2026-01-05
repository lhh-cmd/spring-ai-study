# Spring AI Study 项目

这是一个用于学习 Spring AI 的项目，采用标准的 Spring Web 架构设计。

## 项目简介

本项目是一个多模块的 Spring Boot 项目，用于学习和实践 Spring AI 相关技术。项目采用分层架构，将业务逻辑和 Web 层分离，便于维护和扩展。

## 项目结构

```
spring-ai-study/
├── pom.xml                          # 父 POM，管理所有子模块
├── spring-ai-study-service/         # Service 模块（业务逻辑层）
│   └── src/main/java/com/ai/haha/springaistudyservice/
│       ├── service/                 # Service 接口和实现
│       │   ├── AiStudyService.java
│       │   └── impl/
│       │       └── AiStudyServiceImpl.java
│       └── SpringAiStudyServiceApplication.java
├── spring-ai-study-web/             # Web 模块（控制器层）
│   └── src/main/java/com/ai/haha/springaistudyweb/
│       ├── controller/              # REST 控制器
│       │   └── AiStudyController.java
│       └── SpringAiStudyWebApplication.java  # 应用入口
├── coze-java-sdk/                   # Coze Java SDK 模块（外部 SDK）
└── langfuse-java-sdk/               # Langfuse Java SDK 模块（外部 SDK）
```

## 模块说明

### spring-ai-study-service
业务逻辑层模块，包含：
- `AiStudyService`: AI 学习服务接口
- `AiStudyServiceImpl`: AI 学习服务实现类

### spring-ai-study-web
Web 层模块，包含：
- `AiStudyController`: REST API 控制器
- `SpringAiStudyWebApplication`: 应用主入口类

### SDK 模块
- `coze-java-sdk`: Coze Java SDK（暂不使用）
- `langfuse-java-sdk`: Langfuse Java SDK（暂不使用）

## 技术栈

- **Java**: 17
- **Spring Boot**: 4.0.1
- **Spring Web**: RESTful API 支持
- **Lombok**: 简化 Java 代码
- **Maven**: 项目构建工具

## 快速开始

### 前置要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本

### 启动项目

1. **编译项目**
   ```bash
   mvn clean compile
   ```

2. **启动 Web 应用**
   ```bash
   cd spring-ai-study-web
   mvn spring-boot:run
   ```

   或者直接在 IDE 中运行 `SpringAiStudyWebApplication` 类的 `main` 方法。

3. **访问应用**
   
   应用启动后，默认运行在 `http://localhost:8080`

## API 接口

### 1. 获取欢迎信息

**请求**
```
GET /api/ai-study/welcome
```

**响应**
```
欢迎使用Spring AI学习项目！
```

### 2. 处理消息

**请求**
```
POST /api/ai-study/message
Content-Type: application/json

{
  "message": "你好"
}
```

**响应**
```
您发送的消息是: 你好，已成功处理！
```

## 项目架构

项目采用标准的分层架构：

- **Controller 层** (`spring-ai-study-web`): 处理 HTTP 请求，调用 Service 层
- **Service 层** (`spring-ai-study-service`): 实现业务逻辑
- **模块依赖**: Web 模块依赖 Service 模块

## 开发说明

- Web 模块的主应用类配置了包扫描，可以自动扫描 Service 模块的组件
- Service 模块使用 `@Service` 注解标记服务实现类
- Controller 使用 `@RestController` 和 `@RequestMapping` 注解定义 REST API

## 后续计划

- 集成 Spring AI 相关功能
- 添加更多 AI 学习相关的业务逻辑
- 完善错误处理和日志记录


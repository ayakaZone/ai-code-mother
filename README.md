# 易哒 AI 应用创建平台 🚀

## 项目简介

易哒 AI 应用创建平台是一个革命性的开发工具，让用户只需输入一句话就能生成完整的网站应用。通过先进的 AI 技术，平台能够理解用户需求并自动生成相应的代码，极大地降低了开发门槛和时间成本。

## 核心功能

### 🎯 智能代码生成
- **一句话生成网站**：用户只需描述需求，AI 就能生成完整的网站代码
- **多种代码类型**：支持生成 HTML 页面和多文件项目结构
- **实时流式输出**：生成过程实时展示，让用户了解进度

### 📦 一键打包部署
- **代码打包下载**：将生成的代码一键打包为 ZIP 文件
- **快速部署上线**：自动部署应用并提供访问地址
- **应用管理**：完整的应用生命周期管理（创建、更新、删除）

### 🛠 开发工具集成
- **智能工作流**：基于 LangGraph4j 的工作流引擎
- **代码质量检查**：自动检查生成代码的质量
- **图片资源收集**：智能收集和生成所需的图片资源

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5.4 |
| 编程语言 | Java | 21 |
| 数据库 | MySQL | - |
| 缓存 | Redis, Caffeine | - |
| AI 框架 | LangChain4j, LangGraph4j | 1.1.0, 1.6.0-rc2 |
| 前端 | 生成的 Vue 项目 | - |
| 存储 | 腾讯云 COS | - |
| 其他 | Selenium, Redisson | - |

## 快速开始

### 环境要求
- JDK 21+
- MySQL 5.7+
- Redis 6.0+
- Maven 3.8+

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <项目地址>
   cd ai-code-mother
   ```

2. **配置数据库**
   - 创建数据库 `neko_ai_code_mother`
   - 执行 `neko-ai-code-mother/sql/create_table.sql` 初始化表结构

3. **配置应用**
   - 修改 `neko-ai-code-mother/src/main/resources/application.yml` 中的数据库和 Redis 配置

4. **构建项目**
   ```bash
   cd neko-ai-code-mother
   mvn clean package
   ```

5. **启动应用**
   ```bash
   java -jar target/neko-ai-code-mother-0.0.1-SNAPSHOT.jar
   ```

6. **访问接口文档**
   - 浏览器打开 `http://localhost:8123/api/doc.html`

## 使用指南

### 创建应用
1. 调用 `POST /api/app/add` 接口创建应用
2. 输入应用名称和描述

### 生成代码
1. 调用 `GET /api/app/chat/gen/code` 接口
2. 提供应用 ID 和需求描述（例如："创建一个电商网站，包含商品列表和购物车功能"）
3. 接收实时生成的代码流

### 下载代码
1. 调用 `GET /api/app/download/{appId}` 接口
2. 下载打包好的代码文件

### 部署应用
1. 调用 `POST /api/app/deploy` 接口
2. 提供应用 ID
3. 获取部署后的访问地址

## 项目结构

```
neko-ai-code-mother/
├── src/
│   ├── main/
│   │   ├── java/com/neko/nekoaicodemother/
│   │   │   ├── ai/             # AI 相关功能
│   │   │   ├── core/           # 核心功能
│   │   │   ├── langgraph4j/    # 工作流引擎
│   │   │   ├── contorller/     # 控制器
│   │   │   ├── service/        # 服务层
│   │   │   ├── model/          # 数据模型
│   │   │   └── NekoAiCodeMotherApplication.java  # 应用入口
│   │   └── resources/
│   │       ├── prompt/         # AI 提示词
│   │       ├── mapper/         # MyBatis 映射
│   │       └── application.yml # 应用配置
│   └── test/                   # 测试代码
├── sql/                        # 数据库脚本
└── pom.xml                     # Maven 配置
```

## 核心模块

### AI 代码生成
- **AiCodeGeneratorFacade**：AI 代码生成的核心入口
- **AiCodeGenTypeRoutingService**：根据需求类型路由到不同的代码生成服务
- **CodeParser**：解析生成的代码
- **CodeFilesSaver**：保存生成的代码文件

### 工作流引擎
- **CodeGenWorkflow**：代码生成工作流
- **CodeGenConcurrentWorkflow**：并发代码生成工作流
- **PromptEnhancerNode**：提示词增强节点
- **CodeGeneratorNode**：代码生成节点
- **CodeQualityCheckNode**：代码质量检查节点
- **ImageCollectorNode**：图片资源收集节点
- **ProjectBuilderNode**：项目构建节点

### 应用管理
- **AppService**：应用管理服务
- **ProjectDownloadService**：项目下载服务
- **ScreenshotService**：网页截图服务

## 接口文档

项目使用 Knife4j 生成接口文档，启动后可通过 `http://localhost:8123/api/doc.html` 访问。

主要接口包括：

| 接口 | 方法 | 功能 |
|------|------|------|
| `/app/add` | POST | 创建应用 |
| `/app/chat/gen/code` | GET | AI 生成应用代码 |
| `/app/download/{appId}` | GET | 下载代码 |
| `/app/deploy` | POST | 应用部署 |
| `/app/delete` | POST | 删除应用 |
| `/app/update` | POST | 更新应用 |
| `/app/get/vo` | GET | 获取应用详情 |
| `/app/my/list/page/vo` | POST | 获取用户应用列表 |
| `/app/good/list/page/vo` | POST | 获取精选应用列表 |

## 注意事项

1. **API 速率限制**：AI 对话接口有速率限制，每用户每分钟最多 2 次请求
2. **代码生成时间**：复杂应用的代码生成可能需要较长时间，请耐心等待
3. **部署环境**：部署功能需要配置腾讯云 COS 存储服务
4. **浏览器驱动**：截图功能需要安装对应浏览器的驱动

## 贡献指南

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目！

## 许可证

MIT License

---

**易哒 AI 应用创建平台** - 让开发变得简单有趣！✨

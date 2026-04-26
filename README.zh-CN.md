# 🍜 Firmament Take-Out (苍穹外卖)

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-6.0+-DC382D?logo=redis)](https://redis.io/)
[![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5.15-red)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Language / 语言:** [English](README.md) · **简体中文**（当前）

> 基于 **Spring Boot 3** 开发的全功能外卖管理系统，采用现代化技术栈。🚀

---

## 📋 目录

- [🔭 项目简介](#-项目简介)
- [🛠️ 技术栈](#️-技术栈)
- [📁 项目结构](#-项目结构)
- [🚀 快速开始](#-快速开始)
  - [🔧 环境要求](#-环境要求)
  - [📦 运行步骤](#-运行步骤)
- [🧪 测试](#-测试)
- [⚙️ CI/CD](#️-cicd)
  - [🔄 Jenkins Pipeline](#-jenkins-pipeline)
  - [🔐 Jenkins 配置要求](#-jenkins-配置要求)
  - [📄 Jenkinsfile](#-jenkinsfile)
  - [🏗️ 部署架构](#️-部署架构)
- [🤝 贡献指南](#-贡献指南)
- [📝 许可证](#-许可证)
- [📧 联系方式](#-联系方式)

---

## 🔭 项目简介

本项目是一个完整的外卖管理系统，包含管理端和用户端功能，采用前后端分离架构。项目基于 **Spring Boot 3.5.9** 开发，使用 MyBatis Plus、SpringDoc OpenAPI、MapStruct、FastJson2 等现代化技术栈。

> ⚠️ **注：** 本仓库只包含**后端**代码。

## 🛠️ 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| **后端框架** | Spring Boot 3.5.9 | 应用框架 |
| **JDK** | 17 | 运行时 |
| **ORM框架** | MyBatis Plus 3.5.15 | 数据访问 & 自动填充 |
| **数据库** | MySQL 8.0+ | 关系型数据库 |
| **缓存** | Redis 6.0+ | 分布式缓存 (Spring Cache) |
| **API文档** | SpringDoc OpenAPI 2.8.14 | OpenAPI 3.0 接口文档 |
| **对象映射** | MapStruct 1.6.3 | 编译时 DTO/Entity/VO 映射 |
| **JSON处理** | FastJson2 2.0.60 | JSON 序列化 |
| **构建工具** | Maven | 构建 & 依赖管理 |


```
firmament-take-out/
├── firmament-common/src/main/java/dev/kaiwen/   # 公共模块
│   ├── constant/              # 常量类
│   ├── context/               # 上下文（ThreadLocal）
│   ├── exception/             # 异常类
│   ├── json/                  # JSON 工具类（JacksonObjectMapper）
│   ├── properties/            # 配置属性类
│   ├── result/                # 统一响应结果
│   └── utils/                 # 工具类（密码加密、JWT等）
├── firmament-pojo/src/main/java/dev/kaiwen/    # 实体类模块
│   ├── dto/                   # 数据传输对象
│   ├── entity/                # 实体类
│   └── vo/                    # 视图对象
└── firmament-server/
    ├── src/main/java/dev/kaiwen/  # 服务模块 Java 源码
    │   ├── config/                # 配置类（SpringDoc、Redis、WebMvc等）
    │   ├── controller/admin/      # 管理端控制器
    │   ├── controller/user/       # 用户端控制器
    │   ├── converter/             # MapStruct 转换器
    │   ├── handler/               # 处理器（自动填充、全局异常）
    │   ├── interceptor/           # 拦截器（JWT 管理端/用户端）
    │   ├── mapper/                # MyBatis Mapper
    │   ├── service/               # 业务逻辑层
    │   ├── task/                  # 定时任务（订单超时处理）
    │   └── websocket/             # WebSocket（订单实时推送）
    └── src/main/resources/        # 资源文件（配置、模板）
```

## 📁 项目结构

```
firmament-take-out/
├── firmament-common/src/main/java/dev/kaiwen/   # 公共模块
│   ├── constant/              # 常量类
│   ├── context/               # 上下文（ThreadLocal）
│   ├── exception/             # 异常类
│   ├── json/                  # JSON 工具类（JacksonObjectMapper）
│   ├── properties/            # 配置属性类
│   ├── result/                # 统一响应结果
│   └── utils/                 # 工具类（密码加密、JWT等）
├── firmament-pojo/src/main/java/dev/kaiwen/    # 实体类模块
│   ├── dto/                   # 数据传输对象
│   ├── entity/                # 实体类
│   └── vo/                    # 视图对象
└── firmament-server/
    ├── src/main/java/dev/kaiwen/  # 服务模块 Java 源码
    │   ├── config/                # 配置类（SpringDoc、Redis、WebMvc等）
    │   ├── controller/admin/      # 管理端控制器
    │   ├── controller/user/       # 用户端控制器
    │   ├── converter/             # MapStruct 转换器
    │   ├── handler/               # 处理器（自动填充、全局异常）
    │   ├── interceptor/           # 拦截器（JWT 管理端/用户端）
    │   ├── mapper/                # MyBatis Mapper
    │   ├── service/               # 业务逻辑层
    │   ├── task/                  # 定时任务（订单超时处理）
    │   └── websocket/             # WebSocket（订单实时推送）
    └── src/main/resources/        # 资源文件（配置、模板）
```

## 🚀 快速开始

### 🔧 环境要求

| 要求 | 版本 |
|---|---|
| JDK | 17 |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

### 📦 运行步骤

1. 克隆项目
```bash
git clone https://github.com/kaiwenyao/firmament-take-out.git
cd firmament-take-out
```

2. 配置数据库
   - 复制示例配置文件：`cp firmament-server/src/main/resources/application-dev-demo.yml firmament-server/src/main/resources/application-dev.yml`
   - 修改 `application-dev.yml` 中的数据库连接信息
   - 执行数据库脚本创建表结构

3. 启动 Redis
   - 确保 Redis 服务已启动

4. 运行项目
```bash
mvn clean install
cd firmament-server
mvn spring-boot:run
```

5. 访问 Swagger 文档
   - 打开浏览器访问：`http://localhost:8080/swagger-ui.html`

## 🧪 测试

运行完整测试套件：
```bash
mvn test
```

或者在构建时跳过测试：
```bash
mvn clean package -DskipTests
```

## ⚙️ CI/CD

本项目使用 **Jenkins** 实现持续集成和持续部署（CI/CD），自动化构建、测试和部署流程。

### 🔄 Jenkins Pipeline

Jenkins Pipeline 包含以下阶段：

1. **拉取代码**
   - 从 Git 仓库拉取最新代码

2. **单元测试**
   - 运行 Maven 单元测试
   - 使用生产环境配置文件进行测试

3. **SonarQube 代码质量分析**（可选）
   - 由 `SONAR_ENABLED` 参数控制，默认关闭
   - 执行 `mvn clean verify sonar:sonar`

4. **Maven 打包**
   - 执行 `mvn clean package -DskipTests` 构建 JAR 包
   - 跳过测试（测试已在上一阶段完成）

5. **构建并推送 Docker 镜像**
   - 构建 Docker 镜像
   - 推送到 Docker Hub
   - 仅在非 PR 请求时执行

6. **部署到服务器**
   - 自动部署到生产服务器
   - 仅在 `main` 分支且非 PR 请求时执行
   - 使用 Docker 容器化部署

### 🔐 Jenkins 配置要求

在 Jenkins 中需要配置以下 Credentials：

| 凭证 ID | 说明 |
|---|---|
| `docker-username` | Docker Hub 用户名 |
| `docker-hub-credentials` | Docker Hub 用户名和密码 |
| `server-host` | 生产服务器地址 |
| `server-ssh-key` | 服务器 SSH 私钥 |
| `application-prod-env` | 生产环境配置文件 |

### 📄 Jenkinsfile

项目根目录下的 `Jenkinsfile` 定义了完整的 CI/CD 流程。Pipeline 使用声明式语法，支持：

- 多阶段构建流程
- 条件执行（分支和 PR 判断）
- 安全凭证管理
- 自动化测试和部署

### 🏗️ 部署架构

| 层级 | 技术 |
|---|---|
| 构建环境 | Jenkins + Kubernetes 动态 Pod（`maven` 容器 + `docker` 容器） |
| 镜像仓库 | Docker Hub |
| 运行环境 | 生产服务器（Docker 容器） |
| 网络 | 使用 Docker 网络 `firmament_app-network` |

## 🤝 贡献指南

欢迎贡献代码！你可以通过以下方式参与：

1. **Fork** 本仓库
2. 创建**功能分支**：`git checkout -b feature/your-feature-name`
3. **提交** 你的改动：`git commit -m "添加你的功能"`
4. **推送** 到分支：`git push origin feature/your-feature-name`
5. 发起 **Pull Request** 🚀

### 贡献规范

- 遵循现有代码风格（MapStruct 转换器、Spring Boot 规范）
- 编写清晰的提交信息
- 保持 PR 聚焦于单一改进
- 提交前确保 `mvn clean install` 通过

## 📝 许可证

本项目基于 **MIT 许可证** 开源。详见 [LICENSE](LICENSE) 文件。

## 📧 联系方式

- **GitHub Issues**: [提交 Issue](https://github.com/kaiwenyao/firmament-take-out/issues) 🐛

---

Made with ❤️ by [Kaiwen Yao](https://github.com/kaiwenyao). Happy coding! 🎉

# 🍜 Firmament Take-Out (苍穹外卖)

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-6.0+-DC382D?logo=redis)](https://redis.io/)
[![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5.15-red)](https://baomidou.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Language / 语言:** **English** (current) · [简体中文](README.zh-CN.md)

> A full-featured food delivery management system built on **Spring Boot 3** with a modern tech stack. 🚀

---

## 📋 Table of Contents

- [🔭 Overview](#-overview)
- [🛠️ Tech Stack](#️-tech-stack)
- [📁 Project Structure](#-project-structure)
- [🚀 Getting Started](#-getting-started)
  - [🔧 Prerequisites](#-prerequisites)
  - [📦 Steps](#-steps)
- [🧪 Testing](#-testing)
- [⚙️ CI/CD](#️-cicd)
  - [🔄 Pipeline Stages](#-pipeline-stages)
  - [🔐 Required Jenkins Credentials](#-required-jenkins-credentials)
  - [📄 Jenkinsfile](#-jenkinsfile)
  - [🏗️ Deployment Architecture](#️-deployment-architecture)
- [🤝 Contributing](#-contributing)
- [📝 License](#-license)
- [📧 Contact](#-contact)

---

## 🔭 Overview

This is a full-featured food delivery management system with both admin and user-facing functionality, built on a decoupled front-end/back-end architecture. The backend is developed with **Spring Boot 3.5.9** and uses a modern stack including MyBatis Plus, SpringDoc OpenAPI, MapStruct, and FastJson2.

> ⚠️ **Note:** This repository contains the **backend only**.

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Framework** | Spring Boot 3.5.9 | Application framework |
| **JDK** | 17 | Runtime |
| **ORM** | MyBatis Plus 3.5.15 | Data access & auto-fill |
| **Database** | MySQL 8.0+ | Relational database |
| **Cache** | Redis 6.0+ | Distributed caching (Spring Cache) |
| **API Docs** | SpringDoc OpenAPI 2.8.14 | OpenAPI 3.0 documentation |
| **Object Mapping** | MapStruct 1.6.3 | Compile-time DTO/Entity/VO mapping |
| **JSON** | FastJson2 2.0.60 | JSON serialization |
| **Build** | Maven | Build & dependency management |


## 📁 Project Structure

```
firmament-take-out/
├── firmament-common/src/main/java/dev/kaiwen/   # Shared utilities
│   ├── constant/              # Constants
│   ├── context/               # ThreadLocal context
│   ├── exception/             # Exception classes
│   ├── json/                  # JSON utilities (JacksonObjectMapper)
│   ├── properties/            # Configuration properties
│   ├── result/                # Unified API response wrapper
│   └── utils/                 # Utilities (password, JWT, etc.)
├── firmament-pojo/src/main/java/dev/kaiwen/    # Domain objects
│   ├── dto/                   # Data Transfer Objects
│   ├── entity/                # MyBatis Plus entities
│   └── vo/                    # View Objects
└── firmament-server/
    ├── src/main/java/dev/kaiwen/  # Application source
    │   ├── config/                # Configuration (SpringDoc, Redis, WebMvc, etc.)
    │   ├── controller/admin/      # Admin-facing controllers
    │   ├── controller/user/       # User-facing controllers
    │   ├── converter/             # MapStruct converters
    │   ├── handler/               # Handlers (auto-fill, global exception)
    │   ├── interceptor/           # JWT interceptors (admin / user)
    │   ├── mapper/                # MyBatis Mappers
    │   ├── service/               # Business logic
    │   ├── task/                  # Scheduled tasks (order timeout handling)
    │   └── websocket/             # WebSocket (real-time order push)
    └── src/main/resources/        # Configuration files and templates
```

## 🚀 Getting Started

### 🔧 Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17 |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

### 📦 Steps

1. **Clone the repository**
```bash
git clone https://github.com/kaiwenyao/firmament-take-out.git
cd firmament-take-out
```

2. **Configure the database**
   - Copy the example config: `cp firmament-server/src/main/resources/application-dev-demo.yml firmament-server/src/main/resources/application-dev.yml`
   - Update the database connection settings in `application-dev.yml`
   - Create the database schema manually (SQL scripts are not included in this repository)

3. **Start Redis**
   - Ensure a Redis instance is running

4. **Run the application**
```bash
mvn clean install
cd firmament-server
mvn spring-boot:run
```

5. **Open the API docs**
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

## 🧪 Testing

Run the full test suite:
```bash
mvn test
```

Or skip tests during build:
```bash
mvn clean package -DskipTests
```

## ⚙️ CI/CD

The project uses **Jenkins** for continuous integration and deployment — automated build, test, and deploy.

### 🔄 Pipeline Stages

1. **Checkout** — pull latest code from Git
2. **Unit Tests** — run Maven tests with the production profile
3. **SonarQube Analysis** _(optional)_ — controlled by the `SONAR_ENABLED` parameter (off by default); runs `mvn clean verify sonar:sonar`
4. **Package** — `mvn clean package -DskipTests` (tests already ran in stage 2)
5. **Build & Push Docker Image** — builds and pushes to Docker Hub; skipped on pull requests
6. **Deploy** — deploys to the production server via Docker; runs only on `main` branch, non-PR builds

### 🔐 Required Jenkins Credentials

| Credential ID | Description |
|---|---|
| `docker-username` | Docker Hub username |
| `docker-hub-credentials` | Docker Hub username + password |
| `server-host` | Production server address |
| `server-ssh-key` | Server SSH private key |
| `application-prod-env` | Production environment config file |

### 📄 Jenkinsfile

The `Jenkinsfile` at the repository root defines the full pipeline using declarative syntax. It supports multi-stage builds, conditional execution (branch and PR checks), secure credential handling, and automated deployment.

### 🏗️ Deployment Architecture

| Layer | Technology |
|---|---|
| Build environment | Jenkins + Kubernetes dynamic pods (`maven` + `docker` containers) |
| Image registry | Docker Hub |
| Runtime | Production server (Docker container) |
| Networking | Docker network `firmament_app-network` |

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. Create a **feature branch**: `git checkout -b feature/your-feature-name`
3. **Commit** your changes: `git commit -m "Add your feature"`
4. **Push** to your branch: `git push origin feature/your-feature-name`
5. Open a **Pull Request** 🚀

### Guidelines

- Follow existing code style (MapStruct converters, Spring Boot conventions)
- Write clear commit messages
- Keep PRs focused on a single improvement
- Ensure `mvn clean install` passes before submitting

## 📝 License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

## 📧 Contact

- **GitHub Issues**: [Open an Issue](https://github.com/kaiwenyao/firmament-take-out/issues) 🐛

---

Made with ❤️ by [Kaiwen Yao](https://github.com/kaiwenyao). Happy coding! 🎉

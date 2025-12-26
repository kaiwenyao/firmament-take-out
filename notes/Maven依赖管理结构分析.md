# Maven 依赖管理结构分析

## 项目概述

本项目采用 Maven 多模块架构，使用父子 POM 结构进行依赖管理。项目名称为 `firmament-take-out`，包含三个子模块：`firmament-common`、`firmament-pojo` 和 `firmament-server`。

## 项目结构

```
firmament-take-out (父项目)
├── firmament-common (通用模块)
├── firmament-pojo (实体类模块)
└── firmament-server (服务模块)
```

## 1. 根 POM (firmament-take-out/pom.xml)

### 1.1 角色定位

根 POM 是整个项目的**父项目**和**聚合项目**，负责：
- 统一管理所有子模块
- **统一管理依赖版本（不引入任何依赖）**
- 定义公共属性配置

### 1.2 POM 文件结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- 继承 Spring Boot 父项目 -->
    <parent>
        <artifactId>spring-boot-starter-parent</artifactId>
        <groupId>org.springframework.boot</groupId>
        <version>3.5.9</version>
    </parent>
    
    <!-- 项目基本信息 -->
    <groupId>dev.kaiwen</groupId>
    <artifactId>firmament-take-out</artifactId>
    <packaging>pom</packaging>  <!-- 聚合项目，不打包 -->
    <version>1.0-SNAPSHOT</version>
    
    <!-- 声明子模块 -->
    <modules>
        <module>firmament-common</module>
        <module>firmament-pojo</module>
        <module>firmament-server</module>
    </modules>
    
    <!-- 定义版本属性 -->
    <properties>
        <依赖名称1>版本号1</依赖名称1>
        <依赖名称2>版本号2</依赖名称2>
        <!-- ... 更多版本属性 ... -->
    </properties>
    
    <!-- 依赖版本管理（不引入依赖，只管理版本） -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>...</groupId>
                <artifactId>...</artifactId>
                <version>${属性名}</version>  <!-- 使用 properties 中定义的版本 -->
            </dependency>
            <!-- ... 更多依赖版本声明 ... -->
        </dependencies>
    </dependencyManagement>
    
    <!-- 注意：根 POM 中没有 <dependencies> 标签 -->
    <!-- 这意味着根 POM 不引入任何实际依赖，只做版本管理 -->
</project>
```

### 1.3 关键说明

- **`<packaging>pom</packaging>`**: 表示这是一个聚合项目，不生成 JAR/WAR 文件
- **`<modules>`**: 声明所有子模块，Maven 会按顺序构建
- **`<properties>`**: 定义版本属性，便于统一管理和修改
- **`<dependencyManagement>`**: **只管理版本，不引入依赖**
- **没有 `<dependencies>`**: 根 POM 不引入任何实际依赖

### 1.4 版本管理机制

子模块在引用依赖时，如果依赖在根 POM 的 `<dependencyManagement>` 中已声明，则**无需指定版本号**：

```xml
<!-- 子模块中的依赖声明 -->
<dependency>
    <groupId>...</groupId>
    <artifactId>...</artifactId>
    <!-- 版本号从父 POM 的 dependencyManagement 中自动获取 -->
</dependency>
```

## 2. firmament-common 模块

### 2.1 角色定位

**通用工具模块**，包含常量、异常、工具类等公共功能。

### 2.2 POM 文件结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- 继承根 POM -->
    <parent>
        <artifactId>firmament-take-out</artifactId>
        <groupId>dev.kaiwen</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    
    <modelVersion>4.0.0</modelVersion>
    <artifactId>firmament-common</artifactId>
    <!-- 未指定 packaging，默认为 jar -->
    
    <!-- 实际引入的依赖 -->
    <dependencies>
        <dependency>
            <groupId>...</groupId>
            <artifactId>...</artifactId>
            <!-- 如果父 POM 的 dependencyManagement 中有此依赖，无需写 version -->
        </dependency>
        <!-- ... 更多依赖 ... -->
    </dependencies>
</project>
```

### 2.3 特点

- **继承根 POM**: 通过 `<parent>` 标签继承，获得版本管理
- **引入实际依赖**: 通过 `<dependencies>` 标签引入需要的依赖
- **不依赖其他业务模块**: 保持独立性，可被其他模块复用

## 3. firmament-pojo 模块

### 3.1 角色定位

**数据模型模块**，包含实体类、DTO、VO 等数据对象。

### 3.2 POM 文件结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- 继承根 POM -->
    <parent>
        <artifactId>firmament-take-out</artifactId>
        <groupId>dev.kaiwen</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    
    <modelVersion>4.0.0</modelVersion>
    <artifactId>firmament-pojo</artifactId>
    
    <!-- 实际引入的依赖 -->
    <dependencies>
        <dependency>
            <groupId>...</groupId>
            <artifactId>...</artifactId>
        </dependency>
        <!-- ... 更多依赖 ... -->
    </dependencies>
</project>
```

### 3.3 特点

- **继承根 POM**: 获得版本管理
- **引入实际依赖**: 引入数据模型相关的依赖
- **不依赖其他业务模块**: 保持独立性

## 4. firmament-server 模块

### 4.1 角色定位

**主应用模块**，包含启动类、控制器、服务层等业务逻辑。

### 4.2 POM 文件结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- 继承根 POM -->
    <parent>
        <artifactId>firmament-take-out</artifactId>
        <groupId>dev.kaiwen</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    
    <modelVersion>4.0.0</modelVersion>
    <artifactId>firmament-server</artifactId>
    
    <dependencies>
        <!-- 依赖其他模块：通过 groupId、artifactId、version 引入 -->
        <dependency>
            <groupId>dev.kaiwen</groupId>
            <artifactId>firmament-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        
        <dependency>
            <groupId>dev.kaiwen</groupId>
            <artifactId>firmament-pojo</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        
        <!-- 其他外部依赖 -->
        <dependency>
            <groupId>...</groupId>
            <artifactId>...</artifactId>
        </dependency>
        <!-- ... 更多依赖 ... -->
    </dependencies>
    
    <!-- 构建配置 -->
    <build>
        <plugins>
            <!-- Spring Boot 打包插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <!-- ... 其他插件 ... -->
        </plugins>
    </build>
</project>
```

### 4.3 模块间依赖关系

**在 POM 文件中的体现**：

```xml
<!-- firmament-server 依赖 firmament-common -->
<dependency>
    <groupId>dev.kaiwen</groupId>           <!-- 与根 POM 的 groupId 一致 -->
    <artifactId>firmament-common</artifactId>  <!-- 被依赖模块的 artifactId -->
    <version>1.0-SNAPSHOT</version>        <!-- 与根 POM 的 version 一致 -->
</dependency>

<!-- firmament-server 依赖 firmament-pojo -->
<dependency>
    <groupId>dev.kaiwen</groupId>
    <artifactId>firmament-pojo</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**依赖传递关系**：

```
firmament-server
    ├── firmament-common (直接依赖)
    │   └── common 的所有依赖会传递到 server
    │
    └── firmament-pojo (直接依赖)
        └── pojo 的所有依赖会传递到 server
```

**说明**：
- 当 `firmament-server` 依赖 `firmament-common` 时，`firmament-common` 的所有依赖会自动传递到 `firmament-server`
- 这意味着 `firmament-server` 可以使用 `firmament-common` 中引入的所有依赖
- 如果 `firmament-server` 中重复声明了 `firmament-common` 已有的依赖，会造成重复引用

## 5. 依赖管理机制

### 5.1 版本管理层次

```
Spring Boot Parent (3.5.9)
    └── firmament-take-out (根 POM)
        ├── <dependencyManagement> (版本管理，不引入依赖)
        ├── firmament-common
        │   └── <dependencies> (实际引入依赖)
        ├── firmament-pojo
        │   └── <dependencies> (实际引入依赖)
        └── firmament-server
            └── <dependencies> (实际引入依赖 + 模块依赖)
```

### 5.2 版本解析优先级

1. **子模块显式指定版本** > 父 POM `dependencyManagement` > Spring Boot Parent
2. 如果子模块未指定版本，会从父 POM 的 `dependencyManagement` 中查找
3. 如果父 POM 也未管理，会从 Spring Boot Parent 中查找

### 5.3 依赖传递机制

**模块间依赖传递**：

```
firmament-server 依赖 firmament-common
    ↓
firmament-common 的所有依赖自动传递到 firmament-server
    ↓
firmament-server 可以使用 firmament-common 的所有依赖
```

**在 POM 中的体现**：

```xml
<!-- firmament-server/pom.xml -->
<dependencies>
    <!-- 直接依赖模块 -->
    <dependency>
        <groupId>dev.kaiwen</groupId>
        <artifactId>firmament-common</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- 无需重复声明 firmament-common 中已有的依赖 -->
    <!-- 这些依赖会自动通过传递依赖引入 -->
</dependencies>
```

## 6. 模块职责划分

| 模块 | 职责 | 依赖关系 | POM 特点 |
|------|------|----------|----------|
| **firmament-take-out** | 父项目，版本管理 | 不依赖任何模块 | 只有 `<dependencyManagement>`，没有 `<dependencies>` |
| **firmament-common** | 通用工具、常量、异常 | 不依赖其他业务模块 | 有 `<dependencies>`，引入通用依赖 |
| **firmament-pojo** | 数据模型（Entity、DTO、VO） | 不依赖其他业务模块 | 有 `<dependencies>`，引入数据模型相关依赖 |
| **firmament-server** | 业务逻辑、控制器、服务层 | 依赖 common 和 pojo | 有 `<dependencies>`，包含模块依赖和业务依赖 |

## 7. 关键要点总结

### 7.1 根 POM 的特点

- ✅ **只有 `<dependencyManagement>`**：只管理版本，不引入依赖
- ✅ **没有 `<dependencies>`**：不引入任何实际依赖
- ✅ **`<packaging>pom</packaging>`**：聚合项目，不打包
- ✅ **`<modules>`**：声明所有子模块

### 7.2 子模块的特点

- ✅ **继承根 POM**：通过 `<parent>` 标签
- ✅ **引入实际依赖**：通过 `<dependencies>` 标签
- ✅ **版本自动管理**：依赖版本从父 POM 的 `dependencyManagement` 中获取

### 7.3 模块间依赖的写法

```xml
<!-- 在 firmament-server/pom.xml 中 -->
<dependency>
    <groupId>dev.kaiwen</groupId>              <!-- 与根 POM 的 groupId 一致 -->
    <artifactId>firmament-common</artifactId>   <!-- 被依赖模块的 artifactId -->
    <version>1.0-SNAPSHOT</version>           <!-- 与根 POM 的 version 一致 -->
</dependency>
```

### 7.4 依赖传递

- 当模块 A 依赖模块 B 时，模块 B 的所有依赖会自动传递到模块 A
- 模块 A 无需重复声明模块 B 中已有的依赖
- 避免重复引用可以保持 POM 文件简洁

## 8. 优势分析

### 8.1 统一版本管理

- 所有依赖版本在根 POM 中统一管理
- 避免版本冲突
- 便于升级维护

### 8.2 模块化设计

- 职责清晰，便于维护
- 可复用性强（common、pojo 可被其他项目复用）
- 降低耦合度

### 8.3 构建优化

- Maven 会按依赖顺序构建模块
- 只构建变更的模块（增量构建）
- 支持并行构建

## 9. 总结

本项目采用了标准的 Maven 多模块架构：

- **根 POM**: 聚合项目，**只做版本管理，不引入任何依赖**
- **common 模块**: 提供通用功能，不依赖其他业务模块
- **pojo 模块**: 定义数据模型，不依赖其他业务模块
- **server 模块**: 主应用，通过 `<dependency>` 标签依赖 common 和 pojo 模块

这种结构清晰、易于维护，符合 Maven 最佳实践。

# Firmament Take-Out (苍穹外卖)

基于 Spring Boot 3 重构的外卖管理系统，相比旧版 `sky-take-out` 项目进行了多项技术改进和优化。

## 项目简介

本项目是一个完整的外卖管理系统，包含管理端和用户端功能，采用前后端分离架构。项目基于 Spring Boot 3.5.9 开发，使用 MyBatis Plus、SpringDoc OpenAPI 等现代化技术栈。

## 技术栈

- **后端框架**: Spring Boot 3.5.9
- **ORM框架**: MyBatis Plus 3.5.15
- **数据库**: MySQL
- **缓存**: Redis
- **API文档**: SpringDoc OpenAPI 2.7.0 (官方 Swagger)
- **对象映射**: MapStruct 1.6.3
- **构建工具**: Maven

## 核心改进

相比旧版 `sky-take-out` 项目，本项目进行了以下重要改进：

### 1. MyBatis Plus 字段自动填充

**改进说明：**
实现了公共字段的自动填充功能，避免在业务代码中手动设置创建时间、更新时间、创建人、更新人等字段。

**实现方式：**
- 创建 `AutoFillMetaObjectHandler` 实现 `MetaObjectHandler` 接口
- 在实体类字段上使用 `@TableField(fill = FieldFill.INSERT)` 或 `@TableField(fill = FieldFill.INSERT_UPDATE)` 注解
- 通过 `BaseContext` 获取当前登录用户ID，实现创建人和更新人的自动填充

**核心代码：**
```19:48:firmament-server/src/main/java/dev/kaiwen/handler/AutoFillMetaObjectHandler.java
@Component
@Slf4j
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始进行公共字段自动填充(insert)...");
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 为 4 个字段赋值 (注意：这里是属性名，不是数据库字段名)
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, CREATE_USER, Long.class, currentId);
        this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, UPDATE_USER, Long.class, currentId);
    }

    /**
     * 更新操作自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始进行公共字段自动填充(update)...");

        // 更新时只需要填充这两个
        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, UPDATE_USER, Long.class, BaseContext.getCurrentId());
    }
}
```

**使用示例：**
```39:48:firmament-pojo/src/main/java/dev/kaiwen/entity/Employee.java
    // 插入时自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    // 插入 和 更新 时都自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
```

**优势：**
- 减少重复代码，提高开发效率
- 统一管理公共字段，避免遗漏
- 保证数据一致性

---

### 2. 密码加密安全改进

**改进说明：**
使用 BCrypt 加密算法替代 MD5，提升密码安全性。同时保持对旧 MD5 密码的兼容性，支持平滑迁移。

**实现方式：**
- 创建 `PasswordUtil` 工具类，支持 BCrypt 和 MD5 两种加密方式
- 使用前缀标识密码格式：`{BCRYPT}` 和 `{MD5}`
- 自动识别密码格式并选择相应的验证方法
- 兼容旧数据：没有前缀的 32 位字符串视为 MD5

**核心代码：**
```12:81:firmament-common/src/main/java/dev/kaiwen/utils/PasswordUtil.java
@Slf4j
public class PasswordUtil {

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // MD5密码的前缀标识，用于区分新旧密码
    private static final String MD5_PREFIX = "{MD5}";
    private static final String BCRYPT_PREFIX = "{BCRYPT}";

    /**
     * 加密密码（使用BCrypt）
     * @param rawPassword 原始密码
     * @return 加密后的密码（带BCRYPT前缀）
     */
    public static String encode(String rawPassword) {
        String encoded = passwordEncoder.encode(rawPassword);
        return BCRYPT_PREFIX + encoded;
    }

    /**
     * 验证密码
     * 支持BCrypt和MD5两种格式，自动识别
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码（可能带前缀）
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        // 如果是BCrypt格式
        if (encodedPassword.startsWith(BCRYPT_PREFIX)) {
            String bcryptHash = encodedPassword.substring(BCRYPT_PREFIX.length());
            return passwordEncoder.matches(rawPassword, bcryptHash);
        }
        
        // 如果是MD5格式（带前缀）
        if (encodedPassword.startsWith(MD5_PREFIX)) {
            String md5Hash = encodedPassword.substring(MD5_PREFIX.length());
            String inputMd5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes());
            boolean matches = inputMd5.equals(md5Hash);
            
            // 如果MD5验证成功，建议升级为BCrypt（可选，这里只记录日志）
            if (matches) {
                log.info("检测到MD5密码，建议升级为BCrypt");
            }
            return matches;
        }
        
        // 兼容旧数据：没有前缀的密码，假设是MD5格式
        // 检查长度：MD5是32位十六进制字符串，BCrypt通常是60位
        if (encodedPassword.length() == 32) {
            // 可能是MD5格式
            String inputMd5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes());
            boolean matches = inputMd5.equals(encodedPassword);
            
            if (matches) {
                log.info("检测到旧格式MD5密码，建议升级为BCrypt");
            }
            return matches;
        }
        
        // 尝试BCrypt验证（不带前缀的情况）
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            log.warn("密码验证失败，格式可能不正确", e);
            return false;
        }
    }
```

**优势：**
- **安全性提升**：BCrypt 是专门为密码哈希设计的算法，具有自适应成本因子，比 MD5 更安全
- **向后兼容**：支持旧 MD5 密码，不影响现有用户登录
- **平滑迁移**：可以在用户登录时逐步将 MD5 密码升级为 BCrypt

---

### 3. Swagger 官网新版本 (SpringDoc OpenAPI)

**改进说明：**
采用 SpringDoc OpenAPI（官方 Swagger）替代旧版 Swagger2，完全兼容 Spring Boot 3，提供更好的性能和更丰富的功能。

**实现方式：**
- 使用 `springdoc-openapi-starter-webmvc-ui` 依赖（版本 2.7.0）
- 配置 `OpenAPI` Bean 自定义 API 文档信息
- 使用 `OperationCustomizer` 全局添加请求头参数

**核心代码：**
```62:74:firmament-server/src/main/java/dev/kaiwen/config/WebMvcConfiguration.java
    /**
     * 配置 OpenAPI (官方 Swagger)
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.0.0");
        openAPI.setInfo(new Info()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("基于 Spring Boot 3 重构的苍穹外卖接口文档"));
        return openAPI;
    }
```

**访问地址：**
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

**优势：**
- **官方支持**：SpringDoc 是 Spring 官方推荐的 OpenAPI 3.0 实现
- **Spring Boot 3 兼容**：无需额外配置，开箱即用
- **性能更好**：相比 Swagger2 有更好的性能表现
- **功能丰富**：支持 OpenAPI 3.0 的所有特性

---

### 4. 处理 Swagger 字符串处理问题

**改进说明：**
解决了自定义消息转换器对 Swagger 路径的干扰问题，确保 Swagger 文档正常显示。

**问题描述：**
自定义的 `JacksonObjectMapper` 会处理所有 JSON 响应，包括 Swagger 的 API 文档路径，导致 Swagger UI 无法正常显示。

**解决方案：**
在消息转换器中排除 Swagger 相关路径和 String 类型，避免自定义转换器处理这些请求。

**核心代码：**
```107:148:firmament-server/src/main/java/dev/kaiwen/config/WebMvcConfiguration.java
    /**
     * 扩展 Spring MVC 框架的消息转换器
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");

        // 创建自定义消息转换器，排除 Swagger 相关路径
        MappingJackson2HttpMessageConverter customConverter = new MappingJackson2HttpMessageConverter(new JacksonObjectMapper()) {
            @Override
            public boolean canWrite(Class<?> clazz, MediaType mediaType) {
                // 排除 String 类型和 Swagger 相关路径
                if (clazz == String.class || isSwaggerPath()) {
                    return false;
                }
                return super.canWrite(clazz, mediaType);
            }
        };

        converters.add(0, customConverter);
    }

    /**
     * 判断当前请求是否为 Swagger 相关路径
     */
    private boolean isSwaggerPath() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String path = attributes.getRequest().getRequestURI();
                return path != null && (
                        path.startsWith("/v3/api-docs") ||
                        path.startsWith("/swagger-ui") ||
                        path.startsWith("/swagger-resources") ||
                        path.startsWith("/webjars")
                );
            }
        } catch (Exception e) {
            // 忽略异常，返回 false 继续使用自定义转换器
        }
        return false;
    }
```

**优势：**
- 确保 Swagger 文档正常显示
- 不影响业务接口的自定义 JSON 序列化
- 提高系统的健壮性

---

### 5. MyBatis Plus 雪花算法 ID 精度处理

**改进说明：**
解决了 JavaScript 中 Long 类型大数精度丢失问题。当使用雪花算法生成 ID 时，Long 类型的 ID 在传输到前端时可能会丢失精度，通过将 Long 类型序列化为字符串来解决。

**实现方式：**
- 在 `JacksonObjectMapper` 中配置 Long 类型序列化器
- 在 `JacksonConfig` 中全局配置 Long 类型序列化
- 使用 `ToStringSerializer` 将 Long 类型序列化为字符串

**核心代码：**

方式一：在 `JacksonObjectMapper` 中配置
```48:50:firmament-common/src/main/java/dev/kaiwen/json/JacksonObjectMapper.java
                // 解决 JavaScript 中 Long 类型大数精度丢失问题：将 Long 类型序列化为字符串
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance);
```

方式二：在 `JacksonConfig` 中全局配置
```13:25:firmament-server/src/main/java/dev/kaiwen/config/JacksonConfig.java
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 把 Long 类型序列化为 String
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            // 把 Long 基本类型也序列化为 String
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
```

**优势：**
- 解决 JavaScript 精度丢失问题
- 保证 ID 传输的准确性
- 全局配置，无需在每个实体类上添加注解

---

### 6. MapStruct 转换代替默认 Util 或 Hutool

**改进说明：**
使用 MapStruct 进行 DTO、Entity、VO 之间的对象转换，替代手动编写转换工具类或使用 Hutool 的 BeanUtil。

**实现方式：**
- 定义转换器接口，使用 `@Mapper(componentModel = "spring")` 注解
- MapStruct 在编译时自动生成实现类
- 支持字段映射、忽略字段、自定义转换等高级功能

**核心代码：**
```1:29:firmament-server/src/main/java/dev/kaiwen/converter/EmployeeConverter.java
package dev.kaiwen.converter;

import dev.kaiwen.dto.EmployeeDTO;
import dev.kaiwen.entity.Employee;
import org.mapstruct.Mapper;


// ▼ componentModel = "spring" 是灵魂！
// 加上它，MapStruct 会自动加上 @Component 注解，你就可以在 Service 里 @Autowired 了
@Mapper(componentModel = "spring") 
public interface EmployeeConverter {

    // 不需要写实现类，MapStruct 编译时会自动生成！

    // 1. DTO -> Entity (新增员工时用)
    Employee d2e(EmployeeDTO employeeDTO);

    // 新增：把 DTO 转成 Entity (用于修改操作)
    // 2. Entity -> VO (返回给前端时用)
//    EmployeeVO toVO(Employee employee);
    
    // 3. 集合转换 List<Entity> -> List<VO>
//    List<EmployeeVO> toVOList(List<Employee> list);

    // 4. 高级用法：如果字段名不一样
    // 假设 DTO 里叫 username，但 Entity 里叫 name
    // @Mapping(source = "username", target = "name")
    // Employee toEntityCustom(EmployeeDTO dto);
}
```

**使用示例：**
```21:31:firmament-server/src/main/java/dev/kaiwen/converter/OrderDetailConverter.java
@Mapper(componentModel = "spring")
public interface OrderDetailConverter {
    /**
     * ShoppingCart -> OrderDetail (用于将购物车条目转换为订单明细)
     * @param shoppingCart 购物车条目
     * @return 订单明细
     */
    @Mapping(target = "id", ignore = true)  // id 由数据库自动生成
    @Mapping(target = "orderId", ignore = true)  // orderId 需要手动设置
    OrderDetail cart2Detail(ShoppingCart shoppingCart);
    
    /**
     * 批量转换：ShoppingCart 列表 -> OrderDetail 列表
     * @param shoppingCartList 购物车列表
     * @return 订单明细列表
     */
    List<OrderDetail> cartList2DetailList(List<ShoppingCart> shoppingCartList);
}
```

**在 Service 中使用：**
```java
@Autowired
private EmployeeConverter employeeConverter;

public void save(EmployeeDTO employeeDTO) {
    Employee employee = employeeConverter.d2e(employeeDTO);
    // ... 业务逻辑
}
```

**优势：**
- **编译时生成**：MapStruct 在编译时生成转换代码，运行时无反射，性能优异
- **类型安全**：编译时检查字段映射，避免运行时错误
- **代码简洁**：只需定义接口，无需编写实现代码
- **功能强大**：支持复杂映射、自定义转换、忽略字段等高级功能
- **IDE 友好**：生成的代码可以在 IDE 中查看和调试

---

## 项目结构

```
firmament-take-out/
├── firmament-common/          # 公共模块
│   ├── constant/              # 常量类
│   ├── context/               # 上下文（ThreadLocal）
│   ├── enumeration/           # 枚举类
│   ├── exception/             # 异常类
│   ├── json/                  # JSON 工具类
│   ├── properties/            # 配置属性类
│   ├── result/                # 统一响应结果
│   └── utils/                 # 工具类（密码加密、JWT等）
├── firmament-pojo/            # 实体类模块
│   ├── dto/                   # 数据传输对象
│   ├── entity/                # 实体类
│   └── vo/                    # 视图对象
└── firmament-server/          # 服务模块
    ├── config/                # 配置类
    ├── controller/            # 控制器
    ├── converter/             # MapStruct 转换器
    ├── handler/               # 处理器（自动填充等）
    ├── interceptor/           # 拦截器
    ├── mapper/                # MyBatis Mapper
    ├── service/               # 业务逻辑层
    └── resources/             # 资源文件
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 运行步骤

1. 克隆项目
```bash
git clone <repository-url>
cd firmament-take-out
```

2. 配置数据库
   - 修改 `firmament-server/src/main/resources/application-dev.yml` 中的数据库连接信息
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
   - 打开浏览器访问：`http://localhost:8080/swagger-ui/index.html`

## 许可证

本项目仅供学习使用。

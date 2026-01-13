Just Test
# Firmament Take-Out (苍穹外卖)

基于 Spring Boot 3 重构的外卖管理系统，相比旧版 `sky-take-out` 项目进行了多项技术改进和优化。

## 项目简介

本项目是一个完整的外卖管理系统，包含管理端和用户端功能，采用前后端分离架构。项目基于 Spring Boot 3.5.9 开发，使用 MyBatis Plus、SpringDoc OpenAPI、MapStruct、FastJson2 等现代化技术栈。
注：本仓库只包含后端

## 技术栈

- **后端框架**: Spring Boot 3.5.9
- **ORM框架**: MyBatis Plus 3.5.15
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+ (Spring Cache)
- **API文档**: SpringDoc OpenAPI 2.8.14 (官方 Swagger)
- **对象映射**: MapStruct 1.6.3
- **JSON处理**: FastJson2 2.0.60
- **构建工具**: Maven

## 核心改进

相比旧版 `sky-take-out` 项目，本项目进行了以下重要改进：

---

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
```12:113:firmament-common/src/main/java/dev/kaiwen/utils/PasswordUtil.java
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
- 使用 `springdoc-openapi-starter-webmvc-ui` 依赖（版本 2.8.14）
- 配置 `OpenAPI` Bean 自定义 API 文档信息
- 配置全局安全校验项，支持自定义 Token Header

**核心代码：**
```14:34:firmament-server/src/main/java/dev/kaiwen/config/SpringDocConfig.java
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("苍穹外卖项目接口文档")
                        .version("2.0")
                        .description("基于 Spring Boot 3 + Springdoc 的外卖项目接口文档"))

                // 1. 添加全局安全校验项（让右上角的锁头生效）
                .addSecurityItem(new SecurityRequirement().addList("GlobalToken"))

                .components(new Components()
                        // 2. 配置具体的 Token 模式
                        .addSecuritySchemes("GlobalToken",
                                new SecurityScheme()
                                        // 关键点：苍穹外卖用的是自定义 Header，不是标准的 HTTP Bearer
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("token") // 这里填你后端拦截器里读取的 header 名字
                        ));
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
```59:97:firmament-server/src/main/java/dev/kaiwen/config/WebMvcConfiguration.java
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
- 使用 `ToStringSerializer` 将 Long 类型序列化为字符串
- 同时处理 `Long` 包装类型和 `long` 基本类型
- 额外处理 `BigInteger` 类型，避免精度丢失

**核心代码：**
```41:53:firmament-common/src/main/java/dev/kaiwen/json/JacksonObjectMapper.java
        SimpleModule simpleModule = new SimpleModule()
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
                // 解决 JavaScript 中 Long 类型大数精度丢失问题：将 Long 类型序列化为字符串
                // 4. 【优化点3】补全 BigInteger 的精度处理
                // 除了 Long，BigInteger 在 JS 中也会丢失精度，建议一并处理
                .addSerializer(BigInteger.class, ToStringSerializer.instance)
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance);
```

**优势：**
- 解决 JavaScript 精度丢失问题
- 保证 ID 传输的准确性
- 全局配置，无需在每个实体类上添加注解
- 同时处理 Long 和 BigInteger，覆盖更多场景

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
```14:31:firmament-server/src/main/java/dev/kaiwen/converter/OrderDetailConverter.java
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

### 7. 使用 FastJson2 代替 FastJson

**改进说明：**
使用 FastJson2 替代旧版 FastJson，FastJson2 是阿里巴巴推出的全新 JSON 处理库，性能更优，安全性更好，完全兼容 FastJson API。

**实现方式：**
- 引入 `fastjson2` 依赖（版本 2.0.60）
- 在工具类中使用 `com.alibaba.fastjson2.JSON` 和 `com.alibaba.fastjson2.JSONObject`
- 保持 API 兼容性，迁移成本低

**依赖配置：**
```22:59:pom.xml
        <fastjson2>2.0.60</fastjson2>
        ...
            <dependency>
                <groupId>com.alibaba.fastjson2</groupId>
                <artifactId>fastjson2</artifactId>
                <version>${fastjson2}</version>
            </dependency>
```

**使用示例：**
```java
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

// JSON 字符串转对象
JSONObject jsonObject = JSON.parseObject(jsonString);

// 对象转 JSON 字符串
String jsonString = JSON.toJSONString(object);
```

**优势：**
- **性能提升**：FastJson2 相比 FastJson 性能提升 20-50%
- **安全性增强**：修复了 FastJson 中的安全漏洞
- **API 兼容**：完全兼容 FastJson API，迁移成本低
- **功能完善**：支持更多 JSON 特性，如 JSONPath、流式处理等

---

### 8. 实现 SpringCache 的序列化保存对象类别

**改进说明：**
解决了 SpringCache 使用 Redis 存储时，反序列化无法识别对象类型的问题。通过配置 `GenericJackson2JsonRedisSerializer` 并开启类型信息记录，确保反序列化时能正确还原对象类型。

**实现方式：**
- 在 `RedisConfiguration` 中配置 `CacheManager`
- 使用 `GenericJackson2JsonRedisSerializer` 作为序列化器
- 在 `JacksonObjectMapper` 中开启 `activateDefaultTyping`，添加 `@class` 字段记录类型信息

**核心代码：**
```51:74:firmament-server/src/main/java/dev/kaiwen/config/RedisConfiguration.java
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 1. 同样要给 CacheManager 的 ObjectMapper 开启类型记录
        JacksonObjectMapper objectMapper = new JacksonObjectMapper();

        // 【关键点】这里也要加！
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)) // 使用带类型记录的 Serializer
                .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }
```

**同时配置 RedisTemplate：**
```25:49:firmament-server/src/main/java/dev/kaiwen/config/RedisConfiguration.java
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 1. 获取自定义的 ObjectMapper
        JacksonObjectMapper objectMapper = new JacksonObjectMapper();

        // 【关键点】开启类型白名单（这是解决报错的核心！）
        // 它的作用是：在生成 JSON 时，多加一个 "@class" 字段来记录类名
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        return redisTemplate;
    }
```

**优势：**
- **类型安全**：反序列化时能正确识别对象类型，避免 `ClassCastException`
- **支持多态**：可以存储不同类型的对象，反序列化时自动识别
- **统一配置**：RedisTemplate 和 CacheManager 使用相同的序列化策略
- **便于调试**：JSON 中包含 `@class` 字段，便于查看存储的对象类型

---

## 项目结构

```
firmament-take-out/
├── firmament-common/          # 公共模块
│   ├── constant/              # 常量类
│   ├── context/               # 上下文（ThreadLocal）
│   ├── enumeration/           # 枚举类
│   ├── exception/             # 异常类
│   ├── json/                  # JSON 工具类（JacksonObjectMapper）
│   ├── properties/            # 配置属性类
│   ├── result/                # 统一响应结果
│   └── utils/                 # 工具类（密码加密、JWT等）
├── firmament-pojo/            # 实体类模块
│   ├── dto/                   # 数据传输对象
│   ├── entity/                # 实体类
│   └── vo/                    # 视图对象
└── firmament-server/          # 服务模块
    ├── config/                # 配置类（SpringDoc、Redis、WebMvc等）
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
   - 重命名`firmament-server/src/main/resources/application-dev-demo.yml`为`application-dev.yml`
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

## CI/CD 持续集成与部署

本项目使用 **Jenkins** 实现持续集成和持续部署（CI/CD），自动化构建、测试和部署流程。

### Jenkins Pipeline 流程

Jenkins Pipeline 包含以下阶段：

1. **拉取代码**
   - 从 Git 仓库拉取最新代码

2. **单元测试**
   - 运行 Maven 单元测试
   - 使用生产环境配置文件进行测试

3. **Maven 打包**
   - 执行 `mvn clean package` 构建 JAR 包
   - 跳过测试（测试已在上一阶段完成）

4. **构建并推送 Docker 镜像**
   - 构建 Docker 镜像
   - 推送到 Docker Hub
   - 仅在非 PR 请求时执行

5. **部署到服务器**
   - 自动部署到生产服务器
   - 仅在 `main` 分支且非 PR 请求时执行
   - 使用 Docker 容器化部署

### Jenkins 配置要求

在 Jenkins 中需要配置以下 Credentials：

- `docker-username`: Docker Hub 用户名
- `docker-hub-credentials`: Docker Hub 用户名和密码
- `server-host`: 生产服务器地址
- `server-ssh-key`: 服务器 SSH 私钥
- `application-prod-env`: 生产环境配置文件

### Jenkinsfile

项目根目录下的 `Jenkinsfile` 定义了完整的 CI/CD 流程。Pipeline 使用声明式语法，支持：

- 多阶段构建流程
- 条件执行（分支和 PR 判断）
- 安全凭证管理
- 自动化测试和部署

### 部署架构

- **构建环境**: Jenkins 服务器
- **镜像仓库**: Docker Hub
- **运行环境**: 生产服务器（Docker 容器）
- **网络**: 使用 Docker 网络 `firmament_app-network`

## 许可证

本项目仅供学习使用。
欢迎分享、交流和讨论。

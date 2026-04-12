# Firmament Take-Out (苍穹外卖)

**Language / 语言:** **English** (current) · [简体中文](README.zh-CN.md)

## 目录

- [项目简介](#overview)
- [技术栈](#tech-stack)
- [核心改进](#key-improvements)
  - [MyBatis Plus 公共字段自动填充](#1-mybatis-plus-auto-fill-for-common-fields)
  - [密码加密（BCrypt）](#2-bcrypt-password-hashing-replaces-md5)
  - [SpringDoc OpenAPI](#3-springdoc-openapi-replaces-swagger-2)
  - [Swagger 与自定义消息转换器](#4-fix-custom-message-converter-interfering-with-swagger)
  - [雪花 ID 与 Long 精度](#5-snowflake-id-precision-fix-for-javascript)
  - [MapStruct 对象映射](#6-mapstruct-for-object-mapping-replaces-beanutil--hutool)
  - [FastJson2](#7-fastjson2-replaces-fastjson)
  - [Spring Cache + Redis 序列化](#8-spring-cache-redis-serialization-with-type-information)
- [项目结构](#project-structure)
- [快速开始](#getting-started)
  - [环境要求](#prerequisites)
  - [运行步骤](#steps)
- [CI/CD](#cicd)
  - [Pipeline 阶段](#pipeline-stages)
  - [Jenkins 凭证](#required-jenkins-credentials)
  - [Jenkinsfile](#jenkinsfile)
  - [部署架构](#deployment-architecture)
- [许可证](#license)

---

A food delivery management system rebuilt on Spring Boot 3, with several technical improvements and optimizations over the original `sky-take-out` project.

## Overview

This is a full-featured food delivery management system with both admin and user-facing functionality, built on a decoupled front-end/back-end architecture. The backend is developed with Spring Boot 3.5.9 and uses a modern stack including MyBatis Plus, SpringDoc OpenAPI, MapStruct, and FastJson2.

> Note: This repository contains the backend only.

## Tech Stack

- **Framework**: Spring Boot 3.5.9
- **JDK**: 17
- **ORM**: MyBatis Plus 3.5.15
- **Database**: MySQL 8.0+
- **Cache**: Redis 6.0+ (Spring Cache)
- **API Docs**: SpringDoc OpenAPI 2.8.14
- **Object Mapping**: MapStruct 1.6.3
- **JSON**: FastJson2 2.0.60
- **Build**: Maven

## Key Improvements

The following improvements were made over the original `sky-take-out` project:

---

### 1. MyBatis Plus Auto-Fill for Common Fields

**What changed:**
Common audit fields (create time, update time, created-by, updated-by) are now populated automatically, eliminating the need to set them manually in business code.

**How it works:**
- `AutoFillMetaObjectHandler` implements `MetaObjectHandler`
- Entity fields are annotated with `@TableField(fill = FieldFill.INSERT)` or `@TableField(fill = FieldFill.INSERT_UPDATE)`
- `BaseContext` provides the current user ID for creator/updater fields

**Core code** (`firmament-server/src/main/java/dev/kaiwen/handler/AutoFillMetaObjectHandler.java`):
```java
@Component
@Slf4j
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("Auto-filling common fields (insert)...");
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, CREATE_USER, Long.class, currentId);
        this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, UPDATE_USER, Long.class, currentId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("Auto-filling common fields (update)...");

        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, UPDATE_USER, Long.class, BaseContext.getCurrentId());
    }
}
```

**Entity example** (`firmament-pojo/src/main/java/dev/kaiwen/entity/Employee.java`):
```java
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
```

**Benefits:**
- Less boilerplate in business code
- Centralized management of audit fields — no accidental omissions
- Consistent data across all records

---

### 2. BCrypt Password Hashing (replaces MD5)

**What changed:**
Passwords are now hashed with BCrypt instead of MD5, improving security. Backward compatibility with existing MD5 passwords is preserved for a smooth migration.

**How it works:**
- `PasswordService` supports both BCrypt and MD5 via format prefixes: `{BCRYPT}` and `{MD5}`
- The correct verification method is chosen based on the stored prefix
- Legacy passwords: a 32-character string without a prefix is treated as MD5
- `BCryptPasswordEncoder` is injected via constructor (Bean provided by `SecurityConfig`)

**Core code** (`firmament-common/src/main/java/dev/kaiwen/utils/PasswordService.java`):
```java
@Component
@Slf4j
public class PasswordService {

  private final BCryptPasswordEncoder passwordEncoder;

  private static final String MD5_PREFIX = "{MD5}";
  private static final String BCRYPT_PREFIX = "{BCRYPT}";

  public PasswordService(BCryptPasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  public String encode(String rawPassword) {
    String encoded = passwordEncoder.encode(rawPassword);
    return BCRYPT_PREFIX + encoded;
  }

  public boolean mismatches(String rawPassword, String encodedPassword) {
    if (rawPassword == null || encodedPassword == null) {
      return true;
    }

    if (encodedPassword.startsWith(BCRYPT_PREFIX)) {
      String bcryptHash = encodedPassword.substring(BCRYPT_PREFIX.length());
      return !passwordEncoder.matches(rawPassword, bcryptHash);
    }

    if (encodedPassword.startsWith(MD5_PREFIX)) {
      String md5Hash = encodedPassword.substring(MD5_PREFIX.length());
      return verifyMd5Password(rawPassword, md5Hash, "MD5 password detected — consider upgrading to BCrypt");
    }

    // Legacy: treat bare 32-char strings as MD5
    if (encodedPassword.length() == 32) {
      return verifyMd5Password(rawPassword, encodedPassword, "Legacy MD5 password detected — consider upgrading to BCrypt");
    }

    log.warn("Unrecognized password format, length: {}", encodedPassword.length());
    return true;
  }

  private boolean verifyMd5Password(String rawPassword, String md5Hash, String logMessage) {
    String inputMd5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
    boolean matches = inputMd5.equals(md5Hash);
    if (matches) {
      log.info(logMessage);
    }
    return !matches;
  }
}
```

**Benefits:**
- **Stronger security**: BCrypt is purpose-built for password hashing with an adaptive cost factor
- **Backward compatible**: Existing MD5 passwords continue to work
- **Gradual migration**: MD5 passwords can be silently upgraded to BCrypt at next login

---

### 3. SpringDoc OpenAPI (replaces Swagger 2)

**What changed:**
Replaced the old `springfox` Swagger 2 library with SpringDoc OpenAPI — the Spring-recommended OpenAPI 3.0 implementation, fully compatible with Spring Boot 3.

**How it works:**
- Uses `springdoc-openapi-starter-webmvc-ui` (v2.8.14)
- Custom `OpenAPI` Bean configures API metadata
- Global security scheme supports a custom token header

**Core code** (`firmament-server/src/main/java/dev/kaiwen/config/SpringDocConfig.java`):
```java
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Firmament Take-Out API")
                        .version("2.0")
                        .description("Spring Boot 3 + SpringDoc API documentation"))
                .addSecurityItem(new SecurityRequirement().addList("GlobalToken"))
                .components(new Components()
                        .addSecuritySchemes("GlobalToken",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("token")
                        ));
    }
```

**Endpoints:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

**Benefits:**
- **Spring Boot 3 compatible**: Works out of the box — `springfox` does not support Spring Boot 3
- **OpenAPI 3.0**: Supports the full OpenAPI 3.0 spec, unlike the older Swagger 2 format
- **Actively maintained**: SpringDoc is the community-standard replacement for the abandoned `springfox`

---

### 4. Fix: Custom Message Converter Interfering with Swagger

**What changed:**
Resolved an issue where the custom `JacksonObjectMapper` message converter was intercepting Swagger path requests, causing the Swagger UI to malfunction.

**Root cause:**
The custom converter processed all JSON responses, including Swagger's API doc endpoints, breaking the UI.

**Fix:**
Exclude Swagger-related paths and `String` types from the custom converter.

**Core code** (`firmament-server/src/main/java/dev/kaiwen/config/WebMvcConfiguration.java`):
```java
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("Extending message converters...");

        MappingJackson2HttpMessageConverter customConverter = new MappingJackson2HttpMessageConverter(new JacksonObjectMapper()) {
            @Override
            public boolean canWrite(Class<?> clazz, MediaType mediaType) {
                if (clazz == String.class || isSwaggerPath()) {
                    return false;
                }
                return super.canWrite(clazz, mediaType);
            }
        };

        converters.add(0, customConverter);
    }

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
            // ignore — fall through to use the custom converter
        }
        return false;
    }
```

**Benefits:**
- Swagger UI renders correctly
- Business endpoints continue to use the custom JSON serializer
- No change to existing API behavior

---

### 5. Snowflake ID Precision Fix for JavaScript

**What changed:**
Snowflake-generated `Long` IDs lose precision when transmitted to JavaScript (which uses 64-bit floats). These IDs are now serialized as strings to preserve their full value.

**How it works:**
- `JacksonObjectMapper` registers `ToStringSerializer` for `Long`, `long`, and `BigInteger`
- Applied globally — no per-entity annotations required

**Core code** (`firmament-common/src/main/java/dev/kaiwen/json/JacksonObjectMapper.java`):
```java
        SimpleModule simpleModule = new SimpleModule()
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
                .addSerializer(BigInteger.class, ToStringSerializer.instance)
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance);
```

**Benefits:**
- IDs arrive in the frontend with full precision
- Global config — no `@JsonSerialize` annotations on entities
- Covers both `Long` and `BigInteger`

---

### 6. MapStruct for Object Mapping (replaces BeanUtil / Hutool)

**What changed:**
DTO ↔ Entity ↔ VO conversions are now handled by MapStruct instead of `BeanUtils.copyProperties` or Hutool's `BeanUtil`.

**How it works:**
- Converter interfaces use `@Mapper` and expose a static `INSTANCE`
- MapStruct generates implementations at compile time — no reflection at runtime
- Supports field renaming, ignoring fields, and custom conversion logic

**Example converters** (`firmament-server/src/main/java/dev/kaiwen/converter/EmployeeConverter.java`):
```java
@Mapper
public interface EmployeeConverter {

  EmployeeConverter INSTANCE = Mappers.getMapper(EmployeeConverter.class);

  Employee d2e(EmployeeDto employeeDto);
  EmployeeVo e2v(Employee employee);
  List<EmployeeVo> entityListToVoList(List<Employee> list);
}
```

**`firmament-server/src/main/java/dev/kaiwen/converter/OrderDetailConverter.java`**:
```java
@Mapper
public interface OrderDetailConverter {

  OrderDetailConverter INSTANCE = Mappers.getMapper(OrderDetailConverter.class);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "orderId", ignore = true)
  OrderDetail cart2Detail(ShoppingCart shoppingCart);

  List<OrderDetail> cartList2DetailList(List<ShoppingCart> shoppingCartList);
}
```

**Usage in services:**
```java
// No @Autowired needed — call via the static INSTANCE
Employee employee = EmployeeConverter.INSTANCE.d2e(employeeDto);
List<OrderDetail> details = OrderDetailConverter.INSTANCE.cartList2DetailList(cartList);
```

**Benefits:**
- **Compile-time generation**: No runtime reflection — excellent performance
- **Type-safe**: Field mapping errors are caught at compile time
- **Less code**: Only interfaces needed; implementations are generated
- **IDE-friendly**: Generated code is browsable and debuggable

---

### 7. FastJson2 (replaces FastJson)

**What changed:**
Replaced the legacy `fastjson` library with `fastjson2` — Alibaba's rewritten JSON library that addresses FastJson's security vulnerabilities while keeping a compatible API.

**Dependency** (`pom.xml`):
```xml
        <fastjson2>2.0.60</fastjson2>
        ...
            <dependency>
                <groupId>com.alibaba.fastjson2</groupId>
                <artifactId>fastjson2</artifactId>
                <version>${fastjson2}</version>
            </dependency>
```

**Usage:**
```java
import com.alibaba.fastjson2.JSON;

// e.g. building a WebSocket message body in OrderServiceImpl
String jsonString = JSON.toJSONString(map);
```

**Benefits:**
- **Security fixes**: FastJson2 addresses the deserialization vulnerabilities that affected FastJson
- **API compatible**: Drop-in replacement — the `com.alibaba.fastjson2.JSON` API mirrors FastJson's
- **Extended features**: JSONPath, streaming processing, and more out of the box

---

### 8. Spring Cache Redis Serialization with Type Information

**What changed:**
Fixed a deserialization issue where Spring Cache stored objects in Redis as plain JSON, losing type information and causing `ClassCastException` on retrieval. Objects are now stored with an embedded `@class` field using a safe type whitelist.

**How it works:**
- `createJsonSerializer()` is a private factory method shared by all Redis Beans
- `BasicPolymorphicTypeValidator` restricts subtypes to `java.*` and `dev.kaiwen.*` — safer than `LaissezFaireSubTypeValidator` which allows everything
- Three Redis Beans are configured: `RedisTemplate<Object, Object>`, `RedisTemplate<String, Object>` (object cache), and `RedisTemplate<String, String>` (refresh token storage)

**Core code** (`firmament-server/src/main/java/dev/kaiwen/config/RedisConfiguration.java`):
```java
private GenericJackson2JsonRedisSerializer createJsonSerializer() {
  PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
      .allowIfBaseType(Object.class)
      .allowIfSubType("java.")
      .allowIfSubType("dev.kaiwen.")
      .build();

  ObjectMapper objectMapper = new ObjectMapper();
  objectMapper.registerModule(new JavaTimeModule());
  objectMapper.activateDefaultTyping(
      typeValidator,
      ObjectMapper.DefaultTyping.NON_FINAL,
      JsonTypeInfo.As.PROPERTY
  );
  return new GenericJackson2JsonRedisSerializer(objectMapper);
}

@Bean
public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
  GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

  RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofHours(1))
      .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
      .disableCachingNullValues();

  return RedisCacheManager.builder(redisConnectionFactory)
      .cacheDefaults(config)
      .build();
}
```

**Benefits:**
- **Type-safe deserialization**: No more `ClassCastException` from cached data
- **Restricted deserialization**: Subtypes are limited to `java.*` and `dev.kaiwen.*`, replacing the permissive `LaissezFaireSubTypeValidator`
- **Shared configuration**: All Redis Beans use the same serializer factory
- **Debuggable**: The `@class` field in Redis makes stored objects human-readable

---

## Project Structure

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

## Getting Started

### Prerequisites

- JDK 17
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### Steps

1. **Clone the repository**
```bash
git clone <repository-url>
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

## CI/CD

The project uses **Jenkins** for continuous integration and deployment — automated build, test, and deploy.

### Pipeline Stages

1. **Checkout** — pull latest code from Git
2. **Unit Tests** — run Maven tests with the production profile
3. **SonarQube Analysis** _(optional)_ — controlled by the `SONAR_ENABLED` parameter (off by default); runs `mvn clean verify sonar:sonar`
4. **Package** — `mvn clean package -DskipTests` (tests already ran in stage 2)
5. **Build & Push Docker Image** — builds and pushes to Docker Hub; skipped on pull requests
6. **Deploy** — deploys to the production server via Docker; runs only on `main` branch, non-PR builds

### Required Jenkins Credentials

| Credential ID | Description |
|---|---|
| `docker-username` | Docker Hub username |
| `docker-hub-credentials` | Docker Hub username + password |
| `server-host` | Production server address |
| `server-ssh-key` | Server SSH private key |
| `application-prod-env` | Production environment config file |

### Jenkinsfile

The `Jenkinsfile` at the repository root defines the full pipeline using declarative syntax. It supports multi-stage builds, conditional execution (branch and PR checks), secure credential handling, and automated deployment.

### Deployment Architecture

| Layer | Technology |
|---|---|
| Build environment | Jenkins + Kubernetes dynamic pods (`maven` + `docker` containers) |
| Image registry | Docker Hub |
| Runtime | Production server (Docker container) |
| Networking | Docker network `firmament_app-network` |

## License

This project is for learning purposes only. Sharing, discussion, and feedback are welcome.

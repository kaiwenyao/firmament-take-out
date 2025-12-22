# JWT 令牌使用指南

## 目录

1. [JWT 基础概念](#1-jwt-基础概念)
2. [项目中的 JWT 配置](#2-项目中的-jwt-配置)
3. [JWT 工具类详解](#3-jwt-工具类详解)
4. [登录时的 JWT 生成流程](#4-登录时的-jwt-生成流程)
5. [JWT 验证流程（拦截器）](#5-jwt-验证流程拦截器)
6. [文件调用步骤详解](#6-文件调用步骤详解)
7. [关键代码解析](#7-关键代码解析)
8. [总结](#8-总结)

---

## 1. JWT 基础概念

### 1.1 什么是 JWT？

JWT（JSON Web Token）是一种开放标准（RFC 7519），用于在各方之间安全地传输信息。它是一种紧凑且自包含的方式，用于在客户端和服务器之间传递声明。

### 1.2 JWT 的组成部分

JWT 由三部分组成，用点（`.`）分隔：

```text
Header.Payload.Signature
```

#### Header（头部）
包含令牌的类型（JWT）和使用的签名算法（如 HS256）。

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

#### Payload（载荷）
包含声明（Claims），即要传输的数据。例如用户ID、过期时间等。

```json
{
  "empId": 1,
  "exp": 1234567890
}
```

#### Signature（签名）
用于验证令牌的完整性和真实性。签名由以下部分组成：
- Header（Base64 编码）
- Payload（Base64 编码）
- 密钥（Secret Key）

### 1.3 JWT 的优势

1. **无状态**：服务器不需要存储会话信息，所有信息都在令牌中
2. **跨域友好**：可以在不同域名之间传递
3. **自包含**：令牌本身包含所有必要信息
4. **安全性**：通过签名确保令牌未被篡改

---

## 2. 项目中的 JWT 配置

### 2.1 配置文件位置

JWT 的配置信息存储在 Spring Boot 的配置文件中：

**文件路径**：`firmament-server/src/main/resources/application.yml`

```yaml
# firmament-server/src/main/resources/application.yml (38:45)
firmament:
  jwt:
    # 设置jwt签名加密时使用的秘钥
    admin-secret-key: ykw
    # 设置jwt过期时间
    admin-ttl: 7200000
    # 设置前端传递过来的令牌名称
    admin-token-name: token
```

### 2.2 配置项说明

| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| `admin-secret-key` | JWT 签名使用的密钥，用于加密和解密令牌 | `ykw` |
| `admin-ttl` | JWT 令牌的有效期（毫秒），7200000 = 2小时 | `7200000` |
| `admin-token-name` | 前端在请求头中传递令牌时使用的字段名 | `token` |

**重要提示**：
- 密钥应该足够复杂，生产环境建议使用随机生成的字符串
- 过期时间根据业务需求设置，太短会影响用户体验，太长会有安全风险

### 2.3 JwtProperties 配置类

Spring Boot 通过 `@ConfigurationProperties` 注解自动将配置文件中的值绑定到 Java 对象中。

```java
package dev.kaiwen.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "firmament.jwt")
@Data
public class JwtProperties {

    /**
     * 管理端员工生成jwt令牌相关配置
     */
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;

    /**
     * 用户端微信用户生成jwt令牌相关配置
     */
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;

}
```

**注解说明**：
- `@Component`：将此类注册为 Spring Bean，可以被其他组件注入使用
- `@ConfigurationProperties(prefix = "firmament.jwt")`：指定配置前缀，Spring 会自动将 `firmament.jwt.*` 下的配置绑定到对应的属性
- `@Data`：Lombok 注解，自动生成 getter、setter 等方法

**工作原理**：
1. Spring Boot 启动时扫描 `@ConfigurationProperties` 注解
2. 读取 `application.yml` 中的配置
3. 将配置值自动注入到 `JwtProperties` 对象的属性中
4. 其他类可以通过 `@Autowired` 注入 `JwtProperties` 来获取配置值

---

## 3. JWT 工具类详解

### 3.1 为什么封装成工具类？

将 JWT 操作封装成工具类有以下优势：

1. **代码复用**：避免在多个地方重复编写相同的 JWT 生成和解析代码
2. **统一管理**：所有 JWT 相关操作集中在一个类中，便于维护和修改
3. **易于测试**：工具类可以独立测试，不依赖其他业务逻辑
4. **降低耦合**：业务代码不需要了解 JWT 的具体实现细节

### 3.2 JwtUtil 工具类

```java
// firmament-common/src/main/java/dev/kaiwen/utils/JwtUtil.java (11:58)
public class JwtUtil {
    /**
     * 生成jwt
     * 使用Hs256算法, 私匙使用固定秘钥
     *
     * @param secretKey jwt秘钥
     * @param ttlMillis jwt过期时间(毫秒)
     * @param claims    设置的信息
     * @return
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 指定签名的时候使用的签名算法，也就是header那部分
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        // 生成JWT的时间
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // 设置jwt的body
        JwtBuilder builder = Jwts.builder()
                // 如果有私有声明，一定要先设置这个自己创建的私有的声明，这个是给builder的claim赋值，一旦写在标准的声明赋值之后，就是覆盖了那些标准的声明的
                .setClaims(claims)
                // 设置签名使用的签名算法和签名使用的秘钥
                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
                // 设置过期时间
                .setExpiration(exp);

        return builder.compact();
    }

    /**
     * Token解密
     *
     * @param secretKey jwt秘钥 此秘钥一定要保留好在服务端, 不能暴露出去, 否则sign就可以被伪造, 如果对接多个客户端建议改造成多个
     * @param token     加密后的token
     * @return
     */
    public static Claims parseJWT(String secretKey, String token) {
        // 得到DefaultJwtParser
        Claims claims = Jwts.parser()
                // 设置签名的秘钥
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                // 设置需要解析的jwt
                .parseClaimsJws(token).getBody();
        return claims;
    }

}
```

### 3.3 createJWT() 方法详解

**功能**：生成 JWT 令牌

**参数说明**：
- `secretKey`：用于签名的密钥，必须与解析时使用的密钥一致
- `ttlMillis`：令牌的有效期（毫秒）
- `claims`：要存储在令牌中的信息（如用户ID、用户名等）

**执行步骤**：
1. 指定签名算法为 HS256
2. 计算过期时间 = 当前时间 + 有效期
3. 构建 JWT：
   - 设置自定义声明（claims）
   - 使用密钥签名
   - 设置过期时间
4. 返回编码后的令牌字符串

**示例**：
```java
Map<String, Object> claims = new HashMap<>();
claims.put("empId", 1);
String token = JwtUtil.createJWT("ykw", 7200000, claims);
// 返回类似：eyJhbGciOiJIUzI1NiJ9.eyJlbXBJZCI6MX0.xxxxx
```

### 3.4 parseJWT() 方法详解

**功能**：解析并验证 JWT 令牌

**参数说明**：
- `secretKey`：用于验证签名的密钥，必须与生成时使用的密钥一致
- `token`：要解析的 JWT 令牌字符串

**执行步骤**：
1. 创建 JWT 解析器
2. 设置签名密钥
3. 解析令牌并验证签名
4. 如果验证通过，返回 Claims 对象（包含令牌中的所有信息）
5. 如果验证失败（签名不匹配、已过期等），抛出异常

**安全提示**：
- 密钥必须保密，不能暴露给客户端
- 如果密钥泄露，攻击者可以伪造令牌
- 建议为不同的客户端使用不同的密钥

---

## 4. 登录时的 JWT 生成流程

### 4.1 完整调用链

当用户登录时，JWT 令牌的生成流程如下：

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant Controller as EmployeeController
    participant Service as EmployeeService
    participant Mapper as EmployeeMapper
    participant JwtUtil as JwtUtil工具类
    participant JwtProps as JwtProperties

    Client->>Controller: POST /admin/employee/login<br/>(username, password)
    Controller->>Service: login(employeeLoginDTO)
    Service->>Mapper: getByUsername(username)
    Mapper-->>Service: Employee对象
    Service->>Service: 验证密码和状态
    Service-->>Controller: Employee对象
    
    Controller->>JwtProps: getAdminSecretKey()
    JwtProps-->>Controller: "ykw"
    Controller->>JwtProps: getAdminTtl()
    JwtProps-->>Controller: 7200000
    
    Controller->>Controller: 创建claims Map<br/>(包含empId)
    Controller->>JwtUtil: createJWT(secretKey, ttl, claims)
    JwtUtil-->>Controller: JWT令牌字符串
    
    Controller->>Controller: 构建EmployeeLoginVO<br/>(包含token)
    Controller-->>Client: 返回Result<EmployeeLoginVO>
```

### 4.2 代码实现详解

#### 步骤 1：接收登录请求

```java
// firmament-server/src/main/java/dev/kaiwen/controller/admin/EmployeeController.java (40:44)
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);
```

- `@PostMapping("/login")`：处理 POST 请求，路径为 `/admin/employee/login`
- `@RequestBody`：将请求体中的 JSON 数据转换为 `EmployeeLoginDTO` 对象
- 调用 `employeeService.login()` 验证用户信息

#### 步骤 2：验证用户信息

```java
// firmament-server/src/main/java/dev/kaiwen/service/impl/EmployeeServiceImpl.java (28:39)
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
```

Service 层负责：
1. 从数据库查询用户信息
2. 验证用户名是否存在
3. 验证密码是否正确
4. 验证账号是否被锁定
5. 如果验证失败，抛出相应的异常

#### 步骤 3：生成 JWT 令牌

```java
// firmament-server/src/main/java/dev/kaiwen/controller/admin/EmployeeController.java (46:52)
        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);
```

**详细说明**：

1. **创建 Claims Map**：
   ```java
   Map<String, Object> claims = new HashMap<>();
   claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
   ```
   - 创建一个 Map 存储要放入令牌的信息
   - `JwtClaimsConstant.EMP_ID` 的值是 `"empId"`
   - 将员工ID放入 claims 中

2. **获取配置信息**：
   - `jwtProperties.getAdminSecretKey()`：获取密钥（"ykw"）
   - `jwtProperties.getAdminTtl()`：获取过期时间（7200000毫秒）

3. **调用工具类生成令牌**：
   ```java
   String token = JwtUtil.createJWT(secretKey, ttl, claims);
   ```
   - 传入密钥、过期时间和声明信息
   - 返回 JWT 令牌字符串

#### 步骤 4：构建返回对象

```java
// firmament-server/src/main/java/dev/kaiwen/controller/admin/EmployeeController.java (54:61)
        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
```

- 使用 Builder 模式构建返回对象
- 包含用户信息和 JWT 令牌
- 封装在 `Result` 对象中返回给前端

### 4.3 生成的 JWT 令牌示例

假设员工ID为 1，生成的令牌可能如下：

```text
eyJhbGciOiJIUzI1NiJ9.eyJlbXBJZCI6MSwiZXhwIjoxNzM1Njg4MDAwfQ.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**解码后的内容**：

Header：
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Payload：
```json
{
  "empId": 1,
  "exp": 1735688000
}
```

---

## 5. JWT 验证流程（拦截器）

### 5.1 为什么需要拦截器？

在用户登录后，后续的 API 请求都需要验证用户身份。如果每个 Controller 方法都写验证代码，会导致：
- 代码重复
- 难以维护
- 容易遗漏

使用拦截器可以在请求到达 Controller 之前统一进行 JWT 验证。

### 5.2 JwtTokenAdminInterceptor 拦截器

```java
// firmament-server/src/main/java/dev/kaiwen/interceptor/JwtTokenAdminInterceptor.java (18:57)
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("当前员工id：", empId);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }
}
```

### 5.3 验证流程详解

#### 步骤 1：判断拦截目标

```java
if (!(handler instanceof HandlerMethod)) {
    return true;
}
```

- 检查拦截到的是否是 Controller 方法
- 如果不是（如静态资源），直接放行

#### 步骤 2：从请求头获取令牌

```java
String token = request.getHeader(jwtProperties.getAdminTokenName());
```

- 从 HTTP 请求头中获取令牌
- `jwtProperties.getAdminTokenName()` 返回 `"token"`
- 前端需要在请求头中添加：`token: eyJhbGciOiJIUzI1NiJ9...`

#### 步骤 3：解析和验证令牌

```java
Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
```

- 使用 `JwtUtil.parseJWT()` 解析令牌
- 如果令牌无效、过期或签名不匹配，会抛出异常
- 从 Claims 中提取员工ID

#### 步骤 4：处理验证结果

- **验证成功**：`return true`，请求继续执行
- **验证失败**：设置 HTTP 状态码 401（未授权），`return false`，请求被拦截

### 5.4 拦截器注册配置

```java
// firmament-server/src/main/java/dev/kaiwen/config/WebMvcConfiguration.java (28:33)
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");
    }
```

**配置说明**：
- `addPathPatterns("/admin/**")`：拦截所有以 `/admin/` 开头的请求
- `excludePathPatterns("/admin/employee/login")`：排除登录接口（登录时还没有令牌）

**拦截器执行顺序**：

```mermaid
flowchart TD
    A[客户端请求] --> B{路径匹配?}
    B -->|/admin/**| C[拦截器执行]
    B -->|其他路径| H[直接访问Controller]
    C --> D{是登录接口?}
    D -->|是| H
    D -->|否| E[从请求头获取token]
    E --> F{token验证}
    F -->|成功| G[提取员工ID]
    F -->|失败| I[返回401]
    G --> H[Controller处理请求]
    H --> J[返回响应]
```

---

## 6. 文件调用步骤详解

### 6.1 登录流程完整调用链

```mermaid
sequenceDiagram
    autonumber
    participant C as 客户端
    participant EC as EmployeeController
    participant ES as EmployeeService
    participant EM as EmployeeMapper
    participant DB as 数据库
    participant JP as JwtProperties
    participant JU as JwtUtil
    participant JJ as JJWT库

    Note over C,DB: 1. 登录请求阶段
    C->>EC: POST /admin/employee/login<br/>Body: {username, password}
    EC->>ES: login(employeeLoginDTO)
    ES->>EM: getByUsername(username)
    EM->>DB: SELECT * FROM employee WHERE username=?
    DB-->>EM: Employee数据
    EM-->>ES: Employee对象
    
    Note over ES: 2. 验证阶段
    ES->>ES: 检查employee是否为null
    ES->>ES: 验证密码
    ES->>ES: 检查账号状态
    ES-->>EC: Employee对象（验证通过）
    
    Note over EC,JJ: 3. JWT生成阶段
    EC->>JP: getAdminSecretKey()
    JP-->>EC: "ykw"
    EC->>JP: getAdminTtl()
    JP-->>EC: 7200000
    EC->>EC: 创建claims Map<br/>claims.put("empId", 1)
    EC->>JU: createJWT("ykw", 7200000, claims)
    JU->>JJ: Jwts.builder()<br/>.setClaims(claims)<br/>.signWith(HS256, key)<br/>.setExpiration(exp)
    JJ-->>JU: JWT字符串
    JU-->>EC: token字符串
    
    Note over EC,C: 4. 返回响应阶段
    EC->>EC: 构建EmployeeLoginVO<br/>(id, userName, name, token)
    EC->>EC: Result.success(employeeLoginVO)
    EC-->>C: HTTP 200<br/>Body: {code: 1, data: {...token...}}
```

### 6.2 后续请求验证流程

```mermaid
sequenceDiagram
    autonumber
    participant C as 客户端
    participant I as JwtTokenAdminInterceptor
    participant JP as JwtProperties
    participant JU as JwtUtil
    participant EC as EmployeeController
    participant ES as EmployeeService

    Note over C,ES: 1. 请求拦截阶段
    C->>I: GET /admin/employee/list<br/>Header: token=xxx
    I->>I: 检查handler类型
    I->>JP: getAdminTokenName()
    JP-->>I: "token"
    I->>I: request.getHeader("token")
    I->>I: 获取token字符串
    
    Note over I,JU: 2. 令牌验证阶段
    I->>JP: getAdminSecretKey()
    JP-->>I: "ykw"
    I->>JU: parseJWT("ykw", token)
    JU->>JU: Jwts.parser()<br/>.setSigningKey(key)<br/>.parseClaimsJws(token)
    
    alt 验证成功
        JU-->>I: Claims对象
        I->>I: 提取empId
        I->>I: return true (放行)
        I->>EC: 请求继续
        EC->>ES: 业务处理
        ES-->>EC: 返回结果
        EC-->>C: HTTP 200
    else 验证失败
        JU-->>I: 抛出异常
        I->>I: response.setStatus(401)
        I->>I: return false (拦截)
        I-->>C: HTTP 401 Unauthorized
    end
```

### 6.3 关键文件说明

| 文件路径 | 作用 | 关键注解/方法 |
|---------|------|--------------|
| `EmployeeController.java` | 处理登录请求，生成JWT | `@PostMapping("/login")` |
| `EmployeeService.java` | 验证用户信息 | `login()` |
| `JwtProperties.java` | 读取JWT配置 | `@ConfigurationProperties` |
| `JwtUtil.java` | JWT工具类 | `createJWT()`, `parseJWT()` |
| `JwtTokenAdminInterceptor.java` | 拦截器，验证JWT | `preHandle()` |
| `WebMvcConfiguration.java` | 注册拦截器 | `addInterceptors()` |

---

## 7. 关键代码解析

### 7.1 常量定义

```java
// firmament-common/src/main/java/dev/kaiwen/constant/JwtClaimsConstant.java (1:11)
package dev.kaiwen.constant;

public class JwtClaimsConstant {

    public static final String EMP_ID = "empId";
    public static final String USER_ID = "userId";
    public static final String PHONE = "phone";
    public static final String USERNAME = "username";
    public static final String NAME = "name";

}
```

**作用**：定义 JWT 中使用的键名常量，避免硬编码字符串，提高代码可维护性。

### 7.2 登录接口完整代码

```java
// firmament-server/src/main/java/dev/kaiwen/controller/admin/EmployeeController.java (40:62)
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }
```

**逐行解析**：

1. **`@PostMapping("/login")`**
   - Spring MVC 注解，处理 POST 请求
   - 完整路径：`/admin/employee/login`

2. **`@RequestBody EmployeeLoginDTO`**
   - 将请求体中的 JSON 自动转换为 Java 对象
   - 前端发送：`{"username": "admin", "password": "123456"}`

3. **`@Autowired private JwtProperties jwtProperties`**
   - Spring 依赖注入，自动注入配置对象
   - 可以获取配置文件中的 JWT 相关配置

4. **`employeeService.login(employeeLoginDTO)`**
   - 调用 Service 层验证用户
   - 如果验证失败，会抛出异常（由全局异常处理器捕获）

5. **生成 JWT 令牌**
   - 创建 claims Map，存储员工ID
   - 调用 `JwtUtil.createJWT()` 生成令牌
   - 使用配置的密钥和过期时间

6. **构建返回对象**
   - 使用 Builder 模式创建 `EmployeeLoginVO`
   - 包含用户信息和令牌
   - 封装在 `Result` 中返回

### 7.3 拦截器完整代码

```java
// firmament-server/src/main/java/dev/kaiwen/interceptor/JwtTokenAdminInterceptor.java (34:56)
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("当前员工id：", empId);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }
```

**方法说明**：

- **`preHandle()`**：拦截器的前置处理方法，在 Controller 方法执行前调用
- **返回值**：
  - `true`：继续执行后续流程
  - `false`：拦截请求，不再执行后续流程

**异常处理**：

`JwtUtil.parseJWT()` 可能抛出以下异常：
- `ExpiredJwtException`：令牌已过期
- `SignatureException`：签名验证失败
- `MalformedJwtException`：令牌格式错误

所有异常都会被 catch 捕获，返回 401 状态码。

### 7.4 配置类注册拦截器

```java
// firmament-server/src/main/java/dev/kaiwen/config/WebMvcConfiguration.java (16:33)
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");
    }
```

**关键点**：

1. **`@Configuration`**：标识这是一个配置类，Spring 会扫描并加载
2. **`extends WebMvcConfigurationSupport`**：继承 Spring MVC 配置支持类
3. **`addInterceptors()`**：重写方法，注册自定义拦截器
4. **路径匹配**：
   - `addPathPatterns()`：添加需要拦截的路径模式
   - `excludePathPatterns()`：排除不需要拦截的路径

---

## 8. 总结

### 8.1 JWT 在项目中的完整流程

1. **配置阶段**：在 `application.yml` 中配置密钥、过期时间等
2. **登录阶段**：用户登录成功后，生成 JWT 令牌并返回给前端
3. **存储阶段**：前端将令牌存储在本地（localStorage 或 cookie）
4. **请求阶段**：前端在每次请求的 Header 中携带令牌
5. **验证阶段**：拦截器验证令牌的有效性
6. **授权阶段**：验证通过后，允许访问受保护的资源

### 8.2 关键知识点总结

| 知识点 | 说明 |
|--------|------|
| **JWT 结构** | Header.Payload.Signature 三部分组成 |
| **密钥管理** | 通过配置文件管理，使用 `@ConfigurationProperties` 绑定 |
| **工具类封装** | 将 JWT 操作封装成静态工具类，便于复用 |
| **拦截器机制** | 使用 Spring MVC 拦截器统一验证令牌 |
| **异常处理** | 验证失败返回 401 状态码 |

### 8.3 最佳实践建议

1. **密钥安全**：
   - 生产环境使用复杂的随机密钥
   - 不要将密钥提交到代码仓库
   - 考虑使用环境变量或配置中心

2. **过期时间**：
   - 根据业务需求设置合理的过期时间
   - 可以考虑实现刷新令牌机制

3. **错误处理**：
   - 区分不同类型的错误（过期、无效、缺失）
   - 返回明确的错误信息（但不要泄露敏感信息）

4. **性能优化**：
   - JWT 解析有一定性能开销，可以考虑缓存
   - 对于不需要验证的接口，及时排除

### 8.4 学习建议

对于初学者，建议按以下顺序学习：

1. **理解 JWT 基础概念**：什么是 JWT，为什么使用 JWT
2. **学习 Spring Boot 配置**：理解 `@ConfigurationProperties` 的作用
3. **理解工具类设计**：为什么封装成工具类，如何使用
4. **学习拦截器机制**：理解 Spring MVC 拦截器的工作原理
5. **实践调试**：通过断点调试，观察 JWT 的生成和验证过程

### 8.5 扩展阅读

- [JWT 官方文档](https://jwt.io/)
- [Spring Boot 配置属性](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Spring MVC 拦截器](https://docs.spring.io/spring-framework/docs/current/reference/web.html#mvc-handlermapping-interceptor)

---

**文档版本**：v1.0  
**最后更新**：2024年  
**适用项目**：苍穹外卖（Firmament Take-Out）


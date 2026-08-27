package dev.kaiwen.it;

import com.sun.net.httpserver.HttpServer;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.utils.JwtService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试基类：启动完整 Spring 上下文 + 真实 MySQL/Redis 容器（Testcontainers）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>{@code webEnvironment = RANDOM_PORT}：WebSocket 的 {@code ServerEndpointExporter}
 *       需要真实 servlet 容器（Tomcat）提供 {@code ServerContainer}，不能用 MOCK 环境。</li>
 *   <li>容器以「共享单例」方式手动启动（JVM 生命周期存活），所有集成测试类复用同一组容器与
 *       同一个 Spring 上下文，避免每类重启容器导致的连接被拒与启动开销。
 *       容器地址通过 {@code @DynamicPropertySource} 注入数据源与 Redis。</li>
 *   <li>schema.sql 由 {@code application-it.yml} 的 {@code spring.sql.init}
 *       在容器就绪后执行建表；每条测试用 {@code @Sql} 先清后插保证幂等。</li>
 *   <li>{@code @Sql} 只回滚 MySQL，Redis 状态会在测试方法与测试类之间残留（菜品缓存、
 *       营业状态、刷新令牌等）。因此这里额外用 {@link #flushRedisBeforeEachTest()}
 *       在每个测试方法前 flushDb，消除对方法/类执行顺序的隐式依赖。</li>
 * </ul>
 *
 * <p>新测试优先用 {@link #loginAdmin()} / {@link #loginUser()} 走真实登录签发 JWT；
 * {@link #adminToken(Long)} / {@link #userToken(Long)} 仍保留给既有用例。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
public abstract class IntegrationTestBase {

  /**
   * MySQL 8 共享容器：静态块中手动 start()，JVM 存活期内只启动一次，所有子类复用。
   * 建表交给 {@code application-it.yml} 的 {@code spring.sql.init} 执行 schema.sql。
   */
  static final MySQLContainer<?> MYSQL;

  /** Redis 共享容器：手动 start()，JVM 存活期内只启动一次。 */
  static final GenericContainer<?> REDIS;

  /**
   * Local stub for WeChat jscode2session so {@code POST /user/user/login} IT does not call api.weixin.qq.com.
   */
  static final HttpServer WECHAT_STUB;

  /**
   * 容器标签键：CI 上 Ryuk 被禁用（受限集群拉不起 privileged 容器），
   * 若 pod 被强杀则 JVM shutdown hook 来不及执行，容器会残留在宿主节点上。
   * 给容器打上本次构建的标签，Jenkins {@code post { always }} 据此精确清理，
   * 不会误删并发构建正在使用的容器。本地运行时环境变量缺省，标签不生效。
   */
  static final String BUILD_TAG_LABEL = "dev.kaiwen.it.build";

  static {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    String buildTag = System.getenv("FIRMAMENT_IT_BUILD_TAG");
    MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("firmament_it")
        .withUsername("test")
        .withPassword("test")
        .withEnv("TZ", "UTC")
        .withReuse(false);
    REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(false);
    if (buildTag != null && !buildTag.isBlank()) {
      MYSQL.withLabel(BUILD_TAG_LABEL, buildTag);
      REDIS.withLabel(BUILD_TAG_LABEL, buildTag);
    }
    // 顺序启动：先 MySQL 再 Redis。启动失败会直接抛异常，测试无法继续。
    MYSQL.start();
    REDIS.start();
    try {
      WECHAT_STUB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      WECHAT_STUB.createContext("/sns/jscode2session", exchange -> {
        String query = exchange.getRequestURI().getRawQuery();
        String body;
        if (query != null && query.contains("js_code=bad-code")) {
          body = "{\"errcode\":40029,\"errmsg\":\"invalid code\"}";
        } else if (query != null && query.contains("js_code=existing-user-code")) {
          body = "{\"openid\":\"it-openid-100\"}";
        } else {
          body = "{\"openid\":\"it-wx-openid-new\"}";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
      });
      WECHAT_STUB.start();
      Runtime.getRuntime().addShutdownHook(
          new Thread(() -> WECHAT_STUB.stop(0), "wechat-stub-shutdown"));
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void containerProps(DynamicPropertyRegistry registry) {
    // 数据源直连 MySQL 容器（用 HikariCP 默认连接池，不依赖 Druid）
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name",
        () -> "com.mysql.cj.jdbc.Driver");
    // Redis 直连容器
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
    registry.add("firmament.wechat.login-url",
        () -> "http://127.0.0.1:" + WECHAT_STUB.getAddress().getPort() + "/sns/jscode2session");
  }

  @LocalServerPort
  protected int port;

  @Autowired
  protected org.springframework.boot.test.web.client.TestRestTemplate restTemplate;

  @Autowired
  protected JwtService jwtService;

  @Autowired
  private RedisConnectionFactory redisConnectionFactory;

  @Autowired
  protected JwtProperties jwtProperties;

  /**
   * 每个测试方法前清空 Redis，配合 {@code @Sql} 的 MySQL 清理形成完整隔离。
   *
   * <p>容器与 Spring 上下文是全 JVM 共享的单例，菜品缓存（{@code dish_*}）、店铺营业状态
   * （{@code SHOP_STATUS}）、刷新令牌（{@code refresh_token:*}）等都会跨测试方法与测试类残留，
   * 让断言隐式依赖 JUnit 的执行顺序。这里直接 flushDb 消除该耦合——容器是一次性的测试实例，
   * 清库没有副作用。
   */
  @BeforeEach
  void flushRedisBeforeEachTest() {
    try (RedisConnection connection = redisConnectionFactory.getConnection()) {
      connection.serverCommands().flushDb();
    }
  }

  /**
   * 生成管理端可用的真实 JWT（empId claim），供带 {@code token} 请求头调用 admin 接口。
   *
   * @param empId 员工ID
   * @return 签名后的 access token
   */
  protected String adminToken(Long empId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(JwtClaimsConstant.EMP_ID, empId);
    return jwtService.createJwt(jwtProperties.getAdminSecretKey(),
        jwtProperties.getAdminTtl(), claims);
  }

  /**
   * 生成用户端可用的真实 JWT（userId claim），供带 {@code authentication} 请求头调用 user 接口。
   *
   * @param userId 用户ID
   * @return 签名后的 access token
   */
  protected String userToken(Long userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(JwtClaimsConstant.USER_ID, userId);
    return jwtService.createJwt(jwtProperties.getUserSecretKey(),
        jwtProperties.getUserTtl(), claims);
  }

  /** 管理端请求头键名（与 application-it.yml 的 admin-token-name 一致）。 */
  protected String adminTokenHeader() {
    return jwtProperties.getAdminTokenName();
  }

  /** 用户端请求头键名（与 application-it.yml 的 user-token-name 一致）。 */
  protected String userTokenHeader() {
    return jwtProperties.getUserTokenName();
  }

  /** JSON 请求头，供登录与带 body 的 REST 调用复用。 */
  protected HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /**
   * 管理端真实登录（默认种子账号 admin / 123456），返回登录签发的 access 与 refresh token。
   */
  protected AdminLogin loginAdmin() {
    return loginAdmin("admin", "123456");
  }

  protected AdminLogin loginAdmin(String username, String password) {
    Map<String, String> body = Map.of("username", username, "password", password);
    ResponseEntity<Map> resp = restTemplate.postForEntity(
        "/admin/employee/login", new HttpEntity<>(body, jsonHeaders()), Map.class);
    Map<?, ?> data = requireSuccessData(resp, "admin login");
    String token = (String) data.get("token");
    String refreshToken = (String) data.get("refreshToken");
    if (token == null || token.isBlank() || refreshToken == null || refreshToken.isBlank()) {
      throw new AssertionError("admin login did not return tokens: " + data);
    }
    return new AdminLogin(token, refreshToken);
  }

  /**
   * C 端真实手机号登录（默认种子 13900000000 / 123456），返回登录签发的 access token。
   */
  protected String loginUser() {
    return loginUser("13900000000", "123456");
  }

  protected String loginUser(String phone, String password) {
    Map<String, String> body = Map.of("phone", phone, "password", password);
    ResponseEntity<Map> resp = restTemplate.postForEntity(
        "/user/user/phoneLogin", new HttpEntity<>(body, jsonHeaders()), Map.class);
    Map<?, ?> data = requireSuccessData(resp, "user phoneLogin");
    String token = (String) data.get("token");
    if (token == null || token.isBlank()) {
      throw new AssertionError("user login did not return token: " + data);
    }
    return token;
  }

  protected HttpHeaders adminHeaders(String accessToken) {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), accessToken);
    return headers;
  }

  protected HttpHeaders userHeaders(String accessToken) {
    HttpHeaders headers = jsonHeaders();
    headers.set(userTokenHeader(), accessToken);
    return headers;
  }

  /** Jackson 可能把 Long 序列化成字符串，统一按字符串解析。 */
  protected static long asLong(Object value) {
    return Long.parseLong(String.valueOf(value));
  }

  protected static int asInt(Object value) {
    return Integer.parseInt(String.valueOf(value));
  }

  @SuppressWarnings("rawtypes")
  protected Map<?, ?> requireSuccessData(ResponseEntity<Map> resp, String action) {
    if (resp.getBody() == null) {
      throw new AssertionError(action + " returned empty body, status=" + resp.getStatusCode());
    }
    if (!Integer.valueOf(1).equals(resp.getBody().get("code"))) {
      throw new AssertionError(action + " failed, msg=" + resp.getBody().get("msg"));
    }
    Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
    if (data == null) {
      throw new AssertionError(action + " succeeded but data is null");
    }
    return data;
  }

  /** 管理端登录签发的一对 token。 */
  protected static final class AdminLogin {
    final String token;
    final String refreshToken;

    AdminLogin(String token, String refreshToken) {
      this.token = token;
      this.refreshToken = refreshToken;
    }
  }
}

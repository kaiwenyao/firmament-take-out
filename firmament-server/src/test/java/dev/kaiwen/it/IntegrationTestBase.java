package dev.kaiwen.it;

import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.utils.JwtService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
 * </ul>
 *
 * <p>子类只需关注业务断言，通过 {@link #adminToken(Long)} / {@link #userToken(Long)}
 * 获取真实可用的 JWT 即可。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
public abstract class IntegrationTestBase {

  /**
   * MySQL 8 共享容器：静态块中手动 start()，JVM 存活期内只启动一次，所有子类复用。
   * 用 withInitScript 在首次启动时建表（比依赖 spring.sql.init 更可靠，不受上下文重启影响）。
   */
  static final MySQLContainer<?> MYSQL;

  /** Redis 共享容器：手动 start()，JVM 存活期内只启动一次。 */
  static final GenericContainer<?> REDIS;

  static {
    MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("firmament_it")
        .withUsername("test")
        .withPassword("test")
        .withReuse(false);
    REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(false);
    // 顺序启动：先 MySQL 再 Redis。启动失败会直接抛异常，测试无法继续。
    MYSQL.start();
    REDIS.start();
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
  }

  @LocalServerPort
  protected int port;

  @Autowired
  protected org.springframework.boot.test.web.client.TestRestTemplate restTemplate;

  @Autowired
  protected JwtService jwtService;

  @Autowired
  protected JwtProperties jwtProperties;

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
}

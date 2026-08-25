package dev.kaiwen.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * 管理端员工接口的 REST 集成测试：真实 HTTP → Controller → Service → Mapper → MySQL。
 *
 * <p>覆盖端到端链路：登录拿真实 token → 带 token 调用受保护接口 → 校验数据库副作用与响应。
 */
@Sql(scripts = {"/sql/cleanup.sql", "/sql/data-employee.sql"})
class EmployeeIntegrationTest extends IntegrationTestBase {

  @Autowired
  private ObjectMapper objectMapper;

  /** 真实登录成功：返回真实签名的 JWT 与员工信息。 */
  @Test
  void loginWithValidCredentialsReturnsToken() throws Exception {
    Map<String, String> body = Map.of("username", "admin", "password", "123456");

    ResponseEntity<Map> resp = restTemplate.postForEntity(
        "/admin/employee/login", new HttpEntity<>(body, jsonHeaders()), Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = resp.getBody();
    assertThat(result).isNotNull();
    assertThat(result.get("code")).isEqualTo(1);
    Map<?, ?> data = (Map<?, ?>) result.get("data");
    assertThat(data).isNotNull();
    assertThat(data.get("userName")).isEqualTo("admin");
    assertThat((String) data.get("token")).isNotBlank();
    assertThat((String) data.get("refreshToken")).isNotBlank();
  }

  /** 登录失败：密码错误返回业务失败码（HTTP 200 + code=0）。 */
  @Test
  void loginWithWrongPasswordFails() {
    Map<String, String> body = Map.of("username", "admin", "password", "wrong");

    ResponseEntity<Map> resp = restTemplate.postForEntity(
        "/admin/employee/login", new HttpEntity<>(body, jsonHeaders()), Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().get("code")).isEqualTo(0);
  }

  /** 不带 token 访问受保护接口被拦截器拒绝（401）。 */
  @Test
  void protectedEndpointWithoutTokenReturns401() {
    ResponseEntity<String> resp = restTemplate.getForEntity("/admin/employee/page?page=1&pageSize=10",
        String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /** 带 token 访问受保护接口通过，并真实分页查询数据库。 */
  @Test
  void pageQueryWithValidTokenReturnsRecords() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    ResponseEntity<Map> resp = restTemplate.exchange(
        "/admin/employee/page?page=1&pageSize=10", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = resp.getBody();
    assertThat(result).isNotNull();
    assertThat(result.get("code"))
        .as("page 返回码，msg=%s", result.get("msg"))
        .isEqualTo(1);
    Map<?, ?> data = (Map<?, ?>) result.get("data");
    assertThat(data).isNotNull();
    // data-employee.sql 插入了 1 条 admin 记录
    long total = Long.parseLong(String.valueOf(data.get("total")));
    assertThat(total).isGreaterThanOrEqualTo(1);
    assertThat((java.util.List<?>) data.get("records")).isNotEmpty();
  }

  /** 新增员工真实落库，随后可通过分页查询看到。 */
  @Test
  void saveEmployeePersistsAndIsQueryable() throws Exception {
    // 先用一个不重复的用户名新增
    Map<String, String> newEmp = Map.of(
        "username", "it-user-" + System.nanoTime(),
        "name", "集成测试员工",
        "phone", "13800000000",
        "sex", "1",
        "idNumber", "110101199003070001");

    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    ResponseEntity<Map> saveResp = restTemplate.exchange(
        "/admin/employee", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(newEmp), headers), Map.class);

    assertThat(saveResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(saveResp.getBody()).isNotNull();
    assertThat(saveResp.getBody().get("code"))
        .as("save 返回码，msg=%s", saveResp.getBody().get("msg"))
        .isEqualTo(1);

    // 通过分页查询确认数据已落库
    ResponseEntity<Map> pageResp = restTemplate.exchange(
        "/admin/employee/page?page=1&pageSize=1000", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);

    Map<?, ?> pageData = (Map<?, ?>) pageResp.getBody().get("data");
    java.util.List<?> records = (java.util.List<?>) pageData.get("records");
    boolean found = records.stream()
        .map(r -> (Map<?, ?>) r)
        .anyMatch(r -> "集成测试员工".equals(r.get("name")));
    assertThat(found).as("新增的员工应能在分页查询中出现").isTrue();
  }

  /** 启停员工账号真实改库：禁用后再登录应被拒（账号锁定）。 */
  @Test
  void disableEmployeeBlocksLogin() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    // 禁用 id=2 的 second 用户
    ResponseEntity<Map> resp = restTemplate.exchange(
        "/admin/employee/status/{status}?id={id}", HttpMethod.POST,
        new HttpEntity<>(headers), Map.class, 0, 2L);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("code")).isEqualTo(1);

    // 用被禁用账号登录应失败
    Map<String, String> body = Map.of("username", "second", "password", "123456");
    ResponseEntity<Map> loginResp = restTemplate.postForEntity(
        "/admin/employee/login", new HttpEntity<>(body, jsonHeaders()), Map.class);
    assertThat(loginResp.getBody().get("code")).isEqualTo(0);
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}

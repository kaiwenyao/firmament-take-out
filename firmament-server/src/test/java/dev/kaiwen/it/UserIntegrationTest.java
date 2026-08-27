package dev.kaiwen.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * C端用户接口的 REST 集成测试：手机号登录真实签发 JWT → 带认证访问受保护接口。
 *
 * <p>覆盖：手机号密码登录、获取当前用户信息（验证 ThreadLocal 注入）、地址簿增查改。
 */
@Sql(scripts = {"/sql/cleanup.sql", "/sql/data-user.sql"})
class UserIntegrationTest extends IntegrationTestBase {

  @Autowired
  private ObjectMapper objectMapper;

  /** 手机号登录成功，返回真实签发的 JWT。 */
  @Test
  void phoneLoginReturnsRealToken() {
    Map<String, String> body = Map.of("phone", "13900000000", "password", "123456");

    ResponseEntity<Map> resp = restTemplate.postForEntity(
        "/user/user/phoneLogin", new HttpEntity<>(body, jsonHeaders()), Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = resp.getBody();
    assertThat(result.get("code")).isEqualTo(1);
    Map<?, ?> data = (Map<?, ?>) result.get("data");
    assertThat((String) data.get("token")).isNotBlank();
    assertThat(Long.parseLong(String.valueOf(data.get("id")))).isEqualTo(100L);
  }

  /** 登录失败：密码错误。 */
  @Test
  void phoneLoginWithWrongPasswordFails() {
    Map<String, String> body = Map.of("phone", "13900000000", "password", "wrong");

    ResponseEntity<Map> resp = restTemplate.postForEntity(
        "/user/user/phoneLogin", new HttpEntity<>(body, jsonHeaders()), Map.class);

    assertThat(resp.getBody().get("code")).isEqualTo(0);
  }

  /** 用真实登录拿到的 token 访问受保护接口 /user/user/info，验证 ThreadLocal 注入正确。 */
  @Test
  void getInfoWithLoginTokenReturnsCurrentUser() {
    // 1. 先真实登录拿 token
    Map<String, String> loginBody = Map.of("phone", "13900000000", "password", "123456");
    ResponseEntity<Map> loginResp = restTemplate.postForEntity(
        "/user/user/phoneLogin", new HttpEntity<>(loginBody, jsonHeaders()), Map.class);
    String token = (String) ((Map<?, ?>) loginResp.getBody().get("data")).get("token");

    // 2. 带 token 查询当前用户信息
    HttpHeaders headers = jsonHeaders();
    headers.set(userTokenHeader(), token);

    ResponseEntity<Map> resp = restTemplate.exchange(
        "/user/user/info", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("code")).isEqualTo(1);
    Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
    assertThat(data).isNotNull();
    assertThat(Long.parseLong(String.valueOf(data.get("id")))).isEqualTo(100L);
    assertThat(data.get("phone")).isEqualTo("13900000000");
  }

  /** 不带 token 访问受保护接口 /user/user/info 应被拦截（401）。 */
  @Test
  void getInfoWithoutTokenReturns401() {
    ResponseEntity<String> resp = restTemplate.getForEntity("/user/user/info", String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /** 地址簿：新增地址后能在 list 中读到，验证按当前用户隔离。 */
  @Test
  void addAddressThenListIt() throws Exception {
    HttpHeaders headers = jsonHeaders();
    headers.set(userTokenHeader(), userToken(100L));

    Map<String, Object> address = Map.of(
        "consignee", "张三",
        "phone", "13900000000",
        "sex", "1",
        "detail", "测试地址-集成测试",
        "label", "家");

    ResponseEntity<Map> saveResp = restTemplate.exchange(
        "/user/addressBook", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(address), headers), Map.class);
    assertThat(saveResp.getBody().get("code"))
        .as("msg=%s", saveResp.getBody().get("msg")).isEqualTo(1);

    ResponseEntity<Map> listResp = restTemplate.exchange(
        "/user/addressBook/list", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);

    java.util.List<?> list = (java.util.List<?>) listResp.getBody().get("data");
    assertThat(list).isNotNull();
    assertThat(list.stream()
        .map(r -> (Map<?, ?>) r)
        .anyMatch(r -> "测试地址-集成测试".equals(r.get("detail")))).isTrue();
  }
}

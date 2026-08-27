package dev.kaiwen.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
@Sql(scripts = {"/sql/cleanup.sql", "/sql/data-user.sql", "/sql/data-address.sql"})
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

  @Test
  void wechatLoginUsesStubbedOpenid() {
    ResponseEntity<Map> existing = restTemplate.postForEntity(
        "/user/user/login",
        new HttpEntity<>(Map.of("code", "existing-user-code"), jsonHeaders()), Map.class);
    Map<?, ?> existingData = requireSuccessData(existing, "wx login existing");
    assertThat(asLong(existingData.get("id"))).isEqualTo(100L);
    assertThat((String) existingData.get("token")).isNotBlank();
    assertThat(existingData.get("openid")).isEqualTo("it-openid-100");

    ResponseEntity<Map> created = restTemplate.postForEntity(
        "/user/user/login",
        new HttpEntity<>(Map.of("code", "new-user-code"), jsonHeaders()), Map.class);
    Map<?, ?> createdData = requireSuccessData(created, "wx login new");
    assertThat(asLong(createdData.get("id"))).isNotEqualTo(100L);
    assertThat((String) createdData.get("token")).isNotBlank();
    assertThat(createdData.get("openid")).isEqualTo("it-wx-openid-new");

    ResponseEntity<Map> bad = restTemplate.postForEntity(
        "/user/user/login",
        new HttpEntity<>(Map.of("code", "bad-code"), jsonHeaders()), Map.class);
    assertThat(bad.getBody().get("code")).isEqualTo(0);
    assertThat(String.valueOf(bad.getBody().get("msg"))).contains("登录失败");
  }

  @Test
  void addressBookGetUpdateDefaultAndDelete() throws Exception {
    HttpHeaders userH = userHeaders(loginUser());

    ResponseEntity<Map> byId = restTemplate.exchange(
        "/user/addressBook/{id}", HttpMethod.GET, new HttpEntity<>(userH), Map.class, 1000);
    Map<?, ?> first = requireSuccessData(byId, "get address 1000");
    assertThat(first.get("detail")).isEqualTo("IT-address-1");
    assertThat(asInt(first.get("isDefault"))).isEqualTo(1);

    ResponseEntity<Map> defaultBefore = restTemplate.exchange(
        "/user/addressBook/default", HttpMethod.GET, new HttpEntity<>(userH), Map.class);
    assertThat(asLong(requireSuccessData(defaultBefore, "get default").get("id"))).isEqualTo(1000L);

    ResponseEntity<Map> setDefault = restTemplate.exchange(
        "/user/addressBook/default", HttpMethod.PUT,
        new HttpEntity<>(objectMapper.writeValueAsString(Map.of("id", 1001)), userH), Map.class);
    assertThat(setDefault.getBody().get("code"))
        .as("msg=%s", setDefault.getBody().get("msg")).isEqualTo(1);

    ResponseEntity<Map> defaultAfter = restTemplate.exchange(
        "/user/addressBook/default", HttpMethod.GET, new HttpEntity<>(userH), Map.class);
    Map<?, ?> newDefault = requireSuccessData(defaultAfter, "get default after switch");
    assertThat(asLong(newDefault.get("id"))).isEqualTo(1001L);
    assertThat(asInt(newDefault.get("isDefault"))).isEqualTo(1);

    ResponseEntity<Map> oldAddr = restTemplate.exchange(
        "/user/addressBook/{id}", HttpMethod.GET, new HttpEntity<>(userH), Map.class, 1000);
    assertThat(asInt(requireSuccessData(oldAddr, "old address").get("isDefault"))).isEqualTo(0);

    Map<String, Object> update = Map.of(
        "id", 1001,
        "consignee", "Li Si",
        "phone", "13900000001",
        "sex", "1",
        "detail", "IT-address-2-updated",
        "label", "company");
    ResponseEntity<Map> putResp = restTemplate.exchange(
        "/user/addressBook", HttpMethod.PUT,
        new HttpEntity<>(objectMapper.writeValueAsString(update), userH), Map.class);
    assertThat(putResp.getBody().get("code"))
        .as("msg=%s", putResp.getBody().get("msg")).isEqualTo(1);

    ResponseEntity<Map> updated = restTemplate.exchange(
        "/user/addressBook/{id}", HttpMethod.GET, new HttpEntity<>(userH), Map.class, 1001);
    assertThat(requireSuccessData(updated, "updated address").get("detail"))
        .isEqualTo("IT-address-2-updated");

    ResponseEntity<Map> deleteResp = restTemplate.exchange(
        "/user/addressBook?id={id}", HttpMethod.DELETE,
        new HttpEntity<>(userH), Map.class, 1001);
    assertThat(deleteResp.getBody().get("code"))
        .as("msg=%s", deleteResp.getBody().get("msg")).isEqualTo(1);

    ResponseEntity<Map> missingDefault = restTemplate.exchange(
        "/user/addressBook/default", HttpMethod.GET, new HttpEntity<>(userH), Map.class);
    assertThat(missingDefault.getBody().get("code")).isEqualTo(0);
    assertThat(String.valueOf(missingDefault.getBody().get("msg"))).contains("默认地址");

    ResponseEntity<Map> listResp = restTemplate.exchange(
        "/user/addressBook/list", HttpMethod.GET, new HttpEntity<>(userH), Map.class);
    List<?> list = (List<?>) listResp.getBody().get("data");
    assertThat(list).hasSize(1);
    assertThat(asLong(((Map<?, ?>) list.get(0)).get("id"))).isEqualTo(1000L);
  }
}

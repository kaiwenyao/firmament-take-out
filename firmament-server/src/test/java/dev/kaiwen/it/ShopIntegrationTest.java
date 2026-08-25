package dev.kaiwen.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 店铺营业状态接口的 REST 集成测试：真实经 Redis 读写。
 *
 * <p>覆盖：管理端写入 Redis → 管理端/用户端读取同一 key，验证生产用的
 * {@code RedisTemplate} 直连 Testcontainers Redis 容器，跨接口共享状态。
 */
class ShopIntegrationTest extends IntegrationTestBase {

  /** 管理端设置营业状态后，用户端（无需认证）能读到同一个值。 */
  @Test
  void adminSetsStatusThenUserReadsIt() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    // 管理端：设置为营业中（1）。PUT /admin/shop/{status} 需要 token。
    ResponseEntity<Map> setResp = restTemplate.exchange(
        "/admin/shop/{status}", HttpMethod.PUT,
        new HttpEntity<>(headers), Map.class, 1);

    assertThat(setResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(setResp.getBody()).isNotNull();
    assertThat(setResp.getBody().get("code")).isEqualTo(1);

    // 管理端读取（需 token）
    ResponseEntity<Map> adminGetResp = restTemplate.exchange(
        "/admin/shop/status", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);
    assertThat(adminGetResp.getBody().get("code")).isEqualTo(1);
    assertThat(adminGetResp.getBody().get("data")).isEqualTo(1);

    // 用户端读取（/user/shop/status 在拦截器 excludePathPatterns 中，无需 token）
    ResponseEntity<Map> userGetResp = restTemplate.getForEntity(
        "/user/shop/status", Map.class);
    assertThat(userGetResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(userGetResp.getBody().get("code")).isEqualTo(1);
    assertThat(userGetResp.getBody().get("data")).isEqualTo(1);
  }

  /** 设置打烊状态（0）后读取应为 0。 */
  @Test
  void setStatusToClosedIsReadable() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    restTemplate.exchange("/admin/shop/{status}", HttpMethod.PUT,
        new HttpEntity<>(headers), Map.class, 0);

    ResponseEntity<Map> resp = restTemplate.exchange(
        "/admin/shop/status", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(resp.getBody().get("data")).isEqualTo(0);
  }

  /** 管理端设置营业状态未带 token 应被拦截（401）。 */
  @Test
  void setShopStatusWithoutTokenReturns401() {
    ResponseEntity<String> resp = restTemplate.exchange(
        "/admin/shop/{status}", HttpMethod.PUT,
        new HttpEntity<>(jsonHeaders()), String.class, 1);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}

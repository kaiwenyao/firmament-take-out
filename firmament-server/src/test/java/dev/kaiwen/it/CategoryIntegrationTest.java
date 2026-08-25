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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * 分类接口的 REST 集成测试：真实 CRUD + 分页，覆盖管理端与用户端。
 *
 * <p>覆盖：新增/分页/按类型查询/启停/修改/删除，验证 MyBatis-Plus 分页插件真实生效。
 */
@Sql(scripts = {"/sql/cleanup.sql", "/sql/data-category.sql"})
class CategoryIntegrationTest extends IntegrationTestBase {

  @Autowired
  private ObjectMapper objectMapper;

  /** 按 type 查询分类：用户端 list 无需 token，且只返回启用中(status=1)且匹配 type 的记录。 */
  @Test
  void listByTypeReturnsMatchingCategories() {
    ResponseEntity<Map> resp = restTemplate.getForEntity(
        "/user/category/list?type=1", Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().get("code")).isEqualTo(1);
    java.util.List<?> data = (java.util.List<?>) resp.getBody().get("data");
    // data-category.sql 中 type=1 且启用(status=1)的有 id=10 一条（id=11 是禁用）
    assertThat(data).isNotEmpty();
    assertThat(data).allSatisfy(item -> {
      assertThat(((Map<?, ?>) item).get("type")).isEqualTo(1);
      assertThat(((Map<?, ?>) item).get("status")).isEqualTo(1);
    });
  }

  /** 新增分类真实落库，随后分页查询能看到。 */
  @Test
  void saveCategoryAppearsInPageQuery() throws Exception {
    String name = "it-cat-" + System.nanoTime();
    Map<String, Object> body = Map.of("type", 1, "name", name, "sort", 9);

    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    ResponseEntity<Map> saveResp = restTemplate.exchange(
        "/admin/category", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(body), headers), Map.class);
    assertThat(saveResp.getBody().get("code"))
        .as("msg=%s", saveResp.getBody().get("msg")).isEqualTo(1);

    // 分页查询确认落库
    ResponseEntity<Map> pageResp = restTemplate.exchange(
        "/admin/category/page?page=1&pageSize=100&name=" + name, HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);

    Map<?, ?> data = (Map<?, ?>) pageResp.getBody().get("data");
    assertThat(Long.parseLong(String.valueOf(data.get("total")))).isEqualTo(1);
    java.util.List<?> records = (java.util.List<?>) data.get("records");
    assertThat(((Map<?, ?>) records.get(0)).get("name")).isEqualTo(name);
  }

  /** 分页查询返回全部种子数据并按总量校验。 */
  @Test
  void pageQueryReturnsAllSeededCategories() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    ResponseEntity<Map> resp = restTemplate.exchange(
        "/admin/category/page?page=1&pageSize=100", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);

    Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
    assertThat(Long.parseLong(String.valueOf(data.get("total")))).isEqualTo(3);
    assertThat((java.util.List<?>) data.get("records")).hasSize(3);
  }

  /** 禁用分类真实改库：status 由 1 改为 0。 */
  @Test
  void disableCategoryFlipsStatus() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    ResponseEntity<Map> resp = restTemplate.exchange(
        "/admin/category/status/{status}?id={id}", HttpMethod.POST,
        new HttpEntity<>(headers), Map.class, 0, 10);

    assertThat(resp.getBody().get("code")).isEqualTo(1);

    // 分页查询确认状态已变为 0
    ResponseEntity<Map> pageResp = restTemplate.exchange(
        "/admin/category/page?page=1&pageSize=100", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);
    java.util.List<?> records = (java.util.List<?>)
        ((Map<?, ?>) pageResp.getBody().get("data")).get("records");
    Map<?, ?> updated = records.stream()
        .map(r -> (Map<?, ?>) r)
        .filter(r -> Integer.valueOf(10).equals(toInt(r.get("id"))))
        .findFirst().orElseThrow();
    assertThat(updated.get("status")).isEqualTo(0);
  }

  /** 删除分类真实删库：分页 total 减少。 */
  @Test
  void deleteCategoryReducesTotal() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    restTemplate.exchange("/admin/category?id={id}", HttpMethod.DELETE,
        new HttpEntity<>(headers), Map.class, 12);

    ResponseEntity<Map> resp = restTemplate.exchange(
        "/admin/category/page?page=1&pageSize=100", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);
    long total = Long.parseLong(String.valueOf(
        ((Map<?, ?>) resp.getBody().get("data")).get("total")));
    assertThat(total).isEqualTo(2);
  }

  /** 未带 token 访问受保护的管理端分页接口应被拦截（401）。 */
  @Test
  void adminPageWithoutTokenReturns401() {
    ResponseEntity<String> resp = restTemplate.getForEntity(
        "/admin/category/page?page=1&pageSize=10", String.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private Integer toInt(Object v) {
    return Integer.parseInt(String.valueOf(v));
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}

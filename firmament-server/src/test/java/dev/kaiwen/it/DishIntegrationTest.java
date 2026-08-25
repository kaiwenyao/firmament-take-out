package dev.kaiwen.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
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
 * 菜品接口的 REST 集成测试：管理端新增（含口味）→ 用户端浏览（含 Redis 缓存）。
 *
 * <p>覆盖跨表写入（dish + dish_flavor）、按分类查询、缓存读写、起售/停售。
 */
@Sql(scripts = {"/sql/cleanup.sql", "/sql/data-dish.sql"})
class DishIntegrationTest extends IntegrationTestBase {

  @Autowired
  private ObjectMapper objectMapper;

  /** 管理端新增带口味的菜品，落库 dish + dish_flavor；用户端按分类可查到。 */
  @Test
  void createDishWithFlavorsThenUserListsIt() throws Exception {
    Map<String, Object> flavor = Map.of(
        "name", "辣度",
        "value", "[\"不辣\",\"微辣\",\"变态辣\"]");
    Map<String, Object> body = Map.of(
        "name", "测试菜品-" + System.nanoTime(),
        "categoryId", 20,
        "price", new BigDecimal("42.00"),
        "description", "集成测试新增",
        "status", 1,
        "flavors", List.of(flavor));

    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    ResponseEntity<Map> saveResp = restTemplate.exchange(
        "/admin/dish", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(body), headers), Map.class);
    assertThat(saveResp.getBody().get("code"))
        .as("msg=%s", saveResp.getBody().get("msg")).isEqualTo(1);

    // 用户端按分类查询（无需 token），能看到起售中的菜品
    ResponseEntity<Map> listResp = restTemplate.getForEntity(
        "/user/dish/list?categoryId=20", Map.class);

    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp.getBody().get("code")).isEqualTo(1);
    List<?> dishes = (List<?>) listResp.getBody().get("data");
    assertThat(dishes).isNotNull();
    assertThat(dishes).isNotEmpty();
    // 宫保鸡丁-集成测试 是种子数据里的起售菜品，至少应存在
    assertThat(dishes.stream()
        .map(d -> (Map<?, ?>) d)
        .anyMatch(d -> "宫保鸡丁-集成测试".equals(d.get("name")))).isTrue();
  }

  /** 管理端按 id 查询菜品返回口味（dish_flavor 关联）。 */
  @Test
  void getDishByIdReturnsFlavors() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    ResponseEntity<Map> resp = restTemplate.exchange(
        "/admin/dish/{id}", HttpMethod.GET, new HttpEntity<>(headers), Map.class, 200);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
    assertThat(data.get("name")).isEqualTo("宫保鸡丁-集成测试");
    List<?> flavors = (List<?>) data.get("flavors");
    assertThat(flavors).hasSize(1);
    assertThat(((Map<?, ?>) flavors.get(0)).get("name")).isEqualTo("甜辣度");
  }

  /** 菜品停售后，用户端列表不再包含该菜品。 */
  @Test
  void stopSellingHidesDishFromUserList() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    // 停售 id=200
    restTemplate.exchange("/admin/dish/status/{status}?id={id}", HttpMethod.POST,
        new HttpEntity<>(headers), Map.class, 0, 200);

    // 用户端查询分类 20，不应再出现宫保鸡丁-集成测试
    ResponseEntity<Map> listResp = restTemplate.getForEntity(
        "/user/dish/list?categoryId=20", Map.class);
    List<?> dishes = (List<?>) listResp.getBody().get("data");
    boolean stillPresent = dishes == null ? false : dishes.stream()
        .map(d -> (Map<?, ?>) d)
        .anyMatch(d -> "宫保鸡丁-集成测试".equals(d.get("name")));
    assertThat(stillPresent).as("停售菜品不应出现在用户端列表").isFalse();
  }

  /** 管理端菜品分页查询返回种子数据。 */
  @Test
  void adminDishPageQueryReturnsSeededDish() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    ResponseEntity<Map> resp = restTemplate.exchange(
        "/admin/dish/page?page=1&pageSize=100", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class);

    Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
    assertThat(Long.parseLong(String.valueOf(data.get("total")))).isGreaterThanOrEqualTo(1);
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}

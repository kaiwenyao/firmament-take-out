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
    String dishName = "测试菜品-" + System.nanoTime();
    Map<String, Object> flavor = Map.of(
        "name", "辣度",
        "value", "[\"不辣\",\"微辣\",\"变态辣\"]");
    Map<String, Object> body = Map.of(
        "name", dishName,
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

    List<Map<?, ?>> dishMaps = dishes.stream().<Map<?, ?>>map(d -> (Map<?, ?>) d).toList();
    List<Object> names = dishMaps.stream().<Object>map(d -> d.get("name")).toList();
    // 分类 20 下起售的菜品恰好是：种子数据的 kung-pao-chicken-it + 本次新增的这一条
    assertThat(names).containsExactlyInAnyOrder("kung-pao-chicken-it", dishName);

    // 新增的菜品必须带上刚提交的字段与口味，证明 dish + dish_flavor 两张表都写入了
    Map<?, ?> created = dishMaps.stream()
        .filter(d -> dishName.equals(d.get("name")))
        .findFirst()
        .orElseThrow();
    assertThat(created.get("id")).as("新增菜品应已分配主键").isNotNull();
    // JacksonObjectMapper 把 Long 序列化成字符串（避免前端精度丢失），故按字符串比较
    assertThat(String.valueOf(created.get("categoryId"))).isEqualTo("20");
    assertThat(created.get("status")).as("提交 status=1，应处于起售").isEqualTo(1);
    assertThat(created.get("description")).isEqualTo("集成测试新增");
    assertThat(new BigDecimal(String.valueOf(created.get("price"))))
        .isEqualByComparingTo(new BigDecimal("42.00"));

    List<?> createdFlavors = (List<?>) created.get("flavors");
    assertThat(createdFlavors).as("口味应随菜品一起落库并回查").hasSize(1);
    Map<?, ?> createdFlavor = (Map<?, ?>) createdFlavors.get(0);
    assertThat(createdFlavor.get("name")).isEqualTo("辣度");
    assertThat(createdFlavor.get("value")).isEqualTo("[\"不辣\",\"微辣\",\"变态辣\"]");
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
    assertThat(data.get("name")).isEqualTo("kung-pao-chicken-it");
    List<?> flavors = (List<?>) data.get("flavors");
    assertThat(flavors).hasSize(1);
    assertThat(((Map<?, ?>) flavors.get(0)).get("name")).isEqualTo("sweet-spicy-level");
  }

  /**
   * 菜品停售后，用户端列表不再包含该菜品——同时验证起售停售会失效 Redis 缓存。
   *
   * <p>先查一次把 {@code dish_20} 写进缓存，再停售。若管理端没有清理该 key，
   * 第二次查询会命中旧缓存并仍返回停售菜品，本用例即失败。
   */
  @Test
  void stopSellingHidesDishFromUserList() {
    HttpHeaders headers = jsonHeaders();
    headers.set(adminTokenHeader(), adminToken(1L));

    // 1. 预热缓存：此时菜品起售，应出现在列表里
    assertThat(userListContainsSeededDish())
        .as("停售前，起售菜品应出现在用户端列表（同时把 dish_20 写入缓存）").isTrue();

    // 2. 停售 id=200
    ResponseEntity<Map> stopResp = restTemplate.exchange(
        "/admin/dish/status/{status}?id={id}", HttpMethod.POST,
        new HttpEntity<>(headers), Map.class, 0, 200);
    assertThat(stopResp.getBody().get("code"))
        .as("msg=%s", stopResp.getBody().get("msg")).isEqualTo(1);

    // 3. 再查：缓存应已被清理，返回的是过滤掉停售菜品的新结果
    assertThat(userListContainsSeededDish())
        .as("停售菜品不应出现在用户端列表（缓存未失效时会命中旧数据）").isFalse();
  }

  /** 用户端按分类 20 查询，返回列表中是否包含种子菜品「kung-pao-chicken-it」。 */
  private boolean userListContainsSeededDish() {
    ResponseEntity<Map> listResp = restTemplate.getForEntity(
        "/user/dish/list?categoryId=20", Map.class);
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<?> dishes = (List<?>) listResp.getBody().get("data");
    return dishes != null && dishes.stream()
        .map(d -> (Map<?, ?>) d)
        .anyMatch(d -> "kung-pao-chicken-it".equals(d.get("name")));
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

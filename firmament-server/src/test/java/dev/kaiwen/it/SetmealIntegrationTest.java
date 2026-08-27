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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * 套餐接口 REST 集成测试，对标 {@link DishIntegrationTest}：新增含菜品、用户浏览、缓存失效、启停/删除约束。
 */
@Sql(scripts = {"/sql/cleanup.sql", "/sql/data-employee.sql", "/sql/data-dish.sql"})
class SetmealIntegrationTest extends IntegrationTestBase {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void createSetmealWithDishesThenUserListsIt() throws Exception {
    String name = "it-setmeal-" + System.nanoTime();
    HttpHeaders adminH = adminHeaders(loginAdmin().token);
    long id = createOnSaleSetmeal(adminH, name);

    ResponseEntity<Map> listResp = restTemplate.getForEntity(
        "/user/setmeal/list?categoryId=21", Map.class);
    assertThat(listResp.getBody().get("code")).isEqualTo(1);
    List<?> setmeals = (List<?>) listResp.getBody().get("data");
    List<Object> names = setmeals.stream()
        .<Object>map(s -> ((Map<?, ?>) s).get("name"))
        .toList();
    assertThat(names).contains(name);

    ResponseEntity<Map> dishes = restTemplate.getForEntity(
        "/user/setmeal/dish/{id}", Map.class, id);
    assertThat(dishes.getBody().get("code")).isEqualTo(1);
    List<?> dishItems = (List<?>) dishes.getBody().get("data");
    assertThat(dishItems).isNotEmpty();
    assertThat(((Map<?, ?>) dishItems.get(0)).get("name")).isEqualTo("kung-pao-chicken-it");
  }

  @Test
  void getSetmealByIdAndPageQuery() throws Exception {
    String name = "it-setmeal-page-" + System.nanoTime();
    HttpHeaders adminH = adminHeaders(loginAdmin().token);
    long id = createOnSaleSetmeal(adminH, name);

    ResponseEntity<Map> byId = restTemplate.exchange(
        "/admin/setmeal/{id}", HttpMethod.GET, new HttpEntity<>(adminH), Map.class, id);
    assertThat(byId.getBody().get("code")).isEqualTo(1);
    Map<?, ?> data = (Map<?, ?>) byId.getBody().get("data");
    assertThat(data.get("name")).isEqualTo(name);
    assertThat((List<?>) data.get("setmealDishes")).isNotEmpty();

    ResponseEntity<Map> page = restTemplate.exchange(
        "/admin/setmeal/page?page=1&pageSize=100", HttpMethod.GET,
        new HttpEntity<>(adminH), Map.class);
    assertThat(page.getBody().get("code")).isEqualTo(1);
    Map<?, ?> pageData = (Map<?, ?>) page.getBody().get("data");
    assertThat(asLong(pageData.get("total"))).isGreaterThanOrEqualTo(1);
  }

  @Test
  void stopSellingHidesSetmealFromUserList() throws Exception {
    String name = "it-setmeal-stop-" + System.nanoTime();
    HttpHeaders adminH = adminHeaders(loginAdmin().token);
    long id = createOnSaleSetmeal(adminH, name);

    assertThat(userListContains(name)).isTrue();

    ResponseEntity<Map> stop = restTemplate.exchange(
        "/admin/setmeal/status/{status}?id={id}", HttpMethod.POST,
        new HttpEntity<>(adminH), Map.class, 0, id);
    assertThat(stop.getBody().get("code"))
        .as("msg=%s", stop.getBody().get("msg")).isEqualTo(1);

    assertThat(userListContains(name))
        .as("停售套餐不应出现在用户端列表（缓存未失效时会命中旧数据）").isFalse();
  }

  @Test
  void deleteEnabledSetmealFailsThenSucceedsAfterStop() throws Exception {
    String name = "it-setmeal-del-" + System.nanoTime();
    HttpHeaders adminH = adminHeaders(loginAdmin().token);
    long id = createOnSaleSetmeal(adminH, name);

    ResponseEntity<Map> deleteOnSale = restTemplate.exchange(
        "/admin/setmeal?ids={id}", HttpMethod.DELETE,
        new HttpEntity<>(adminH), Map.class, id);
    assertThat(deleteOnSale.getBody().get("code")).isEqualTo(0);
    assertThat(String.valueOf(deleteOnSale.getBody().get("msg"))).contains("起售");

    assertCode(restTemplate.exchange(
        "/admin/setmeal/status/{status}?id={id}", HttpMethod.POST,
        new HttpEntity<>(adminH), Map.class, 0, id), 1);

    assertCode(restTemplate.exchange(
        "/admin/setmeal?ids={id}", HttpMethod.DELETE,
        new HttpEntity<>(adminH), Map.class, id), 1);

    ResponseEntity<Map> page = restTemplate.exchange(
        "/admin/setmeal/page?page=1&pageSize=100&name={name}", HttpMethod.GET,
        new HttpEntity<>(adminH), Map.class, name);
    Map<?, ?> pageData = (Map<?, ?>) page.getBody().get("data");
    assertThat(asLong(pageData.get("total"))).isEqualTo(0);
  }

  @Test
  void enableSetmealFailsWhenDishDisabled() throws Exception {
    String name = "it-setmeal-enable-" + System.nanoTime();
    HttpHeaders adminH = adminHeaders(loginAdmin().token);
    long id = createSetmeal(adminH, name, 0);

    assertCode(restTemplate.exchange(
        "/admin/dish/status/{status}?id={id}", HttpMethod.POST,
        new HttpEntity<>(adminH), Map.class, 0, 200), 1);

    ResponseEntity<Map> enable = restTemplate.exchange(
        "/admin/setmeal/status/{status}?id={id}", HttpMethod.POST,
        new HttpEntity<>(adminH), Map.class, 1, id);
    assertThat(enable.getBody().get("code")).isEqualTo(0);
    assertThat(String.valueOf(enable.getBody().get("msg"))).contains("未启售");
  }

  @Test
  void updateSetmealReplacesSetmealDishes() throws Exception {
    String name = "it-setmeal-upd-" + System.nanoTime();
    HttpHeaders adminH = adminHeaders(loginAdmin().token);
    long id = createOnSaleSetmeal(adminH, name);

    Map<String, Object> body = Map.of(
        "id", id,
        "name", name + "-updated",
        "categoryId", 21,
        "price", new BigDecimal("99.00"),
        "status", 1,
        "description", "updated",
        "setmealDishes", List.of(Map.of(
            "dishId", 200,
            "name", "kung-pao-chicken-it",
            "price", new BigDecimal("38.00"),
            "copies", 3)));

    assertCode(restTemplate.exchange(
        "/admin/setmeal", HttpMethod.PUT,
        new HttpEntity<>(objectMapper.writeValueAsString(body), adminH), Map.class), 1);

    ResponseEntity<Map> byId = restTemplate.exchange(
        "/admin/setmeal/{id}", HttpMethod.GET, new HttpEntity<>(adminH), Map.class, id);
    Map<?, ?> data = (Map<?, ?>) byId.getBody().get("data");
    assertThat(data.get("name")).isEqualTo(name + "-updated");
    List<?> dishes = (List<?>) data.get("setmealDishes");
    assertThat(asInt(((Map<?, ?>) dishes.get(0)).get("copies"))).isEqualTo(3);
  }

  private long createOnSaleSetmeal(HttpHeaders adminH, String name) throws Exception {
    return createSetmeal(adminH, name, 1);
  }

  private long createSetmeal(HttpHeaders adminH, String name, int status) throws Exception {
    Map<String, Object> body = Map.of(
        "name", name,
        "categoryId", 21,
        "price", new BigDecimal("88.00"),
        "status", status,
        "description", "it-setmeal",
        "setmealDishes", List.of(Map.of(
            "dishId", 200,
            "name", "kung-pao-chicken-it",
            "price", new BigDecimal("38.00"),
            "copies", 2)));
    ResponseEntity<Map> save = restTemplate.exchange(
        "/admin/setmeal", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(body), adminH), Map.class);
    assertThat(save.getBody().get("code"))
        .as("msg=%s", save.getBody().get("msg")).isEqualTo(1);

    ResponseEntity<Map> page = restTemplate.exchange(
        "/admin/setmeal/page?page=1&pageSize=100&name={name}", HttpMethod.GET,
        new HttpEntity<>(adminH), Map.class, name);
    List<?> records = (List<?>) ((Map<?, ?>) page.getBody().get("data")).get("records");
    return asLong(((Map<?, ?>) records.get(0)).get("id"));
  }

  private boolean userListContains(String name) {
    ResponseEntity<Map> listResp = restTemplate.getForEntity(
        "/user/setmeal/list?categoryId=21", Map.class);
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<?> setmeals = (List<?>) listResp.getBody().get("data");
    return setmeals != null && setmeals.stream()
        .map(s -> (Map<?, ?>) s)
        .anyMatch(s -> name.equals(s.get("name")));
  }

  @SuppressWarnings("rawtypes")
  private void assertCode(ResponseEntity<Map> resp, int expected) {
    assertThat(resp.getBody().get("code"))
        .as("msg=%s", resp.getBody().get("msg"))
        .isEqualTo(expected);
  }
}

package dev.kaiwen.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * C端购物车 REST 集成测试：add 后 sub 减数量、减到 0 删除条目、clean 清空。
 */
@Sql(scripts = {"/sql/cleanup.sql", "/sql/data-user.sql", "/sql/data-dish.sql"})
class ShoppingCartIntegrationTest extends IntegrationTestBase {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void addSubThenClean() throws Exception {
    HttpHeaders userH = userHeaders(loginUser());
    Map<String, Object> dish = new HashMap<>();
    dish.put("dishId", 200);

    assertCode(restTemplate.exchange(
        "/user/shoppingCart/add", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(dish), userH), Map.class), 1);
    assertCode(restTemplate.exchange(
        "/user/shoppingCart/add", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(dish), userH), Map.class), 1);

    List<?> afterAdd = cartList(userH);
    assertThat(afterAdd).hasSize(1);
    assertThat(asInt(((Map<?, ?>) afterAdd.get(0)).get("number"))).isEqualTo(2);

    assertCode(restTemplate.exchange(
        "/user/shoppingCart/sub", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(dish), userH), Map.class), 1);
    List<?> afterSub = cartList(userH);
    assertThat(afterSub).hasSize(1);
    assertThat(asInt(((Map<?, ?>) afterSub.get(0)).get("number"))).isEqualTo(1);

    assertCode(restTemplate.exchange(
        "/user/shoppingCart/sub", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(dish), userH), Map.class), 1);
    assertThat(cartList(userH)).isEmpty();

    assertCode(restTemplate.exchange(
        "/user/shoppingCart/add", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(dish), userH), Map.class), 1);
    assertThat(cartList(userH)).hasSize(1);

    assertCode(restTemplate.exchange(
        "/user/shoppingCart/clean", HttpMethod.DELETE,
        new HttpEntity<>(userH), Map.class), 1);
    assertThat(cartList(userH)).isEmpty();
  }

  private List<?> cartList(HttpHeaders userH) {
    ResponseEntity<Map> resp = restTemplate.exchange(
        "/user/shoppingCart/list", HttpMethod.GET, new HttpEntity<>(userH), Map.class);
    assertThat(resp.getBody().get("code")).isEqualTo(1);
    return (List<?>) resp.getBody().get("data");
  }

  @SuppressWarnings("rawtypes")
  private void assertCode(ResponseEntity<Map> resp, int expected) {
    assertThat(resp.getBody().get("code"))
        .as("msg=%s", resp.getBody().get("msg"))
        .isEqualTo(expected);
  }
}

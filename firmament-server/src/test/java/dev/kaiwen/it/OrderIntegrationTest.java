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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * 订单全生命周期 REST 集成测试：用户下单→支付→管理端接单/派送/完成，以及取消/拒单/再来一单。
 *
 * <p>Token 一律走登录接口签发，不使用 {@code jwtService.createJwt} 自铸。
 */
@Sql(scripts = {
    "/sql/cleanup.sql",
    "/sql/data-employee.sql",
    "/sql/data-user.sql",
    "/sql/data-dish.sql",
    "/sql/data-order.sql"
})
class OrderIntegrationTest extends IntegrationTestBase {

  private static final long ADDRESS_ID = 1000L;
  private static final long DISH_ID = 200L;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void fullLifecycleSubmitPayConfirmDeliveryComplete() throws Exception {
    String userToken = loginUser();
    AdminLogin admin = loginAdmin();
    HttpHeaders userH = userHeaders(userToken);
    HttpHeaders adminH = adminHeaders(admin.token);

    addDishToCart(userH);
    Map<?, ?> submit = submitOrder(userH);
    String orderNumber = (String) submit.get("orderNumber");
    long orderId = asLong(submit.get("id"));
    assertThat(orderNumber).isNotBlank();

    Map<?, ?> pending = orderDetail(userH, orderNumber);
    assertThat(asInt(pending.get("status"))).isEqualTo(1);
    assertThat(asInt(pending.get("payStatus"))).isEqualTo(0);

    ResponseEntity<Map> cartAfterSubmit = restTemplate.exchange(
        "/user/shoppingCart/list", HttpMethod.GET, new HttpEntity<>(userH), Map.class);
    assertThat((List<?>) cartAfterSubmit.getBody().get("data")).isEmpty();

    pay(userH, orderNumber);
    Map<?, ?> paid = orderDetail(userH, orderNumber);
    assertThat(asInt(paid.get("status"))).isEqualTo(2);
    assertThat(asInt(paid.get("payStatus"))).isEqualTo(1);

    ResponseEntity<Map> history = restTemplate.exchange(
        "/user/order/historyOrders?page=1&pageSize=10", HttpMethod.GET,
        new HttpEntity<>(userH), Map.class);
    assertCode(history, 1);
    Map<?, ?> historyData = (Map<?, ?>) history.getBody().get("data");
    assertThat(asLong(historyData.get("total"))).isGreaterThanOrEqualTo(1);

    assertCode(restTemplate.exchange(
        "/admin/order/confirm", HttpMethod.PUT,
        new HttpEntity<>(objectMapper.writeValueAsString(Map.of("id", orderId)), adminH),
        Map.class), 1);
    assertThat(asInt(orderDetail(userH, orderNumber).get("status"))).isEqualTo(3);

    assertCode(restTemplate.exchange(
        "/admin/order/delivery/{id}", HttpMethod.PUT,
        new HttpEntity<>(adminH), Map.class, orderId), 1);
    assertThat(asInt(orderDetail(userH, orderNumber).get("status"))).isEqualTo(4);

    assertCode(restTemplate.exchange(
        "/admin/order/complete/{id}", HttpMethod.PUT,
        new HttpEntity<>(adminH), Map.class, orderId), 1);
    Map<?, ?> completed = orderDetail(userH, orderNumber);
    assertThat(asInt(completed.get("status"))).isEqualTo(5);
    assertThat(completed.get("deliveryTime")).isNotNull();

    ResponseEntity<Map> search = restTemplate.exchange(
        "/admin/order/conditionSearch?page=1&pageSize=10&number={number}",
        HttpMethod.GET, new HttpEntity<>(adminH), Map.class, orderNumber);
    assertCode(search, 1);
    Map<?, ?> searchData = (Map<?, ?>) search.getBody().get("data");
    assertThat(asLong(searchData.get("total"))).isEqualTo(1);

    ResponseEntity<Map> details = restTemplate.exchange(
        "/admin/order/details/{id}", HttpMethod.GET,
        new HttpEntity<>(adminH), Map.class, orderId);
    assertCode(details, 1);
    assertThat(((Map<?, ?>) details.getBody().get("data")).get("number"))
        .isEqualTo(orderNumber);

    ResponseEntity<Map> stats = restTemplate.exchange(
        "/admin/order/statistics", HttpMethod.GET, new HttpEntity<>(adminH), Map.class);
    assertCode(stats, 1);
    Map<?, ?> statsData = (Map<?, ?>) stats.getBody().get("data");
    assertThat(statsData.get("toBeConfirmed")).isNotNull();
    assertThat(statsData.get("confirmed")).isNotNull();
    assertThat(statsData.get("deliveryInProgress")).isNotNull();
  }

  @Test
  void userCancelAfterPayThenRepetitionRefillsCart() throws Exception {
    String userToken = loginUser();
    HttpHeaders userH = userHeaders(userToken);

    addDishToCart(userH);
    String orderNumber = (String) submitOrder(userH).get("orderNumber");
    pay(userH, orderNumber);

    assertCode(restTemplate.exchange(
        "/user/order/cancel/number/{orderNumber}", HttpMethod.PUT,
        new HttpEntity<>(userH), Map.class, orderNumber), 1);

    Map<?, ?> cancelled = orderDetail(userH, orderNumber);
    assertThat(asInt(cancelled.get("status"))).isEqualTo(6);
    assertThat(asInt(cancelled.get("payStatus"))).isEqualTo(2);

    assertCode(restTemplate.exchange(
        "/user/order/repetition/number/{orderNumber}", HttpMethod.POST,
        new HttpEntity<>(userH), Map.class, orderNumber), 1);

    ResponseEntity<Map> cart = restTemplate.exchange(
        "/user/shoppingCart/list", HttpMethod.GET, new HttpEntity<>(userH), Map.class);
    assertCode(cart, 1);
    assertThat((List<?>) cart.getBody().get("data")).isNotEmpty();
  }

  @Test
  void adminRejectsPaidOrder() throws Exception {
    String userToken = loginUser();
    AdminLogin admin = loginAdmin();
    HttpHeaders userH = userHeaders(userToken);
    HttpHeaders adminH = adminHeaders(admin.token);

    addDishToCart(userH);
    Map<?, ?> submit = submitOrder(userH);
    String orderNumber = (String) submit.get("orderNumber");
    long orderId = asLong(submit.get("id"));
    pay(userH, orderNumber);

    Map<String, Object> body = Map.of("id", orderId, "rejectionReason", "out-of-stock-it");
    assertCode(restTemplate.exchange(
        "/admin/order/rejection", HttpMethod.PUT,
        new HttpEntity<>(objectMapper.writeValueAsString(body), adminH), Map.class), 1);

    Map<?, ?> rejected = orderDetail(userH, orderNumber);
    assertThat(asInt(rejected.get("status"))).isEqualTo(6);
    assertThat(asInt(rejected.get("payStatus"))).isEqualTo(2);
    assertThat(rejected.get("rejectionReason")).isEqualTo("out-of-stock-it");
  }

  @Test
  void reminderThenAdminCancelPaidOrder() throws Exception {
    String userToken = loginUser();
    AdminLogin admin = loginAdmin();
    HttpHeaders userH = userHeaders(userToken);
    HttpHeaders adminH = adminHeaders(admin.token);

    addDishToCart(userH);
    Map<?, ?> submit = submitOrder(userH);
    String orderNumber = (String) submit.get("orderNumber");
    long orderId = asLong(submit.get("id"));
    pay(userH, orderNumber);

    assertCode(restTemplate.exchange(
        "/user/order/reminder/number/{orderNumber}", HttpMethod.GET,
        new HttpEntity<>(userH), Map.class, orderNumber), 1);

    Map<String, Object> body = Map.of("id", orderId, "cancelReason", "admin-cancel-it");
    assertCode(restTemplate.exchange(
        "/admin/order/cancel", HttpMethod.PUT,
        new HttpEntity<>(objectMapper.writeValueAsString(body), adminH), Map.class), 1);

    Map<?, ?> cancelled = orderDetail(userH, orderNumber);
    assertThat(asInt(cancelled.get("status"))).isEqualTo(6);
    assertThat(cancelled.get("cancelReason")).isEqualTo("admin-cancel-it");
  }

  private void addDishToCart(HttpHeaders userH) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("dishId", DISH_ID);
    body.put("dishFlavor", "mild");
    assertCode(restTemplate.exchange(
        "/user/shoppingCart/add", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(body), userH), Map.class), 1);
  }

  private Map<?, ?> submitOrder(HttpHeaders userH) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("addressBookId", ADDRESS_ID);
    body.put("payMethod", 1);
    body.put("packAmount", 0);
    body.put("deliveryStatus", 1);
    body.put("tablewareStatus", 1);
    ResponseEntity<Map> resp = restTemplate.exchange(
        "/user/order/submit", HttpMethod.POST,
        new HttpEntity<>(objectMapper.writeValueAsString(body), userH), Map.class);
    return requireSuccessData(resp, "submit order");
  }

  private void pay(HttpHeaders userH, String orderNumber) throws Exception {
    Map<String, Object> body = Map.of("orderNumber", orderNumber, "payMethod", 1);
    ResponseEntity<Map> resp = restTemplate.exchange(
        "/user/order/payment", HttpMethod.PUT,
        new HttpEntity<>(objectMapper.writeValueAsString(body), userH), Map.class);
    assertCode(resp, 1);
    assertThat(resp.getBody().get("data")).isEqualTo("支付成功");
  }

  private Map<?, ?> orderDetail(HttpHeaders userH, String orderNumber) {
    ResponseEntity<Map> resp = restTemplate.exchange(
        "/user/order/orderDetail/number/{orderNumber}", HttpMethod.GET,
        new HttpEntity<>(userH), Map.class, orderNumber);
    return requireSuccessData(resp, "order detail " + orderNumber);
  }

  @SuppressWarnings("rawtypes")
  private void assertCode(ResponseEntity<Map> resp, int expected) {
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().get("code"))
        .as("msg=%s", resp.getBody().get("msg"))
        .isEqualTo(expected);
  }
}

package dev.kaiwen.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * 管理端工作台聚合接口的 REST 集成测试。
 */
@Sql(scripts = {
    "/sql/cleanup.sql",
    "/sql/data-employee.sql",
    "/sql/data-user.sql",
    "/sql/data-dish.sql",
    "/sql/data-report.sql"
})
class WorkspaceIntegrationTest extends IntegrationTestBase {

  @Test
  void businessDataReflectsTodayCompletedOrders() {
    Map<?, ?> data = getWorkspace("/admin/workspace/businessData");
    assertThat(((Number) data.get("turnover")).doubleValue()).isEqualTo(100.0);
    assertThat(asInt(data.get("validOrderCount"))).isEqualTo(1);
    assertThat(((Number) data.get("orderCompletionRate")).doubleValue()).isEqualTo(0.25);
    assertThat(((Number) data.get("unitPrice")).doubleValue()).isEqualTo(100.0);
    assertThat(asInt(data.get("newUsers"))).isEqualTo(1);
  }

  @Test
  void overviewOrdersCountsByStatusToday() {
    Map<?, ?> data = getWorkspace("/admin/workspace/overviewOrders");
    assertThat(asInt(data.get("waitingOrders"))).isEqualTo(1);
    assertThat(asInt(data.get("deliveredOrders"))).isEqualTo(1);
    assertThat(asInt(data.get("completedOrders"))).isEqualTo(1);
    assertThat(asInt(data.get("cancelledOrders"))).isEqualTo(1);
    assertThat(asInt(data.get("allOrders"))).isEqualTo(4);
  }

  @Test
  void overviewDishesCountsEnabledAndDisabled() {
    Map<?, ?> data = getWorkspace("/admin/workspace/overviewDishes");
    assertThat(asInt(data.get("sold"))).isEqualTo(1);
    assertThat(asInt(data.get("discontinued"))).isEqualTo(1);
  }

  @Test
  void overviewSetmealsCountsEnabledAndDisabled() {
    Map<?, ?> data = getWorkspace("/admin/workspace/overviewSetmeals");
    assertThat(asInt(data.get("sold"))).isEqualTo(1);
    assertThat(asInt(data.get("discontinued"))).isEqualTo(1);
  }

  @SuppressWarnings("rawtypes")
  private Map<?, ?> getWorkspace(String path) {
    HttpHeaders headers = adminHeaders(loginAdmin().token);
    ResponseEntity<Map> resp = restTemplate.exchange(
        path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    return requireSuccessData(resp, path);
  }
}

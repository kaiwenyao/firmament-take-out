package dev.kaiwen.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * 管理端报表聚合接口的 REST 集成测试：真实 MySQL 查询 + 内存按日聚合。
 */
@Sql(scripts = {
    "/sql/cleanup.sql",
    "/sql/data-employee.sql",
    "/sql/data-user.sql",
    "/sql/data-dish.sql",
    "/sql/data-report.sql"
})
class ReportIntegrationTest extends IntegrationTestBase {

  @Test
  void turnoverStatisticsAggregatesCompletedOrders() {
    LocalDate end = LocalDate.now();
    LocalDate begin = end.minusDays(1);
    Map<?, ?> data = getReport("/admin/report/turnoverStatistics", begin, end);
    assertThat((String) data.get("dateList")).isNotBlank();
    assertThat((String) data.get("turnoverList")).isNotBlank();
    assertThat((String) data.get("turnoverList")).contains("100");
  }

  @Test
  void userStatisticsReturnsDailyNewAndCumulativeCounts() {
    LocalDate end = LocalDate.now();
    LocalDate begin = end.minusDays(1);
    Map<?, ?> data = getReport("/admin/report/userStatistics", begin, end);
    assertThat((String) data.get("newUserList")).isNotBlank();
    assertThat((String) data.get("totalUserList")).isNotBlank();
  }

  @Test
  void ordersStatisticsComputesCompletionRate() {
    LocalDate end = LocalDate.now();
    LocalDate begin = end.minusDays(1);
    Map<?, ?> data = getReport("/admin/report/ordersStatistics", begin, end);
    assertThat(asInt(data.get("totalOrderCount"))).isGreaterThanOrEqualTo(2);
    assertThat(asInt(data.get("validOrderCount"))).isGreaterThanOrEqualTo(2);
    assertThat(data.get("orderCompletionRate")).isNotNull();
  }

  @Test
  void top10ReturnsDishNamesBySalesVolume() {
    LocalDate end = LocalDate.now();
    LocalDate begin = end.minusDays(1);
    Map<?, ?> data = getReport("/admin/report/top10", begin, end);
    assertThat((String) data.get("nameList")).contains("it-top-dish");
    assertThat((String) data.get("numberList")).isNotBlank();
  }

  @Test
  void exportReturnsXlsxContentTypeAndNonEmptyBody() {
    HttpHeaders headers = adminHeaders(loginAdmin().token);
    ResponseEntity<byte[]> resp = restTemplate.exchange(
        "/admin/report/export", HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    MediaType contentType = resp.getHeaders().getContentType();
    assertThat(contentType).isNotNull();
    assertThat(contentType.toString())
        .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    assertThat(resp.getBody()).isNotEmpty();
  }

  @SuppressWarnings("rawtypes")
  private Map<?, ?> getReport(String path, LocalDate begin, LocalDate end) {
    HttpHeaders headers = adminHeaders(loginAdmin().token);
    ResponseEntity<Map> resp = restTemplate.exchange(
        path + "?begin={begin}&end={end}", HttpMethod.GET,
        new HttpEntity<>(headers), Map.class, begin.toString(), end.toString());
    return requireSuccessData(resp, path);
  }
}

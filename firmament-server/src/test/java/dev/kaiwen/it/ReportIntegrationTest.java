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
    assertThat(data.get("dateList")).isEqualTo(begin + "," + end);
    assertThat(data.get("turnoverList")).isEqualTo("50.00,100.00");
  }

  @Test
  void userStatisticsReturnsDailyNewAndCumulativeCounts() {
    LocalDate end = LocalDate.now();
    LocalDate begin = end.minusDays(1);
    Map<?, ?> data = getReport("/admin/report/userStatistics", begin, end);
    assertThat(data.get("dateList")).isEqualTo(begin + "," + end);
    assertThat(data.get("newUserList")).isEqualTo("1,1");
    assertThat(data.get("totalUserList")).isEqualTo("1,2");
  }

  @Test
  void ordersStatisticsComputesCompletionRate() {
    LocalDate end = LocalDate.now();
    LocalDate begin = end.minusDays(1);
    Map<?, ?> data = getReport("/admin/report/ordersStatistics", begin, end);
    assertThat(data.get("dateList")).isEqualTo(begin + "," + end);
    assertThat(data.get("orderCountList")).isEqualTo("1,4");
    assertThat(data.get("validOrderCountList")).isEqualTo("1,1");
    assertThat(asInt(data.get("totalOrderCount"))).isEqualTo(5);
    assertThat(asInt(data.get("validOrderCount"))).isEqualTo(2);
    assertThat(((Number) data.get("orderCompletionRate")).doubleValue()).isEqualTo(0.4);
  }

  @Test
  void top10ReturnsDishNamesBySalesVolume() {
    LocalDate end = LocalDate.now();
    LocalDate begin = end.minusDays(1);
    Map<?, ?> data = getReport("/admin/report/top10", begin, end);
    assertThat(data.get("nameList")).isEqualTo("it-top-dish,it-second-dish");
    assertThat(data.get("numberList")).isEqualTo("4,2");
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

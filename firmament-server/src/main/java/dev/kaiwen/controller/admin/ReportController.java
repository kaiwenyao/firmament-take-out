package dev.kaiwen.controller.admin;

import dev.kaiwen.result.Result;
import dev.kaiwen.service.ReportService;
import dev.kaiwen.vo.OrderReportVo;
import dev.kaiwen.vo.SalesTop10ReportVo;
import dev.kaiwen.vo.TurnoverReportVo;
import dev.kaiwen.vo.UserReportVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report controller.
 */
@RestController
@RequestMapping("/admin/report")
@Tag(name = "数据统计相关接口")
@Slf4j
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  /**
   * Get turnover statistics.
   *
   * @param begin The start date of the statistics period.
   * @param end   The end date of the statistics period.
   * @return The turnover statistics report.
   */
  @GetMapping("/turnoverStatistics")
  @Operation(summary = "营业额统计")
  public Result<TurnoverReportVo> turnoverStatistics(
      @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
      @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
    log.info("营业额数据统计: {}, {}", begin, end);
    // 参数验证在Service层进行
    return Result.success(reportService.getTurnoverStatistics(begin, end));

  }

  /**
   * Get user statistics.
   *
   * @param begin The start date of the statistics period.
   * @param end   The end date of the statistics period.
   * @return The user statistics report.
   */
  @GetMapping("/userStatistics")
  @Operation(summary = "用户数据统计")
  public Result<UserReportVo> userStatistics(
      @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
      @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
    log.info("用户数据统计: {}, {}", begin, end);
    return Result.success(reportService.getUserStatistics(begin, end));
  }

  /**
   * Get order statistics.
   *
   * @param begin The start date of the statistics period.
   * @param end   The end date of the statistics period.
   * @return The order statistics report.
   */
  @GetMapping("/ordersStatistics")
  @Operation(summary = "订单数据统计")
  public Result<OrderReportVo> ordersStatistics(
      @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
      @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
    log.info("订单数据统计: {}, {}", begin, end);
    return Result.success(reportService.getOrderStatistics(begin, end));
  }

  /**
   * Get top 10 sales statistics.
   *
   * @param begin The start date of the statistics period.
   * @param end   The end date of the statistics period.
   * @return The top 10 sales statistics report.
   */
  @GetMapping("/top10")
  @Operation(summary = "销量top10")
  public Result<SalesTop10ReportVo> top10(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
      @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
    log.info("销量top10统计: {}, {}", begin, end);
    return Result.success(reportService.getSalesTop10(begin, end));
  }

  /**
   * Export business data report for the last 30 days.
   *
   * @param response The HTTP servlet response to write the Excel file.
   * @throws IOException If export fails.
   */
  @GetMapping("/export")
  @Operation(summary = "导出最近30天的数据报表")
  public void export(HttpServletResponse response) throws IOException {
    log.info("导出最近30天的数据报表");



    // 设置响应头
    String fileName =
        "运营数据报表_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
    fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("\\+", "%20");
    // 调用service获取Excel数据
    byte[] excelBytes = reportService.exportBusinessData();
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
    response.setContentLength(excelBytes.length);

    // 写入响应流
    try (OutputStream outputStream = response.getOutputStream()) {
      outputStream.write(excelBytes);
      outputStream.flush();
    }
  }
}


package dev.kaiwen.controller.admin;


import dev.kaiwen.result.Result;
import dev.kaiwen.service.ReportService;
import dev.kaiwen.vo.OrderReportVO;
import dev.kaiwen.vo.SalesTop10ReportVO;
import dev.kaiwen.vo.TurnoverReportVO;
import dev.kaiwen.vo.UserReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/admin/report")
@Tag(name = "数据统计相关接口")
@Slf4j
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/turnoverStatistics")
    @Operation(summary = "营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end) {
        log.info("营业额数据统计: {}, {}", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));

    }

    @GetMapping("/userStatistics")
    @Operation(summary = "用户数据统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end
    ) {
        log.info("用户数据统计: {}, {}", begin, end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    @GetMapping("/ordersStatistics")
    @Operation(summary = "订单数据统计")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end
    ) {
        log.info("订单数据统计: {}, {}", begin, end);
        return Result.success(reportService.getOrderStatistics(begin, end));
    }

    @GetMapping("/top10")
    @Operation(summary = "销量top10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate end
    ) {
        log.info("销量top10统计: {}, {}", begin, end);
        return Result.success(reportService.getSalesTop10(begin, end));
    }

    @GetMapping("/export")
    @Operation(summary = "导出最近30天的数据报表")
    public void export(HttpServletResponse response) throws IOException {
        log.info("导出最近30天的数据报表");
        
        // 调用service获取Excel数据
        byte[] excelBytes = reportService.exportBusinessData();
        
        // 设置响应头
        String fileName = "运营数据报表_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        
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


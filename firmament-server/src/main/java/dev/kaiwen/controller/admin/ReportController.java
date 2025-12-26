package dev.kaiwen.controller.admin;


import dev.kaiwen.result.Result;
import dev.kaiwen.service.ReportService;
import dev.kaiwen.vo.TurnoverReportVO;
import dev.kaiwen.vo.UserReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

}


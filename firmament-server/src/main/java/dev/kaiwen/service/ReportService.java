package dev.kaiwen.service;


import dev.kaiwen.vo.TurnoverReportVO;
import dev.kaiwen.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);
}


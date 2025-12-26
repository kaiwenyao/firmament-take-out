package dev.kaiwen.service;


import dev.kaiwen.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface IReportService {
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);
}

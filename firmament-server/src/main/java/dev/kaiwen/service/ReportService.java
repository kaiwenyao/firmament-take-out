package dev.kaiwen.service;


import dev.kaiwen.vo.OrderReportVO;
import dev.kaiwen.vo.SalesTop10ReportVO;
import dev.kaiwen.vo.TurnoverReportVO;
import dev.kaiwen.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);

    /**
     * 导出最近30天的数据报表
     * @return Excel文件的字节数组
     */
    byte[] exportBusinessData();
}


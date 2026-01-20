package dev.kaiwen.service;


import dev.kaiwen.vo.OrderReportVo;
import dev.kaiwen.vo.SalesTop10ReportVo;
import dev.kaiwen.vo.TurnoverReportVo;
import dev.kaiwen.vo.UserReportVo;

import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVo getTurnoverStatistics(LocalDate begin, LocalDate end);

    UserReportVo getUserStatistics(LocalDate begin, LocalDate end);

    OrderReportVo getOrderStatistics(LocalDate begin, LocalDate end);

    SalesTop10ReportVo getSalesTop10(LocalDate begin, LocalDate end);

    /**
     * 导出最近30天的数据报表
     * @return Excel文件的字节数组
     */
    byte[] exportBusinessData();
}


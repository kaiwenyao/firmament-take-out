package dev.kaiwen.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.ReportService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.OrderReportVo;
import dev.kaiwen.vo.SalesTop10ReportVo;
import dev.kaiwen.vo.TurnoverReportVo;
import dev.kaiwen.vo.UserReportVo;
import io.jsonwebtoken.Claims;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

  @MockitoBean
  private ReportService reportService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private MockMvc mockMvc;

  private void setupJwtTokenMock() {
    given(jwtProperties.getAdminTokenName()).willReturn("token");
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn("1");
  }

  @Test
  void turnoverStatisticsSuccess() throws Exception {
    setupJwtTokenMock();

    TurnoverReportVo vo = TurnoverReportVo.builder()
        .dateList("2024-01-01,2024-01-02")
        .turnoverList("1000.0,2000.0")
        .build();
    given(
        reportService.getTurnoverStatistics(any(LocalDate.class), any(LocalDate.class))).willReturn(
        vo);

    mockMvc.perform(get("/admin/report/turnoverStatistics")
            .header("token", "mock-accessToken")
            .param("begin", "2024-01-01")
            .param("end", "2024-01-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.dateList").value("2024-01-01,2024-01-02"))
        .andExpect(jsonPath("$.data.turnoverList").value("1000.0,2000.0"));

    verify(reportService).getTurnoverStatistics(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 2)
    );
  }

  @Test
  void userStatisticsSuccess() throws Exception {
    setupJwtTokenMock();

    UserReportVo vo = UserReportVo.builder()
        .dateList("2024-01-01,2024-01-02")
        .totalUserList("100,110")
        .newUserList("10,10")
        .build();
    given(reportService.getUserStatistics(any(LocalDate.class), any(LocalDate.class))).willReturn(
        vo);

    mockMvc.perform(get("/admin/report/userStatistics")
            .header("token", "mock-accessToken")
            .param("begin", "2024-01-01")
            .param("end", "2024-01-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.dateList").value("2024-01-01,2024-01-02"))
        .andExpect(jsonPath("$.data.totalUserList").value("100,110"))
        .andExpect(jsonPath("$.data.newUserList").value("10,10"));

    verify(reportService).getUserStatistics(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 2)
    );
  }

  @Test
  void ordersStatisticsSuccess() throws Exception {
    setupJwtTokenMock();

    OrderReportVo vo = OrderReportVo.builder()
        .dateList("2024-01-01,2024-01-02")
        .orderCountList("50,60")
        .validOrderCountList("45,55")
        .totalOrderCount(110)
        .validOrderCount(100)
        .orderCompletionRate(90.9)
        .build();
    given(reportService.getOrderStatistics(any(LocalDate.class), any(LocalDate.class))).willReturn(
        vo);

    mockMvc.perform(get("/admin/report/ordersStatistics")
            .header("token", "mock-accessToken")
            .param("begin", "2024-01-01")
            .param("end", "2024-01-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.totalOrderCount").value(110))
        .andExpect(jsonPath("$.data.validOrderCount").value(100))
        .andExpect(jsonPath("$.data.orderCompletionRate").value(90.9));

    verify(reportService).getOrderStatistics(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 2)
    );
  }

  @Test
  void top10Success() throws Exception {
    setupJwtTokenMock();

    SalesTop10ReportVo vo = SalesTop10ReportVo.builder()
        .nameList("鱼香肉丝,宫保鸡丁")
        .numberList("100,80")
        .build();
    given(reportService.getSalesTop10(any(LocalDate.class), any(LocalDate.class))).willReturn(vo);

    mockMvc.perform(get("/admin/report/top10")
            .header("token", "mock-accessToken")
            .param("begin", "2024-01-01")
            .param("end", "2024-01-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.nameList").value("鱼香肉丝,宫保鸡丁"))
        .andExpect(jsonPath("$.data.numberList").value("100,80"));

    verify(reportService).getSalesTop10(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 2)
    );
  }

  @Test
  void exportSuccess() throws Exception {
    setupJwtTokenMock();

    byte[] excelBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
    given(reportService.exportBusinessData()).willReturn(excelBytes);

    mockMvc.perform(get("/admin/report/export").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .andExpect(header().exists("Content-Disposition"))
        .andExpect(content().bytes(excelBytes));

    verify(reportService).exportBusinessData();
  }

  @Test
  void turnoverStatisticsWhenServiceThrows() throws Exception {
    setupJwtTokenMock();

    given(reportService.getTurnoverStatistics(any(LocalDate.class), any(LocalDate.class)))
        .willThrow(new RuntimeException("数据库异常"));

    mockMvc.perform(get("/admin/report/turnoverStatistics")
            .header("token", "mock-accessToken")
            .param("begin", "2024-01-01")
            .param("end", "2024-01-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  @Test
  void exportWhenServiceThrows() throws Exception {
    setupJwtTokenMock();

    given(reportService.exportBusinessData()).willThrow(new RuntimeException("导出失败"));

    mockMvc.perform(get("/admin/report/export").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }
}

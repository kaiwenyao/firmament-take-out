package dev.kaiwen.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.handler.GlobalExceptionHandler;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.WorkspaceService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.BusinessDataVo;
import dev.kaiwen.vo.DishOverViewVo;
import dev.kaiwen.vo.OrderOverViewVo;
import dev.kaiwen.vo.SetmealOverViewVo;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceController.class)
class WorkspaceControllerTest {

  @MockitoBean
  private WorkspaceService workspaceService;

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
  void businessDataSuccess() throws Exception {
    setupJwtTokenMock();

    BusinessDataVo vo = BusinessDataVo.builder()
        .turnover(5000.0)
        .validOrderCount(100)
        .orderCompletionRate(95.5)
        .unitPrice(50.0)
        .newUsers(20)
        .build();
    given(workspaceService.getBusinessData(any(LocalDateTime.class),
        any(LocalDateTime.class))).willReturn(vo);

    mockMvc.perform(get("/admin/workspace/businessData").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.turnover").value(5000.0))
        .andExpect(jsonPath("$.data.validOrderCount").value(100))
        .andExpect(jsonPath("$.data.orderCompletionRate").value(95.5))
        .andExpect(jsonPath("$.data.unitPrice").value(50.0))
        .andExpect(jsonPath("$.data.newUsers").value(20));

    verify(workspaceService).getBusinessData(any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  void orderOverViewSuccess() throws Exception {
    setupJwtTokenMock();

    OrderOverViewVo vo = OrderOverViewVo.builder()
        .waitingOrders(5)
        .deliveredOrders(3)
        .completedOrders(100)
        .cancelledOrders(2)
        .allOrders(110)
        .build();
    given(workspaceService.getOrderOverView()).willReturn(vo);

    mockMvc.perform(get("/admin/workspace/overviewOrders").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.waitingOrders").value(5))
        .andExpect(jsonPath("$.data.deliveredOrders").value(3))
        .andExpect(jsonPath("$.data.completedOrders").value(100))
        .andExpect(jsonPath("$.data.cancelledOrders").value(2))
        .andExpect(jsonPath("$.data.allOrders").value(110));

    verify(workspaceService).getOrderOverView();
  }

  @Test
  void dishOverViewSuccess() throws Exception {
    setupJwtTokenMock();

    DishOverViewVo vo = DishOverViewVo.builder()
        .sold(50)
        .discontinued(5)
        .build();
    given(workspaceService.getDishOverView()).willReturn(vo);

    mockMvc.perform(get("/admin/workspace/overviewDishes").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.sold").value(50))
        .andExpect(jsonPath("$.data.discontinued").value(5));

    verify(workspaceService).getDishOverView();
  }

  @Test
  void setmealOverViewSuccess() throws Exception {
    setupJwtTokenMock();

    SetmealOverViewVo vo = SetmealOverViewVo.builder()
        .sold(10)
        .discontinued(2)
        .build();
    given(workspaceService.getSetmealOverView()).willReturn(vo);

    mockMvc.perform(get("/admin/workspace/overviewSetmeals").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.sold").value(10))
        .andExpect(jsonPath("$.data.discontinued").value(2));

    verify(workspaceService).getSetmealOverView();
  }

  @Test
  void businessDataWhenServiceThrows() throws Exception {
    setupJwtTokenMock();

    given(workspaceService.getBusinessData(any(LocalDateTime.class), any(LocalDateTime.class)))
        .willThrow(new RuntimeException("查询失败"));

    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(get("/admin/workspace/businessData").header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0));
    } finally {
      logger.setLevel(originalLevel);
    }
  }

  @Test
  void orderOverViewWhenServiceThrows() throws Exception {
    setupJwtTokenMock();

    given(workspaceService.getOrderOverView()).willThrow(new RuntimeException("查询失败"));

    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(get("/admin/workspace/overviewOrders").header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0));
    } finally {
      logger.setLevel(originalLevel);
    }
  }
}

package dev.kaiwen.controller.admin;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.OrdersCancelDto;
import dev.kaiwen.dto.OrdersConfirmDto;
import dev.kaiwen.dto.OrdersPageQueryDto;
import dev.kaiwen.dto.OrdersRejectionDto;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.OrderStatisticsVo;
import dev.kaiwen.vo.OrderVo;
import io.jsonwebtoken.Claims;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

  @MockitoBean
  private OrderService orderService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  private void setupJwtTokenMock(Long empId) {
    given(jwtProperties.getAdminTokenName()).willReturn("token");
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(empId.toString());
  }

  @Test
  void conditionSearchSuccess() throws Exception {
    Long empId = 1L;
    setupJwtTokenMock(empId);

    PageResult pageResult = new PageResult(0L, new ArrayList<>());
    given(orderService.conditionSearch(any(OrdersPageQueryDto.class))).willReturn(pageResult);

    mockMvc.perform(get("/admin/order/conditionSearch")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(0))
        .andExpect(jsonPath("$.data.records").isArray());

    verify(orderService).conditionSearch(any(OrdersPageQueryDto.class));
  }

  @Test
  void conditionSearchWithResults() throws Exception {
    Long empId = 1L;
    setupJwtTokenMock(empId);

    OrderVo orderVo = new OrderVo();
    orderVo.setId(1L);
    orderVo.setNumber("ORDER001");
    orderVo.setStatus(2);
    List<OrderVo> orders = new ArrayList<>();
    orders.add(orderVo);

    PageResult pageResult = new PageResult(1L, orders);
    given(orderService.conditionSearch(any(OrdersPageQueryDto.class))).willReturn(pageResult);

    mockMvc.perform(get("/admin/order/conditionSearch")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10")
            .param("number", "ORDER001")
            .param("status", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records[0].id").value(1))
        .andExpect(jsonPath("$.data.records[0].number").value("ORDER001"))
        .andExpect(jsonPath("$.data.records[0].status").value(2));

    verify(orderService).conditionSearch(any(OrdersPageQueryDto.class));
  }

  @Test
  void statisticsSuccess() throws Exception {
    Long empId = 1L;
    setupJwtTokenMock(empId);

    OrderStatisticsVo statisticsVo = new OrderStatisticsVo();
    statisticsVo.setToBeConfirmed(5);
    statisticsVo.setConfirmed(3);
    statisticsVo.setDeliveryInProgress(2);
    given(orderService.statistics()).willReturn(statisticsVo);

    mockMvc.perform(get("/admin/order/statistics")
            .header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.toBeConfirmed").value(5))
        .andExpect(jsonPath("$.data.confirmed").value(3))
        .andExpect(jsonPath("$.data.deliveryInProgress").value(2));

    verify(orderService).statistics();
  }

  @Test
  void detailsSuccess() throws Exception {
    Long empId = 1L;
    Long orderId = 100L;

    setupJwtTokenMock(empId);

    OrderVo orderVo = new OrderVo();
    orderVo.setId(orderId);
    orderVo.setNumber("ORDER100");
    orderVo.setStatus(3);
    orderVo.setOrderDishes("菜品信息");
    given(orderService.details(orderId)).willReturn(orderVo);

    mockMvc.perform(get("/admin/order/details/{id}", orderId)
            .header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.number").value("ORDER100"))
        .andExpect(jsonPath("$.data.status").value(3))
        .andExpect(jsonPath("$.data.orderDishes").value("菜品信息"));

    verify(orderService).details(orderId);
  }

  @Test
  void confirmSuccess() throws Exception {
    Long empId = 1L;
    OrdersConfirmDto confirmDto = new OrdersConfirmDto();
    confirmDto.setId(100L);
    confirmDto.setStatus(3);

    setupJwtTokenMock(empId);

    mockMvc.perform(put("/admin/order/confirm")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(confirmDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(orderService).confirm(any(OrdersConfirmDto.class));
  }

  @Test
  void rejectionSuccess() throws Exception {
    Long empId = 1L;
    OrdersRejectionDto rejectionDto = new OrdersRejectionDto();
    rejectionDto.setId(100L);
    rejectionDto.setRejectionReason("商品已售罄");

    setupJwtTokenMock(empId);

    mockMvc.perform(put("/admin/order/rejection")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(rejectionDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(orderService).rejection(any(OrdersRejectionDto.class));
  }

  @Test
  void cancelSuccess() throws Exception {
    Long empId = 1L;
    OrdersCancelDto cancelDto = new OrdersCancelDto();
    cancelDto.setId(100L);
    cancelDto.setCancelReason("用户取消");

    setupJwtTokenMock(empId);

    mockMvc.perform(put("/admin/order/cancel")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cancelDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(orderService).cancel(any(OrdersCancelDto.class));
  }

  @Test
  void deliverySuccess() throws Exception {
    Long empId = 1L;
    Long orderId = 100L;

    setupJwtTokenMock(empId);

    mockMvc.perform(put("/admin/order/delivery/{id}", orderId)
            .header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(orderService).delivery(orderId);
  }

  @Test
  void completeSuccess() throws Exception {
    Long empId = 1L;
    Long orderId = 100L;

    setupJwtTokenMock(empId);

    mockMvc.perform(put("/admin/order/complete/{id}", orderId)
            .header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(orderService).complete(orderId);
  }
}

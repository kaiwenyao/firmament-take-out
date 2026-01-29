package dev.kaiwen.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.OrdersPaymentDto;
import dev.kaiwen.dto.OrdersSubmitDto;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.OrderSubmitVo;
import dev.kaiwen.vo.OrderVo;
import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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

  private void setupUserJwtMock() {
    given(jwtProperties.getUserTokenName()).willReturn("token");
    given(jwtProperties.getUserSecretKey()).willReturn("mock-secret-key");
    Claims claims = org.mockito.Mockito.mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"),
        org.mockito.ArgumentMatchers.anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.USER_ID)).willReturn("1");
  }

  @Test
  void reminderByNumberSuccess() throws Exception {
    setupUserJwtMock();

    String orderNumber = "202401290001";

    mockMvc.perform(get("/user/order/reminder/number/{orderNumber}", orderNumber).header("token",
            "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(orderService).reminderByNumber(orderNumber);
  }

  @Test
  void submitSuccess() throws Exception {
    setupUserJwtMock();

    OrdersSubmitDto dto = new OrdersSubmitDto();
    dto.setAddressBookId(1L);
    dto.setPayMethod(1);
    dto.setAmount(new BigDecimal("88.00"));
    OrderSubmitVo vo = OrderSubmitVo.builder()
        .id(1L)
        .orderNumber("202401290001")
        .orderAmount(new BigDecimal("88.00"))
        .orderTime(LocalDateTime.now())
        .build();
    given(orderService.submitOrder(any(OrdersSubmitDto.class))).willReturn(vo);

    mockMvc.perform(post("/user/order/submit")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.orderNumber").value("202401290001"));

    verify(orderService).submitOrder(any(OrdersSubmitDto.class));
  }

  @Test
  void paymentSuccess() throws Exception {
    setupUserJwtMock();

    OrdersPaymentDto dto = new OrdersPaymentDto();
    dto.setOrderNumber("202401290001");
    dto.setPayMethod(1);

    mockMvc.perform(put("/user/order/payment")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").value("支付成功"));

    verify(orderService).payment(any(OrdersPaymentDto.class));
  }

  @Test
  void pageSuccess() throws Exception {
    setupUserJwtMock();

    PageResult pageResult = new PageResult(1L, Collections.emptyList());
    given(orderService.pageQuery4User(1, 10, null)).willReturn(pageResult);

    mockMvc.perform(get("/user/order/historyOrders")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(1));

    verify(orderService).pageQuery4User(1, 10, null);
  }

  @Test
  void detailsByNumberSuccess() throws Exception {
    setupUserJwtMock();

    String orderNumber = "202401290001";
    OrderVo orderVo = new OrderVo();
    orderVo.setNumber(orderNumber);
    orderVo.setAmount(new BigDecimal("88.00"));
    given(orderService.detailsByNumber(orderNumber)).willReturn(orderVo);

    mockMvc.perform(get("/user/order/orderDetail/number/{orderNumber}", orderNumber).header("token",
            "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.number").value(orderNumber));

    verify(orderService).detailsByNumber(orderNumber);
  }

  @Test
  void cancelByNumberSuccess() throws Exception {
    setupUserJwtMock();

    String orderNumber = "202401290001";

    mockMvc.perform(put("/user/order/cancel/number/{orderNumber}", orderNumber).header("token",
            "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(orderService).userCancelByNumber(orderNumber);
  }

  @Test
  void repetitionByNumberSuccess() throws Exception {
    setupUserJwtMock();

    String orderNumber = "202401290001";

    mockMvc.perform(post("/user/order/repetition/number/{orderNumber}", orderNumber).header("token",
            "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(orderService).repetitionByNumber(orderNumber);
  }
}

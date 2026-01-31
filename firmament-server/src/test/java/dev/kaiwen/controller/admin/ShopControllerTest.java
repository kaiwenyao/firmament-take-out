package dev.kaiwen.controller.admin;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.handler.GlobalExceptionHandler;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.ShopService;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShopController.class)
class ShopControllerTest {

  @MockitoBean
  private ShopService shopService;

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
  void setStatusOpen() throws Exception {
    setupJwtTokenMock();

    Integer status = 1;

    mockMvc.perform(put("/admin/shop/{status}", status).header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(shopService).setStatus(status);
  }

  @Test
  void setStatusClosed() throws Exception {
    setupJwtTokenMock();

    Integer status = 0;

    mockMvc.perform(put("/admin/shop/{status}", status).header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(shopService).setStatus(status);
  }

  @Test
  void getStatusSuccess() throws Exception {
    setupJwtTokenMock();

    given(shopService.getStatus()).willReturn(1);

    mockMvc.perform(get("/admin/shop/status").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").value(1));

    verify(shopService).getStatus();
  }

  @Test
  void getStatusWhenClosed() throws Exception {
    setupJwtTokenMock();

    given(shopService.getStatus()).willReturn(0);

    mockMvc.perform(get("/admin/shop/status").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").value(0));

    verify(shopService).getStatus();
  }

  @Test
  void setStatusWhenServiceThrows() throws Exception {
    setupJwtTokenMock();

    doThrow(new RuntimeException("设置失败")).when(shopService).setStatus(1);

    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(put("/admin/shop/1").header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0));
    } finally {
      logger.setLevel(originalLevel);
    }

    verify(shopService).setStatus(1);
  }

  @Test
  void getStatusWhenServiceThrows() throws Exception {
    setupJwtTokenMock();

    given(shopService.getStatus()).willThrow(new RuntimeException("查询失败"));

    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(get("/admin/shop/status").header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0));
    } finally {
      logger.setLevel(originalLevel);
    }
  }
}

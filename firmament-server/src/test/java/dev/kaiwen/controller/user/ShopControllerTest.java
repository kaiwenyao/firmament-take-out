package dev.kaiwen.controller.user;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.ShopService;
import dev.kaiwen.utils.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShopController.class)
class ShopControllerTest {

  @MockitoBean
  private ShopService shopService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void getStatusWhenOpen() throws Exception {
    given(shopService.getStatus()).willReturn(1);

    mockMvc.perform(get("/user/shop/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").value(1));

    verify(shopService).getStatus();
  }

  @Test
  void getStatusWhenClosed() throws Exception {
    given(shopService.getStatus()).willReturn(0);

    mockMvc.perform(get("/user/shop/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").value(0));

    verify(shopService).getStatus();
  }
}

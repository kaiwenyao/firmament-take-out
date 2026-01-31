package dev.kaiwen.controller.user;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.ShoppingCartDto;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.ShoppingCartService;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShoppingCartController.class)
class ShoppingCartControllerTest {

  @MockitoBean
  private ShoppingCartService shoppingCartService;

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
    given(jwtService.parseJwt(org.mockito.ArgumentMatchers.eq("mock-secret-key"),
        org.mockito.ArgumentMatchers.anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.USER_ID)).willReturn("1");
  }

  @Test
  void addSuccess() throws Exception {
    setupUserJwtMock();

    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(1L);
    dto.setDishFlavor("不要辣");

    mockMvc.perform(post("/user/shoppingCart/add")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(shoppingCartService).addShoppingCart(any(ShoppingCartDto.class));
  }

  @Test
  void subSuccess() throws Exception {
    setupUserJwtMock();

    ShoppingCartDto dto = new ShoppingCartDto();
    dto.setDishId(1L);

    mockMvc.perform(post("/user/shoppingCart/sub")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(shoppingCartService).subShoppingCart(any(ShoppingCartDto.class));
  }

  @Test
  void listSuccess() throws Exception {
    setupUserJwtMock();

    List<ShoppingCart> list = Collections.singletonList(
        ShoppingCart.builder().id(1L).dishId(1L).number(2).build());
    given(shoppingCartService.showShoppingCart()).willReturn(list);

    mockMvc.perform(get("/user/shoppingCart/list").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(1));

    verify(shoppingCartService).showShoppingCart();
  }

  @Test
  void cleanShoppingCartSuccess() throws Exception {
    setupUserJwtMock();

    mockMvc.perform(delete("/user/shoppingCart/clean").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(shoppingCartService).cleanShoppingCart();
  }
}

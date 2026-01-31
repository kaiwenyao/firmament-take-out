package dev.kaiwen.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.SetmealService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.DishItemVo;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SetmealController.class)
class SetmealControllerTest {

  @MockitoBean
  private SetmealService setmealService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void listSuccess() throws Exception {
    Long categoryId = 10L;
    List<Setmeal> list = Collections.singletonList(
        Setmeal.builder().id(1L).name("商务套餐").categoryId(categoryId).price(new BigDecimal("38"))
            .build());
    given(setmealService.list(any(Setmeal.class))).willReturn(list);

    mockMvc.perform(get("/user/setmeal/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].name").value("商务套餐"));

    verify(setmealService).list(any(Setmeal.class));
  }

  @Test
  void dishListSuccess() throws Exception {
    Long setmealId = 100L;
    List<DishItemVo> list = Collections.singletonList(
        DishItemVo.builder().name("鱼香肉丝").copies(1).build());
    given(setmealService.getDishItemById(setmealId)).willReturn(list);

    mockMvc.perform(get("/user/setmeal/dish/{id}", setmealId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].name").value("鱼香肉丝"));

    verify(setmealService).getDishItemById(setmealId);
  }
}

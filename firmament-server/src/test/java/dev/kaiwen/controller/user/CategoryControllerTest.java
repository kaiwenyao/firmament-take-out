package dev.kaiwen.controller.user;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.kaiwen.entity.Category;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.CategoryService;
import dev.kaiwen.utils.JwtService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CategoryController.class)
class CategoryControllerTest {

  @MockitoBean
  private CategoryService categoryService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void listSuccess() throws Exception {
    List<Category> list = Collections.singletonList(
        Category.builder().id(1L).name("热销").type(1).sort(1).build());
    given(categoryService.list(1)).willReturn(list);

    mockMvc.perform(get("/user/category/list").param("type", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].name").value("热销"));

    verify(categoryService).list(1);
  }

  @Test
  void listWhenEmpty() throws Exception {
    given(categoryService.list(2)).willReturn(Collections.emptyList());

    mockMvc.perform(get("/user/category/list").param("type", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray());

    verify(categoryService).list(2);
  }
}

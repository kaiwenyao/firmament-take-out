package dev.kaiwen.controller.admin;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.CategoryDto;
import dev.kaiwen.dto.CategoryPageQueryDto;
import dev.kaiwen.entity.Category;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.CategoryService;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

  @MockitoBean
  private CategoryService categoryService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private ObjectMapper objectMapper;

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
  void saveSuccess() throws Exception {
    CategoryDto categoryDto = new CategoryDto();
    categoryDto.setType(1);
    categoryDto.setName("测试分类");
    categoryDto.setSort(1);

    setupJwtTokenMock();

    mockMvc.perform(post("/admin/category")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(categoryDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(categoryService).save(any(CategoryDto.class));
  }

  @Test
  void pageSuccess() throws Exception {
    CategoryPageQueryDto queryDto = new CategoryPageQueryDto();
    queryDto.setPage(1);
    queryDto.setPageSize(10);
    queryDto.setName("测试");
    queryDto.setType(1);

    setupJwtTokenMock();

    List<Category> categories = new ArrayList<>();
    categories.add(Category.builder().id(1L).name("测试分类").type(1).sort(1).status(1).build());
    PageResult pageResult = new PageResult(1L, categories);
    given(categoryService.pageQuery(any(CategoryPageQueryDto.class))).willReturn(pageResult);

    mockMvc.perform(get("/admin/category/page")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10")
            .param("name", "测试")
            .param("type", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records[0].id").value(1))
        .andExpect(jsonPath("$.data.records[0].name").value("测试分类"))
        .andExpect(jsonPath("$.data.records[0].type").value(1));

    verify(categoryService).pageQuery(any(CategoryPageQueryDto.class));
  }

  @Test
  void pageWithEmptyResult() throws Exception {
    setupJwtTokenMock();

    PageResult pageResult = new PageResult(0L, new ArrayList<>());
    given(categoryService.pageQuery(any(CategoryPageQueryDto.class))).willReturn(pageResult);

    mockMvc.perform(get("/admin/category/page")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(0))
        .andExpect(jsonPath("$.data.records").isEmpty());

    verify(categoryService).pageQuery(any(CategoryPageQueryDto.class));
  }

  @Test
  void deleteByIdSuccess() throws Exception {
    Long categoryId = 100L;

    setupJwtTokenMock();

    mockMvc.perform(delete("/admin/category")
            .header("token", "mock-accessToken")
            .param("id", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(categoryService).deleteById(categoryId);
  }

  @Test
  void updateSuccess() throws Exception {
    CategoryDto categoryDto = new CategoryDto();
    categoryDto.setId(100L);
    categoryDto.setType(1);
    categoryDto.setName("更新后的分类");
    categoryDto.setSort(2);

    setupJwtTokenMock();

    mockMvc.perform(put("/admin/category")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(categoryDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(categoryService).update(any(CategoryDto.class));
  }

  @Test
  void enableOrDisableSuccess() throws Exception {
    Integer status = 1;
    Long categoryId = 100L;

    setupJwtTokenMock();

    mockMvc.perform(post("/admin/category/status/{status}", status)
            .header("token", "mock-accessToken")
            .param("id", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(categoryService).enableOrDisable(status, categoryId);
  }

  @Test
  void enableOrDisableDisable() throws Exception {
    Integer status = 0;
    Long categoryId = 100L;

    setupJwtTokenMock();

    mockMvc.perform(post("/admin/category/status/{status}", status)
            .header("token", "mock-accessToken")
            .param("id", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(categoryService).enableOrDisable(status, categoryId);
  }

  @Test
  void listSuccess() throws Exception {
    Integer type = 1;

    setupJwtTokenMock();

    List<Category> categories = new ArrayList<>();
    categories.add(Category.builder().id(1L).name("菜品分类1").type(1).sort(1).status(1).build());
    categories.add(Category.builder().id(2L).name("菜品分类2").type(1).sort(2).status(1).build());
    given(categoryService.list(type)).willReturn(categories);

    mockMvc.perform(get("/admin/category/list")
            .header("token", "mock-accessToken")
            .param("type", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].name").value("菜品分类1"))
        .andExpect(jsonPath("$.data[1].id").value(2))
        .andExpect(jsonPath("$.data[1].name").value("菜品分类2"));

    verify(categoryService).list(type);
  }

  @Test
  void listWithEmptyResult() throws Exception {
    Integer type = 2;

    setupJwtTokenMock();

    given(categoryService.list(type)).willReturn(new ArrayList<>());

    mockMvc.perform(get("/admin/category/list")
            .header("token", "mock-accessToken")
            .param("type", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data").isEmpty());

    verify(categoryService).list(type);
  }
}

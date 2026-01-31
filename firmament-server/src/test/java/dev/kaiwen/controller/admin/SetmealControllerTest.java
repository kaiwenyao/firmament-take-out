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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.SetmealDto;
import dev.kaiwen.dto.SetmealPageQueryDto;
import dev.kaiwen.handler.GlobalExceptionHandler;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.SetmealService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.SetmealVo;
import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SetmealController.class)
class SetmealControllerTest {

  @MockitoBean
  private SetmealService setmealService;

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
    setupJwtTokenMock();

    SetmealDto setmealDto = new SetmealDto();
    setmealDto.setCategoryId(10L);
    setmealDto.setName("测试套餐");
    setmealDto.setPrice(new BigDecimal("38.00"));
    setmealDto.setStatus(1);
    setmealDto.setDescription("测试描述");
    setmealDto.setImage("test.jpg");

    mockMvc.perform(post("/admin/setmeal")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(setmealDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(setmealService).saveWithDish(any(SetmealDto.class));
  }

  @Test
  void pageSuccess() throws Exception {
    setupJwtTokenMock();

    List<SetmealVo> setmeals = new ArrayList<>();
    setmeals.add(
        SetmealVo.builder().id(1L).name("套餐1").categoryId(10L).price(new BigDecimal("38.00"))
            .status(1).build());
    PageResult pageResult = new PageResult(1L, setmeals);
    given(setmealService.pageQuery(any(SetmealPageQueryDto.class))).willReturn(pageResult);

    mockMvc.perform(get("/admin/setmeal/page")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records[0].id").value(1))
        .andExpect(jsonPath("$.data.records[0].name").value("套餐1"));

    verify(setmealService).pageQuery(any(SetmealPageQueryDto.class));
  }

  @Test
  void getByIdSuccess() throws Exception {
    setupJwtTokenMock();

    Long setmealId = 100L;
    SetmealVo setmealVo = SetmealVo.builder()
        .id(setmealId)
        .name("测试套餐")
        .categoryId(10L)
        .price(new BigDecimal("38.00"))
        .status(1)
        .categoryName("测试分类")
        .build();
    given(setmealService.getByIdWithDish(setmealId)).willReturn(setmealVo);

    mockMvc.perform(get("/admin/setmeal/{id}", setmealId).header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.id").value(setmealId))
        .andExpect(jsonPath("$.data.name").value("测试套餐"))
        .andExpect(jsonPath("$.data.categoryId").value(10))
        .andExpect(jsonPath("$.data.price").value(38.00));

    verify(setmealService).getByIdWithDish(setmealId);
  }

  @Test
  void updateSuccess() throws Exception {
    setupJwtTokenMock();

    SetmealDto setmealDto = new SetmealDto();
    setmealDto.setId(100L);
    setmealDto.setCategoryId(10L);
    setmealDto.setName("更新后的套餐");
    setmealDto.setPrice(new BigDecimal("42.00"));
    setmealDto.setStatus(1);

    mockMvc.perform(put("/admin/setmeal")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(setmealDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(setmealService).update(any(SetmealDto.class));
  }

  @Test
  void deleteSuccess() throws Exception {
    setupJwtTokenMock();

    mockMvc.perform(delete("/admin/setmeal")
            .header("token", "mock-accessToken")
            .param("ids", "1", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(setmealService).deleteBatch(List.of(1L, 2L));
  }

  @Test
  void startOrStopSuccess() throws Exception {
    setupJwtTokenMock();

    Integer status = 1;
    Long setmealId = 100L;

    mockMvc.perform(post("/admin/setmeal/status/{status}", status)
            .header("token", "mock-accessToken")
            .param("id", String.valueOf(setmealId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(setmealService).startOrStop(status, setmealId);
  }

  @Test
  void getByIdWhenNotFound() throws Exception {
    setupJwtTokenMock();

    Long setmealId = 999L;
    given(setmealService.getByIdWithDish(setmealId)).willThrow(new RuntimeException("套餐不存在"));

    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(get("/admin/setmeal/{id}", setmealId).header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0));
    } finally {
      logger.setLevel(originalLevel);
    }

    verify(setmealService).getByIdWithDish(setmealId);
  }

  @Test
  void pageWhenEmptyResult() throws Exception {
    setupJwtTokenMock();

    PageResult emptyResult = new PageResult(0L, new ArrayList<>());
    given(setmealService.pageQuery(any(SetmealPageQueryDto.class))).willReturn(emptyResult);

    mockMvc.perform(get("/admin/setmeal/page")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(0))
        .andExpect(jsonPath("$.data.records").isArray());

    verify(setmealService).pageQuery(any(SetmealPageQueryDto.class));
  }
}

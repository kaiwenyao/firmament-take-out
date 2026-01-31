package dev.kaiwen.controller.admin;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.CacheConstant;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.dto.DishDto;
import dev.kaiwen.dto.DishPageQueryDto;
import dev.kaiwen.entity.DishFlavor;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.DishService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.DishVo;
import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DishController.class)
class DishControllerTest {

  @MockitoBean
  private DishService dishService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @MockitoBean(name = "redisTemplateStringObject")
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  /**
   * 设置 JWT token Mock 的辅助方法.
   */
  private void setupJwtTokenMock(Long empId) {
    given(jwtProperties.getAdminTokenName()).willReturn("token");
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(empId.toString());
  }

  /**
   * Mock Redis keys 和 delete 操作的辅助方法.
   */
  private void setupRedisMock(String pattern, Set<String> keys) {
    given(redisTemplate.keys(pattern)).willReturn(keys);
    if (keys != null && !keys.isEmpty()) {
      // RedisTemplate 的 delete 方法接受 Collection<String> 参数
      given(redisTemplate.delete(anyCollection())).willReturn((long) keys.size());
    }
  }

  @Test
  void createDishSuccess() throws Exception {
    // 准备测试数据
    DishDto dishDto = new DishDto();
    dishDto.setName("测试菜品");
    Long categoryId = 10L;
    dishDto.setCategoryId(categoryId);
    dishDto.setPrice(new BigDecimal("28.00"));
    dishDto.setImage("test.jpg");
    dishDto.setDescription("测试菜品描述");
    dishDto.setStatus(1);

    List<DishFlavor> flavors = new ArrayList<>();
    DishFlavor flavor1 = new DishFlavor();
    flavor1.setName("甜度");
    flavor1.setValue("正常糖");
    flavors.add(flavor1);
    dishDto.setFlavors(flavors);

    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Redis keys
    String cacheKey = CacheConstant.DISH_KEY_PREFIX + categoryId;
    Set<String> cacheKeys = new HashSet<>();
    cacheKeys.add(cacheKey + "_1");
    cacheKeys.add(cacheKey + "_2");
    setupRedisMock(cacheKey, cacheKeys);

    // 执行请求
    mockMvc.perform(post("/admin/dish")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dishDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    // 验证方法调用
    verify(dishService).saveWithFlavor(any(DishDto.class));
    verify(redisTemplate).keys(cacheKey);
    verify(redisTemplate).delete(cacheKeys);
  }

  @Test
  void createDishWithoutFlavors() throws Exception {
    // 准备测试数据
    DishDto dishDto = new DishDto();
    dishDto.setName("测试菜品（无口味）");
    Long categoryId = 10L;
    dishDto.setCategoryId(categoryId);
    dishDto.setPrice(new BigDecimal("25.00"));
    dishDto.setStatus(1);

    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Redis keys（空集合）
    String cacheKey = CacheConstant.DISH_KEY_PREFIX + categoryId;
    setupRedisMock(cacheKey, new HashSet<>());

    // 执行请求
    mockMvc.perform(post("/admin/dish")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dishDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    // 验证方法调用
    verify(dishService).saveWithFlavor(any(DishDto.class));
    verify(redisTemplate).keys(cacheKey);
    // 由于 keys 返回空集合，delete 不会被调用
    verify(redisTemplate, never()).delete(anyCollection());
  }

  @Test
  void pageQuerySuccess() throws Exception {
    // 准备测试数据
    DishPageQueryDto queryDto = new DishPageQueryDto();
    queryDto.setPage(1);
    queryDto.setPageSize(10);
    queryDto.setName("测试");
    queryDto.setCategoryId(10);
    queryDto.setStatus(1);

    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回分页结果
    List<DishVo> dishes = new ArrayList<>();
    DishVo dish1 = DishVo.builder()
        .id(1L)
        .name("测试菜品1")
        .categoryId(10L)
        .price(new BigDecimal("28.00"))
        .status(1)
        .build();
    dishes.add(dish1);

    PageResult pageResult = new PageResult(1L, dishes);
    given(dishService.pageQuery(any(DishPageQueryDto.class))).willReturn(pageResult);

    // 执行请求
    mockMvc.perform(get("/admin/dish/page")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10")
            .param("name", "测试")
            .param("categoryId", "10")
            .param("status", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records[0].id").value(1))
        .andExpect(jsonPath("$.data.records[0].name").value("测试菜品1"))
        .andExpect(jsonPath("$.data.records[0].categoryId").value(10))
        .andExpect(jsonPath("$.data.records[0].price").value(28.00));

    // 验证方法调用
    verify(dishService).pageQuery(any(DishPageQueryDto.class));
  }

  @Test
  void pageQueryWithEmptyResult() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回空分页结果
    PageResult pageResult = new PageResult(0L, new ArrayList<>());
    given(dishService.pageQuery(any(DishPageQueryDto.class))).willReturn(pageResult);

    // 执行请求
    mockMvc.perform(get("/admin/dish/page")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(0))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records").isEmpty());

    // 验证方法调用
    verify(dishService).pageQuery(any(DishPageQueryDto.class));
  }

  @Test
  void deleteDishSuccess() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回菜品信息
    Long categoryId1 = 10L;
    DishVo dish1 = DishVo.builder()
        .id(1L)
        .categoryId(categoryId1)
        .name("菜品1")
        .build();
    Long categoryId2 = 20L;
    DishVo dish2 = DishVo.builder()
        .id(2L)
        .categoryId(categoryId2)
        .name("菜品2")
        .build();

    given(dishService.getDishById(1L)).willReturn(dish1);
    given(dishService.getDishById(2L)).willReturn(dish2);

    // Mock Redis keys
    String cacheKey1 = CacheConstant.DISH_KEY_PREFIX + categoryId1;
    String cacheKey2 = CacheConstant.DISH_KEY_PREFIX + categoryId2;
    Set<String> cacheKeys1 = new HashSet<>();
    cacheKeys1.add(cacheKey1 + "_1");
    Set<String> cacheKeys2 = new HashSet<>();
    cacheKeys2.add(cacheKey2 + "_1");
    setupRedisMock(cacheKey1, cacheKeys1);
    setupRedisMock(cacheKey2, cacheKeys2);

    // 执行请求
    mockMvc.perform(delete("/admin/dish")
            .header("token", "mock-accessToken")
            .param("ids", "1", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    // 验证方法调用
    verify(dishService).getDishById(1L);
    verify(dishService).getDishById(2L);
    verify(dishService).deleteDish(List.of(1L, 2L));
    verify(redisTemplate).keys(cacheKey1);
    verify(redisTemplate).keys(cacheKey2);
    verify(redisTemplate).delete(cacheKeys1);
    verify(redisTemplate).delete(cacheKeys2);
  }

  @Test
  void deleteDishWithNullDish() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回 null（菜品不存在）
    given(dishService.getDishById(999L)).willReturn(null);

    // 执行请求
    mockMvc.perform(delete("/admin/dish")
            .header("token", "mock-accessToken")
            .param("ids", "999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    // 验证方法调用
    verify(dishService).getDishById(999L);
    verify(dishService).deleteDish(List.of(999L));
    // 由于菜品不存在，不会清理缓存
    verify(redisTemplate, never()).keys(anyString());
  }

  @Test
  void getDishByIdSuccess() throws Exception {
    // 准备测试数据
    List<DishFlavor> flavors = new ArrayList<>();
    DishFlavor flavor = new DishFlavor();
    flavor.setName("甜度");
    flavor.setValue("正常糖");
    flavors.add(flavor);

    Long dishId = 100L;
    DishVo dishVo = DishVo.builder()
        .id(dishId)
        .name("测试菜品")
        .categoryId(10L)
        .price(new BigDecimal("28.00"))
        .image("test.jpg")
        .description("测试描述")
        .status(1)
        .categoryName("测试分类")
        .flavors(flavors)
        .build();
    given(dishService.getDishById(dishId)).willReturn(dishVo);

    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // 执行请求
    mockMvc.perform(get("/admin/dish/{id}", dishId)
            .header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.id").value(dishId))
        .andExpect(jsonPath("$.data.name").value("测试菜品"))
        .andExpect(jsonPath("$.data.categoryId").value(10))
        .andExpect(jsonPath("$.data.price").value(28.00))
        .andExpect(jsonPath("$.data.status").value(1))
        .andExpect(jsonPath("$.data.categoryName").value("测试分类"))
        .andExpect(jsonPath("$.data.flavors").isArray())
        .andExpect(jsonPath("$.data.flavors[0].name").value("甜度"));

    // 验证方法调用
    verify(dishService).getDishById(dishId);
  }

  @Test
  void updateDishSuccess() throws Exception {
    // 准备测试数据
    DishDto dishDto = new DishDto();
    Long dishId = 100L;
    dishDto.setId(dishId);
    dishDto.setName("更新后的菜品");
    Long categoryId = 10L;
    dishDto.setCategoryId(categoryId);
    dishDto.setPrice(new BigDecimal("30.00"));
    dishDto.setStatus(1);

    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回旧的菜品信息（分类未变更）
    DishVo oldDish = DishVo.builder()
        .id(dishId)
        .categoryId(categoryId)
        .build();
    given(dishService.getDishById(dishId)).willReturn(oldDish);

    // Mock Redis keys
    String cacheKey = CacheConstant.DISH_KEY_PREFIX + categoryId;
    Set<String> cacheKeys = new HashSet<>();
    cacheKeys.add(cacheKey + "_1");
    setupRedisMock(cacheKey, cacheKeys);

    // 执行请求
    mockMvc.perform(put("/admin/dish")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dishDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    // 验证方法调用
    verify(dishService).getDishById(dishId);
    verify(dishService).updateDish(any(DishDto.class));
    verify(redisTemplate).keys(cacheKey);
    verify(redisTemplate).delete(cacheKeys);
  }

  @Test
  void updateDishWithCategoryChanged() throws Exception {
    // 准备测试数据
    DishDto dishDto = new DishDto();
    Long dishId = 100L;
    dishDto.setId(dishId);
    dishDto.setName("更新后的菜品");
    Long newCategoryId = 20L;
    dishDto.setCategoryId(newCategoryId);
    dishDto.setPrice(new BigDecimal("30.00"));

    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回旧的菜品信息（分类已变更）
    Long oldCategoryId = 10L;
    DishVo oldDish = DishVo.builder()
        .id(dishId)
        .categoryId(oldCategoryId)
        .build();
    given(dishService.getDishById(dishId)).willReturn(oldDish);

    // Mock Redis keys（两个分类的缓存都要清理）
    String oldCacheKey = CacheConstant.DISH_KEY_PREFIX + oldCategoryId;
    String newCacheKey = CacheConstant.DISH_KEY_PREFIX + newCategoryId;
    Set<String> oldCacheKeys = new HashSet<>();
    oldCacheKeys.add(oldCacheKey + "_1");
    Set<String> newCacheKeys = new HashSet<>();
    newCacheKeys.add(newCacheKey + "_1");
    setupRedisMock(oldCacheKey, oldCacheKeys);
    setupRedisMock(newCacheKey, newCacheKeys);

    // 执行请求
    mockMvc.perform(put("/admin/dish")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dishDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    // 验证方法调用
    verify(dishService).getDishById(dishId);
    verify(dishService).updateDish(any(DishDto.class));
    // 验证两个分类的缓存都被清理
    verify(redisTemplate).keys(oldCacheKey);
    verify(redisTemplate).keys(newCacheKey);
    verify(redisTemplate).delete(oldCacheKeys);
    verify(redisTemplate).delete(newCacheKeys);
  }

  @Test
  void updateDishWhenOldDishIsNull() throws Exception {
    // 准备测试数据 - 覆盖 oldDish == null 的情况
    DishDto dishDto = new DishDto();
    Long dishId = 999L; // 不存在的菜品ID
    dishDto.setId(dishId);
    dishDto.setName("更新后的菜品");
    Long categoryId = 10L;
    dishDto.setCategoryId(categoryId);
    dishDto.setPrice(new BigDecimal("30.00"));

    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回 null（旧菜品不存在）
    given(dishService.getDishById(dishId)).willReturn(null);

    // Mock Redis keys（只清理新分类的缓存，因为 oldDish 为 null）
    String cacheKey = CacheConstant.DISH_KEY_PREFIX + categoryId;
    Set<String> cacheKeys = new HashSet<>();
    cacheKeys.add(cacheKey + "_1");
    setupRedisMock(cacheKey, cacheKeys);

    // 执行请求
    mockMvc.perform(put("/admin/dish")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dishDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    // 验证方法调用
    verify(dishService).getDishById(dishId);
    verify(dishService).updateDish(any(DishDto.class));
    // 验证只清理了新分类的缓存（因为 oldDish 为 null，不会进入 if 分支）
    verify(redisTemplate).keys(cacheKey);
    verify(redisTemplate).delete(cacheKeys);
    // 验证不会清理旧分类的缓存（因为 oldDish 为 null）
    verify(redisTemplate, never()).keys(CacheConstant.DISH_KEY_PREFIX + "oldCategoryId");
  }

  @Test
  void startOrStopSuccess() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回菜品信息
    Long dishId = 100L;
    Long categoryId = 10L;
    DishVo dish = DishVo.builder()
        .id(dishId)
        .categoryId(categoryId)
        .name("测试菜品")
        .build();
    given(dishService.getDishById(dishId)).willReturn(dish);

    // Mock Redis keys
    String cacheKey = CacheConstant.DISH_KEY_PREFIX + categoryId;
    Set<String> cacheKeys = new HashSet<>();
    cacheKeys.add(cacheKey + "_1");
    setupRedisMock(cacheKey, cacheKeys);

    // 执行请求
    Integer status = 1; // 起售
    mockMvc.perform(post("/admin/dish/status/{status}", status)
            .header("token", "mock-accessToken")
            .param("id", String.valueOf(dishId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    // 验证方法调用
    verify(dishService).getDishById(dishId);
    verify(dishService).startOrStop(status, dishId);
    verify(redisTemplate).keys(cacheKey);
    verify(redisTemplate).delete(cacheKeys);
  }

  @Test
  void startOrStopWithNullDish() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    Long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回 null（菜品不存在）
    Long dishId = 999L;
    given(dishService.getDishById(dishId)).willReturn(null);

    // 执行请求
    Integer status = 0; // 停售
    mockMvc.perform(post("/admin/dish/status/{status}", status)
            .header("token", "mock-accessToken")
            .param("id", String.valueOf(dishId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    // 验证方法调用
    verify(dishService).getDishById(dishId);
    verify(dishService).startOrStop(status, dishId);
    // 由于菜品不存在，不会清理缓存
    verify(redisTemplate, never()).keys(anyString());
  }
}

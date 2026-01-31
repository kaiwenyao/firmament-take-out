package dev.kaiwen.controller.user;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.entity.Dish;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.DishService;
import dev.kaiwen.utils.JwtService;
import dev.kaiwen.vo.DishVo;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DishController.class)
class DishControllerTest {

  @MockitoBean
  private DishService dishService;

  @MockitoBean
  private RedisTemplate<String, String> redisTemplateStringString;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  private ValueOperations<String, String> valueOperations;

  @MockitoSpyBean
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> operations = mock(ValueOperations.class);
    valueOperations = operations;
    given(redisTemplateStringString.opsForValue()).willReturn(valueOperations);
  }

  @Test
  void listWhenCacheMiss() throws Exception {
    Long categoryId = 10L;
    List<DishVo> list = Collections.singletonList(
        DishVo.builder().id(1L).name("鱼香肉丝").price(new BigDecimal("28.00")).build());
    given(valueOperations.get("dish_" + categoryId)).willReturn(null);
    given(dishService.listWithFlavor(any(Dish.class))).willReturn(list);

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].name").value("鱼香肉丝"));

    verify(dishService).listWithFlavor(any(Dish.class));
  }

  @Test
  void listWhenCacheHit() throws Exception {
    Long categoryId = 10L;
    String cacheJson = "[{\"id\":1,\"name\":\"宫保鸡丁\",\"price\":32.00}]";
    given(valueOperations.get("dish_" + categoryId)).willReturn(cacheJson);

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].name").value("宫保鸡丁"));

    verify(valueOperations).get("dish_" + categoryId);
  }

  /**
   * 覆盖：cacheJson 为空白时跳过缓存，走 DB.
   */
  @Test
  void listWhenCacheBlank() throws Exception {
    Long categoryId = 10L;
    given(valueOperations.get("dish_" + categoryId)).willReturn("   ");
    List<DishVo> list = Collections.singletonList(
        DishVo.builder().id(2L).name("青椒肉丝").price(new BigDecimal("26.00")).build());
    given(dishService.listWithFlavor(any(Dish.class))).willReturn(list);

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data[0].name").value("青椒肉丝"));

    verify(dishService).listWithFlavor(any(Dish.class));
  }

  /**
   * 覆盖：缓存命中但解析结果为空列表，不提前 return，继续走 DB.
   */
  @Test
  void listWhenCacheParsesToEmptyList() throws Exception {
    Long categoryId = 10L;
    given(valueOperations.get("dish_" + categoryId)).willReturn("[]");
    List<DishVo> dbList = Collections.singletonList(
        DishVo.builder().id(3L).name("回锅肉").price(new BigDecimal("30.00")).build());
    given(dishService.listWithFlavor(any(Dish.class))).willReturn(dbList);

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data[0].name").value("回锅肉"));

    verify(dishService).listWithFlavor(any(Dish.class));
  }

  /**
   * 覆盖：缓存内容非法 JSON，parseDishCache 抛异常，catch 中 delete(key).
   */
  @Test
  void listWhenCacheInvalidJsonCatchBlock() throws Exception {
    Long categoryId = 10L;
    String key = "dish_" + categoryId;
    given(valueOperations.get(key)).willReturn("not-valid-json");
    List<DishVo> list = Collections.singletonList(
        DishVo.builder().id(1L).name("鱼香肉丝").price(new BigDecimal("28.00")).build());
    given(dishService.listWithFlavor(any(Dish.class))).willReturn(list);

    Logger logger = (Logger) LoggerFactory.getLogger(DishController.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(1))
          .andExpect(jsonPath("$.data[0].name").value("鱼香肉丝"));
    } finally {
      logger.setLevel(originalLevel);
    }

    verify(dishService).listWithFlavor(any(Dish.class));
    verify(redisTemplateStringString).delete(key);
  }

  /**
   * 覆盖：parseDishCache 中 raw 非 List，返回 Collections.emptyList()（缓存为 "{}"）.
   */
  @Test
  void listWhenCacheIsNotListParseReturnsEmpty() throws Exception {
    Long categoryId = 10L;
    given(valueOperations.get("dish_" + categoryId)).willReturn("{}");
    List<DishVo> dbList = Collections.singletonList(
        DishVo.builder().id(4L).name("蒜泥白肉").price(new BigDecimal("32.00")).build());
    given(dishService.listWithFlavor(any(Dish.class))).willReturn(dbList);

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data[0].name").value("蒜泥白肉"));

    verify(dishService).listWithFlavor(any(Dish.class));
  }

  /**
   * 覆盖：DB 查询后写入 Redis 时 objectMapper.writeValueAsString 抛 JsonProcessingException，catch 中 log.warn.
   * 与跳过缓存.
   */
  @Test
  void listWhenCacheSerializationFails() throws Exception {
    Long categoryId = 10L;
    given(valueOperations.get("dish_" + categoryId)).willReturn(null);
    List<DishVo> list = Collections.singletonList(
        DishVo.builder().id(1L).name("鱼香肉丝").price(new BigDecimal("28.00")).build());
    given(dishService.listWithFlavor(any(Dish.class))).willReturn(list);
    doThrow(new JsonProcessingException("mock") {
    })
        .when(objectMapper).writeValueAsString(any());

    Logger logger = (Logger) LoggerFactory.getLogger(DishController.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(1))
          .andExpect(jsonPath("$.data[0].name").value("鱼香肉丝"));
    } finally {
      logger.setLevel(originalLevel);
    }

    verify(dishService).listWithFlavor(any(Dish.class));
  }

  /**
   * 覆盖：parseDishCache 中 wrapper 格式 ["java.util.ArrayList", [...]]，payload = rawList.get(1).
   */
  @Test
  void listWhenCacheWrapperFormat() throws Exception {
    Long categoryId = 10L;
    String cacheJson = "[\"java.util.ArrayList\",[{\"id\":5,\"name\":\"水煮鱼\",\"price\":48.00}]]";
    given(valueOperations.get("dish_" + categoryId)).willReturn(cacheJson);

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(5))
        .andExpect(jsonPath("$.data[0].name").value("水煮鱼"))
        .andExpect(jsonPath("$.data[0].price").value(48.0));

    verify(valueOperations).get("dish_" + categoryId);
  }

  /**
   * 覆盖：size==2 且 rawList.get(0) 非 String，条件不成立，payload 为 rawList.
   */
  @Test
  void listWhenCacheFirstElementNotString() throws Exception {
    Long categoryId = 10L;
    String cacheJson = "[1,[{\"id\":7,\"name\":\"麻婆豆腐\",\"price\":18.00}]]";
    List<DishVo> cachedList = Collections.singletonList(
        DishVo.builder().id(7L).name("麻婆豆腐").price(new BigDecimal("18.00")).build());
    given(valueOperations.get("dish_" + categoryId)).willReturn(cacheJson);
    doReturn(cachedList).when(objectMapper)
        .convertValue(any(), ArgumentMatchers.<TypeReference<List<DishVo>>>any());

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data[0].name").value("麻婆豆腐"));

    verify(dishService, never()).listWithFlavor(any(Dish.class));
  }

  /**
   * 覆盖：size==2 且 rawList.get(1) 非 List，条件不成立，payload 为 rawList.
   */
  @Test
  void listWhenCacheSecondElementNotList() throws Exception {
    Long categoryId = 10L;
    String cacheJson = "[\"java.util.ArrayList\",\"not-a-list\"]";
    List<DishVo> cachedList = Collections.singletonList(
        DishVo.builder().id(8L).name("鱼香茄子").price(new BigDecimal("22.00")).build());
    given(valueOperations.get("dish_" + categoryId)).willReturn(cacheJson);
    doReturn(cachedList).when(objectMapper)
        .convertValue(any(), ArgumentMatchers.<TypeReference<List<DishVo>>>any());

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data[0].name").value("鱼香茄子"));

    verify(dishService, never()).listWithFlavor(any(Dish.class));
  }

  /**
   * 覆盖：缓存解析结果为 null，不提前 return，继续走 DB.
   */
  @Test
  void listWhenCacheParsesToNullList() throws Exception {
    Long categoryId = 10L;
    String cacheJson = "[{\"id\":1,\"name\":\"宫保鸡丁\",\"price\":32.00}]";
    given(valueOperations.get("dish_" + categoryId)).willReturn(cacheJson);
    doReturn(null).when(objectMapper)
        .convertValue(any(), ArgumentMatchers.<TypeReference<List<DishVo>>>any());
    List<DishVo> dbList = Collections.singletonList(
        DishVo.builder().id(6L).name("干煸四季豆").price(new BigDecimal("26.00")).build());
    given(dishService.listWithFlavor(any(Dish.class))).willReturn(dbList);

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data[0].name").value("干煸四季豆"));

    verify(dishService).listWithFlavor(any(Dish.class));
  }

  /**
   * 覆盖：DB 返回 null，跳过缓存写入.
   */
  @Test
  void listWhenDbReturnsNull() throws Exception {
    Long categoryId = 10L;
    given(valueOperations.get("dish_" + categoryId)).willReturn(null);
    given(dishService.listWithFlavor(any(Dish.class))).willReturn(null);

    mockMvc.perform(get("/user/dish/list").param("categoryId", String.valueOf(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").value(nullValue()));

    verify(dishService).listWithFlavor(any(Dish.class));
    verify(valueOperations, never()).set(anyString(), anyString());
  }
}

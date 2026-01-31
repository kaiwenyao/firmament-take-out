package dev.kaiwen.controller.admin;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.CacheConstant;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.EmployeeDto;
import dev.kaiwen.dto.EmployeeLoginDto;
import dev.kaiwen.dto.EmployeePageQueryDto;
import dev.kaiwen.dto.PasswordEditDto;
import dev.kaiwen.dto.RefreshTokenDto;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.EmployeeService;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

  @MockitoBean
  private EmployeeService employeeService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties; // 配置类也可以 Mock

  @MockitoBean(name = "redisTemplateStringString")
  private RedisTemplate<String, String> redisTemplate; // 模拟 Redis
  @Autowired
  private ObjectMapper objectMapper;

  // ✅ 改动 1：在这里定义 Mock 对象，Mockito 会自动处理泛型
  @Mock
  private ValueOperations<String, String> valueOperationsMock;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void login() throws Exception {
    EmployeeLoginDto employeeLoginDto = new EmployeeLoginDto();
    employeeLoginDto.setUsername("admin");
    employeeLoginDto.setPassword("123456");
    Employee employee = new Employee();
    employee.setId(1L);
    employee.setUsername("admin");
    employee.setPassword("123456");
    given(employeeService.login(employeeLoginDto)).willReturn(employee);

    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    given(jwtProperties.getAdminTtl()).willReturn(7200000L);
    given(jwtProperties.getAdminRefreshTtl()).willReturn(604800000L);

    String mockAccessToken = "mock-accessToken";
    String mockRefreshToken = "mock-refreshToken";

    given(jwtService.createJwt(anyString(), eq(7200000L), anyMap()))
        .willReturn(mockAccessToken);

    given(jwtService.createJwt(anyString(), eq(604800000L), anyMap()))
        .willReturn(mockRefreshToken);
    given(redisTemplate.opsForValue()).willReturn(valueOperationsMock);

    mockMvc.perform(post("/admin/employee/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(employeeLoginDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.token").value(mockAccessToken))
        .andExpect(jsonPath("$.data.refreshToken").value(mockRefreshToken))
        .andExpect(jsonPath("$.data.userName").value("admin"));

    verify(employeeService).login(any(EmployeeLoginDto.class));
    String expectedRedisKey = CacheConstant.REFRESH_TOKEN_KEY_PREFIX + "1";
    verify(valueOperationsMock).set(
        expectedRedisKey,
        mockRefreshToken,
        604800000L,
        TimeUnit.MILLISECONDS
    );
  }

  @Test
  void logoutSuccessWhenTokenExists() throws Exception {
    // 准备测试数据
    given(jwtProperties.getAdminTokenName()).willReturn("token");
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);
    long empId = 1L;
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(empId);

    // Mock BaseContext.getCurrentId() 返回员工ID
    String redisKey = CacheConstant.REFRESH_TOKEN_KEY_PREFIX + empId;
    try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
      baseContextMock.when(BaseContext::getCurrentId).thenReturn(empId);

      // Mock Redis删除操作返回true（表示Token存在且删除成功）
      given(redisTemplate.delete(redisKey)).willReturn(Boolean.TRUE);

      // 执行请求
      mockMvc.perform(post("/admin/employee/logout")
              .header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(1))
          .andExpect(jsonPath("$.msg").value(nullValue()));

      // 验证Redis删除操作被调用
      verify(redisTemplate).delete(redisKey);
    }
  }

  @Test
  void logoutSuccessWhenTokenNotExists() throws Exception {
    // 准备测试数据
    given(jwtProperties.getAdminTokenName()).willReturn("token");
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);
    long empId = 2L;
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(empId);

    // Mock BaseContext.getCurrentId() 返回员工ID
    String redisKey = CacheConstant.REFRESH_TOKEN_KEY_PREFIX + empId;
    try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
      baseContextMock.when(BaseContext::getCurrentId).thenReturn(empId);

      // Mock Redis删除操作返回false（表示Token不存在）
      given(redisTemplate.delete(redisKey)).willReturn(Boolean.FALSE);

      // 执行请求
      mockMvc.perform(post("/admin/employee/logout")
              .header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(1))
          .andExpect(jsonPath("$.msg").value(nullValue()));

      // 验证Redis删除操作被调用（即使Token不存在也会尝试删除）
      verify(redisTemplate).delete(redisKey);
    }
  }

  @Test
  void logoutSuccessWhenEmpIdIsNull() throws Exception {
    given(jwtProperties.getAdminTokenName()).willReturn("token");
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(99L);

    // Mock BaseContext.getCurrentId() 返回null（未登录状态）
    try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
      baseContextMock.when(BaseContext::getCurrentId).thenReturn(null);

      // 执行请求
      mockMvc.perform(post("/admin/employee/logout")
              .header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(1))
          .andExpect(jsonPath("$.msg").value(nullValue()));

      // 验证Redis删除操作没有被调用（因为empId为null）
      verify(redisTemplate, never()).delete(anyString());
    }
  }

  @Test
  void logoutSuccessWhenRedisThrowsException() throws Exception {
    // 准备测试数据
    given(jwtProperties.getAdminTokenName()).willReturn("token");
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);
    long empId = 3L;
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(empId);

    Logger logger = (Logger) LoggerFactory.getLogger(EmployeeController.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    String redisKey = CacheConstant.REFRESH_TOKEN_KEY_PREFIX + empId;
    try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
      // Mock BaseContext.getCurrentId() 返回员工ID
      baseContextMock.when(BaseContext::getCurrentId).thenReturn(empId);

      // Mock Redis删除操作抛出异常
      given(redisTemplate.delete(redisKey))
          .willThrow(new RuntimeException("Redis连接失败"));

      // 执行请求 - 即使发生异常也应该返回成功
      mockMvc.perform(post("/admin/employee/logout")
              .header("token", "mock-accessToken"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(1))
          .andExpect(jsonPath("$.msg").value(nullValue()));

      // 验证Redis删除操作被调用
      verify(redisTemplate).delete(redisKey);
    } finally {
      logger.setLevel(originalLevel);
    }
  }

  @Test
  void refreshTokenSuccess() throws Exception {
    // 准备测试数据
    String refreshToken = "mock-refreshToken";
    RefreshTokenDto refreshTokenDto = new RefreshTokenDto();
    refreshTokenDto.setRefreshToken(refreshToken);

    // Mock JWT 相关配置
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
    given(jwtProperties.getAdminTtl()).willReturn(7200000L);

    // Mock JWT 解析返回 Claims
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt("mock-secret-key", refreshToken)).willReturn(claims);
    long empId = 1L;
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(String.valueOf(empId));

    // Mock Redis 获取存储的 Refresh Token
    String redisKey = CacheConstant.REFRESH_TOKEN_KEY_PREFIX + empId;
    given(redisTemplate.opsForValue()).willReturn(valueOperationsMock);
    String storedRefreshToken = "mock-refreshToken"; // Redis 中存储的 token
    given(valueOperationsMock.get(redisKey)).willReturn(storedRefreshToken);

    // Mock 生成新的 Access Token
    String newAccessToken = "new-mock-accessToken";
    given(jwtService.createJwt(eq("mock-secret-key"), eq(7200000L), anyMap()))
        .willReturn(newAccessToken);

    // 执行请求
    mockMvc.perform(post("/admin/employee/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshTokenDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.token").value(newAccessToken))
        .andExpect(jsonPath("$.data.refreshToken").value(refreshToken));

    // 验证方法调用
    verify(jwtService).parseJwt("mock-secret-key", refreshToken);
    verify(valueOperationsMock).get(redisKey);
    verify(jwtService).createJwt(eq("mock-secret-key"), eq(7200000L), anyMap());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "   ")
  void refreshTokenWhenTokenIsBlank(String refreshToken) throws Exception {
    RefreshTokenDto refreshTokenDto = new RefreshTokenDto();
    refreshTokenDto.setRefreshToken(refreshToken);

    // 执行请求
    mockMvc.perform(post("/admin/employee/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshTokenDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.msg").value("Refresh Token不能为空"));
  }

  @Test
  void refreshTokenWhenRedisTokenNotFound() throws Exception {
    // 准备测试数据
    String refreshToken = "mock-refreshToken";
    RefreshTokenDto refreshTokenDto = new RefreshTokenDto();
    refreshTokenDto.setRefreshToken(refreshToken);

    // Mock JWT 相关配置
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");

    // Mock JWT 解析返回 Claims
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt("mock-secret-key", refreshToken)).willReturn(claims);
    long empId = 2L;
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(String.valueOf(empId));

    // Mock Redis 中不存在 Refresh Token
    String redisKey = CacheConstant.REFRESH_TOKEN_KEY_PREFIX + empId;
    given(redisTemplate.opsForValue()).willReturn(valueOperationsMock);
    given(valueOperationsMock.get(redisKey)).willReturn(null);

    // 执行请求
    mockMvc.perform(post("/admin/employee/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshTokenDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.msg").value("Refresh Token已失效，请重新登录"));

    // 验证方法调用
    verify(jwtService).parseJwt("mock-secret-key", refreshToken);
    verify(valueOperationsMock).get(redisKey);
  }

  @Test
  void refreshTokenWhenTokenMismatch() throws Exception {
    // 准备测试数据
    String refreshToken = "mock-refreshToken";
    RefreshTokenDto refreshTokenDto = new RefreshTokenDto();
    refreshTokenDto.setRefreshToken(refreshToken);

    // Mock JWT 相关配置
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");

    // Mock JWT 解析返回 Claims
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt("mock-secret-key", refreshToken)).willReturn(claims);
    long empId = 3L;
    given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(String.valueOf(empId));

    // Mock Redis 获取存储的 Refresh Token（不匹配）
    String redisKey = CacheConstant.REFRESH_TOKEN_KEY_PREFIX + empId;
    given(redisTemplate.opsForValue()).willReturn(valueOperationsMock);
    String storedRefreshToken = "different-refreshToken"; // Redis 中存储的 token 不匹配
    given(valueOperationsMock.get(redisKey)).willReturn(storedRefreshToken);

    // 执行请求
    mockMvc.perform(post("/admin/employee/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshTokenDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.msg").value("Refresh Token无效，请重新登录"));

    // 验证方法调用
    verify(jwtService).parseJwt("mock-secret-key", refreshToken);
    verify(valueOperationsMock).get(redisKey);
  }

  @Test
  void refreshTokenWhenJwtParseFails() throws Exception {
    // 准备测试数据
    String refreshToken = "invalid-refreshToken";

    RefreshTokenDto refreshTokenDto = new RefreshTokenDto();
    refreshTokenDto.setRefreshToken(refreshToken);

    // Mock JWT 相关配置
    given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");

    // Mock JWT 解析抛出异常（token 无效或已过期）
    given(jwtService.parseJwt("mock-secret-key", refreshToken))
        .willThrow(new RuntimeException("JWT解析失败"));

    Logger logger = (Logger) LoggerFactory.getLogger(EmployeeController.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      // 执行请求
      mockMvc.perform(post("/admin/employee/refresh")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(refreshTokenDto)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0))
          .andExpect(jsonPath("$.msg").value("Refresh Token无效或已过期，请重新登录"));
    } finally {
      logger.setLevel(originalLevel);
    }

    // 验证方法调用
    verify(jwtService).parseJwt("mock-secret-key", refreshToken);
  }

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

  @Test
  void saveSuccess() throws Exception {
    // 准备测试数据
    EmployeeDto employeeDto = new EmployeeDto();
    employeeDto.setUsername("testUser");
    employeeDto.setName("测试用户");
    employeeDto.setPhone("13800138000");
    employeeDto.setSex("1");
    employeeDto.setIdNumber("110101199001011234");

    // 设置 JWT token Mock
    long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回成功
    Result<String> successResult = Result.success("新增员工成功");
    given(employeeService.save(any(EmployeeDto.class))).willReturn(successResult);

    // 执行请求
    mockMvc.perform(post("/admin/employee")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(employeeDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").value("新增员工成功"));

    // 验证方法调用
    verify(employeeService).save(any(EmployeeDto.class));
  }

  @Test
  void pageSuccess() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回分页结果
    List<Employee> employees = new ArrayList<>();
    Employee employee = Employee.builder()
        .id(1L)
        .username("testUser")
        .name("测试用户")
        .build();
    employees.add(employee);

    PageResult pageResult = new PageResult(1L, employees);
    given(employeeService.pageQuery(any(EmployeePageQueryDto.class))).willReturn(pageResult);

    // 执行请求
    mockMvc.perform(get("/admin/employee/page")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10")
            .param("name", "测试"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records[0].id").value(1))
        .andExpect(jsonPath("$.data.records[0].username").value("testUser"))
        .andExpect(jsonPath("$.data.records[0].name").value("测试用户"));

    // 验证方法调用
    verify(employeeService).pageQuery(any(EmployeePageQueryDto.class));
  }

  @Test
  void pageWithEmptyResult() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回空分页结果
    PageResult pageResult = new PageResult(0L, new ArrayList<>());
    given(employeeService.pageQuery(any(EmployeePageQueryDto.class))).willReturn(pageResult);

    // 执行请求
    mockMvc.perform(get("/admin/employee/page")
            .header("token", "mock-accessToken")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.total").value(0))
        .andExpect(jsonPath("$.data.records").isArray())
        .andExpect(jsonPath("$.data.records").isEmpty());

    // 验证方法调用
    verify(employeeService).pageQuery(any(EmployeePageQueryDto.class));
  }

  @Test
  void enableOrDisableEmployeeSuccess() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    long empId = 1L;
    setupJwtTokenMock(empId);

    // 执行请求
    int status = 1; // 启用
    long employeeId = 100L;
    mockMvc.perform(post("/admin/employee/status/{status}", status)
            .header("token", "mock-accessToken")
            .param("id", String.valueOf(employeeId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    // 验证方法调用
    verify(employeeService).enableOrDisable(status, employeeId);
  }

  @Test
  void enableOrDisableEmployeeDisable() throws Exception {
    // 准备测试数据
    // 设置 JWT token Mock
    long empId = 1L;
    setupJwtTokenMock(empId);

    // 执行请求
    int status = 0; // 禁用
    long employeeId = 100L;
    mockMvc.perform(post("/admin/employee/status/{status}", status)
            .header("token", "mock-accessToken")
            .param("id", String.valueOf(employeeId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    // 验证方法调用
    verify(employeeService).enableOrDisable(status, employeeId);
  }

  @Test
  void getByIdSuccess() throws Exception {
    // 准备测试数据
    long targetEmployeeId = 100L;
    Employee employee = Employee.builder()
        .id(targetEmployeeId)
        .username("testUser")
        .name("测试用户")
        .phone("13800138000")
        .sex("1")
        .idNumber("110101199001011234")
        .status(1)
        .password("encrypted-password")
        .build();

    // 设置 JWT token Mock
    long empId = 1L;
    setupJwtTokenMock(empId);

    // Mock Service 返回员工信息
    given(employeeService.getById(targetEmployeeId)).willReturn(employee);

    // 执行请求
    mockMvc.perform(get("/admin/employee/{id}", targetEmployeeId)
            .header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.id").value(targetEmployeeId))
        .andExpect(jsonPath("$.data.username").value("testUser"))
        .andExpect(jsonPath("$.data.name").value("测试用户"))
        .andExpect(jsonPath("$.data.phone").value("13800138000"))
        .andExpect(jsonPath("$.data.sex").value("1"))
        .andExpect(jsonPath("$.data.idNumber").value("110101199001011234"))
        .andExpect(jsonPath("$.data.status").value(1))
        .andExpect(jsonPath("$.data.password").value("****")); // 密码应该被隐藏

    // 验证方法调用
    verify(employeeService).getById(targetEmployeeId);
  }

  @Test
  void updateSuccess() throws Exception {
    // 准备测试数据
    EmployeeDto employeeDto = new EmployeeDto();
    employeeDto.setId(100L);
    employeeDto.setUsername("updateduser");
    employeeDto.setName("更新后的用户");
    employeeDto.setPhone("13900139000");
    employeeDto.setSex("0");
    employeeDto.setIdNumber("110101199001011111");

    // 设置 JWT token Mock
    long empId = 1L;
    setupJwtTokenMock(empId);

    // 执行请求
    mockMvc.perform(put("/admin/employee")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(employeeDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    // 验证方法调用
    verify(employeeService).update(any(EmployeeDto.class));
  }

  @Test
  void editPasswordSuccess() throws Exception {
    // 准备测试数据
    long empId = 1L;
    PasswordEditDto passwordEditDto = new PasswordEditDto();
    passwordEditDto.setEmpId(empId);
    passwordEditDto.setOldPassword("oldPassword123");
    passwordEditDto.setNewPassword("newPassword456");

    // 设置 JWT token Mock
    setupJwtTokenMock(empId);

    Logger logger = (Logger) LoggerFactory.getLogger(EmployeeController.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
    try {
      // 执行请求
      mockMvc.perform(put("/admin/employee/editPassword")
              .header("token", "mock-accessToken")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(passwordEditDto)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(1))
          .andExpect(jsonPath("$.msg").value(nullValue()));
    } finally {
      logger.setLevel(originalLevel);
    }

    // 验证方法调用
    verify(employeeService).editPassword(any(PasswordEditDto.class));
  }
}

package dev.kaiwen.interceptor;

import static dev.kaiwen.constant.MessageConstant.USER_NOT_LOGIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.result.Result;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;

/**
 * {@link JwtTokenUserInterceptor} 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenUserInterceptorTest {

  @Mock
  private JwtProperties jwtProperties;

  @Mock
  private JwtService jwtService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private ObjectMapper objectMapper;
  private JwtTokenUserInterceptor interceptor;

  private Level savedLogLevel;

  /**
   * 用于构造 HandlerMethod 的占位 Controller.
   */
  @SuppressWarnings("unused")
  public static class DummyHandler {

    /** 故意为空：仅用于构造 HandlerMethod，测试中不执行. */
    public void handle() {
      // no-op
    }
  }

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    interceptor = new JwtTokenUserInterceptor(jwtProperties, objectMapper, jwtService);
    // 单元测试中会触发 401/IOException 等，临时关闭拦截器日志，避免堆栈等大量输出
    Logger logger = (Logger) LoggerFactory.getLogger(JwtTokenUserInterceptor.class);
    savedLogLevel = logger.getLevel();
    logger.setLevel(Level.OFF);
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(JwtTokenUserInterceptor.class);
    logger.setLevel(savedLogLevel);
    BaseContext.removeCurrentId();
  }

  @Nested
  @DisplayName("preHandle - 非 Controller 方法")
  class PreHandleNonHandlerMethod {

    @Test
    void whenHandlerIsNotHandlerMethodThenReturnTrue() {
      Object handler = new Object();

      boolean result = interceptor.preHandle(request, response, handler);

      assertTrue(result);
    }
  }

  @Nested
  @DisplayName("preHandle - HandlerMethod")
  class PreHandleHandlerMethod {

    private HandlerMethod handlerMethod;

    @BeforeEach
    void createHandlerMethod() throws NoSuchMethodException {
      handlerMethod =
          new HandlerMethod(
              new DummyHandler(),
              DummyHandler.class.getDeclaredMethod("handle"));
    }

    @Test
    void whenTokenIsNullThenReturnFalseAndWrite401() throws IOException {
      given(jwtProperties.getUserTokenName()).willReturn("token");
      given(request.getHeader("token")).willReturn(null);

      StringWriter bodyWriter = new StringWriter();
      given(response.getWriter()).willReturn(new PrintWriter(bodyWriter));

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(401);
        verify(response).setContentType("application/json;charset=UTF-8");
        baseContext.verify(BaseContext::removeCurrentId);

        Result<?> parsed = objectMapper.readValue(bodyWriter.toString(), Result.class);
        assertEquals(0, parsed.getCode());
        assertEquals(USER_NOT_LOGIN, parsed.getMsg());
      }
    }

    @Test
    void whenTokenIsEmptyThenReturnFalseAndWrite401() throws IOException {
      given(jwtProperties.getUserTokenName()).willReturn("token");
      given(request.getHeader("token")).willReturn("");

      StringWriter bodyWriter = new StringWriter();
      given(response.getWriter()).willReturn(new PrintWriter(bodyWriter));

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(401);
        baseContext.verify(BaseContext::removeCurrentId);

        Result<?> parsed = objectMapper.readValue(bodyWriter.toString(), Result.class);
        assertEquals(USER_NOT_LOGIN, parsed.getMsg());
      }
    }

    @Test
    void whenTokenIsBlankThenReturnFalseAndWrite401() throws IOException {
      given(jwtProperties.getUserTokenName()).willReturn("token");
      given(request.getHeader("token")).willReturn("   ");

      StringWriter bodyWriter = new StringWriter();
      given(response.getWriter()).willReturn(new PrintWriter(bodyWriter));

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(401);
        baseContext.verify(BaseContext::removeCurrentId);

        Result<?> parsed = objectMapper.readValue(bodyWriter.toString(), Result.class);
        assertEquals(USER_NOT_LOGIN, parsed.getMsg());
      }
    }

    @Test
    void whenTokenValidThenSetCurrentIdAndReturnTrue() {
      given(jwtProperties.getUserTokenName()).willReturn("token");
      given(jwtProperties.getUserSecretKey()).willReturn("mock-secret-key");
      given(request.getHeader("token")).willReturn("valid-jwt-token");

      Claims claims = mock(Claims.class);
      given(claims.get(JwtClaimsConstant.USER_ID)).willReturn(100L);
      given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        baseContext.verify(() -> BaseContext.setCurrentId(100L));
      }
    }

    @Test
    void whenTokenValidWithStringUserIdThenSetCurrentIdAndReturnTrue() {
      given(jwtProperties.getUserTokenName()).willReturn("token");
      given(jwtProperties.getUserSecretKey()).willReturn("mock-secret-key");
      given(request.getHeader("token")).willReturn("valid-jwt-token");

      Claims claims = mock(Claims.class);
      given(claims.get(JwtClaimsConstant.USER_ID)).willReturn("200");
      given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        baseContext.verify(() -> BaseContext.setCurrentId(200L));
      }
    }

    @Test
    void whenJwtParseThrowsThenRemoveCurrentIdAndReturnFalseAndWrite401() throws IOException {
      given(jwtProperties.getUserTokenName()).willReturn("token");
      given(jwtProperties.getUserSecretKey()).willReturn("mock-secret-key");
      given(request.getHeader("token")).willReturn("invalid-token");
      given(jwtService.parseJwt(eq("mock-secret-key"), anyString()))
          .willThrow(new RuntimeException("JWT 解析失败"));

      StringWriter bodyWriter = new StringWriter();
      given(response.getWriter()).willReturn(new PrintWriter(bodyWriter));

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(401);
        baseContext.verify(BaseContext::removeCurrentId);

        Result<?> parsed = objectMapper.readValue(bodyWriter.toString(), Result.class);
        assertEquals(USER_NOT_LOGIN, parsed.getMsg());
      }
    }

    @Test
    void whenWriteUnauthorizedResponseGetWriterThrowsThenCatchAndReturnFalse()
        throws IOException {
      given(jwtProperties.getUserTokenName()).willReturn("token");
      given(request.getHeader("token")).willReturn(null);
      given(response.getWriter()).willThrow(new IOException("mock IO error"));

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(401);
        verify(response).setContentType("application/json;charset=UTF-8");
        baseContext.verify(BaseContext::removeCurrentId);
      }
    }
  }

  @Nested
  @DisplayName("afterCompletion")
  class AfterCompletion {

    @Test
    void afterCompletionRemovesCurrentId() {
      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        interceptor.afterCompletion(request, response, new Object(), null);

        baseContext.verify(BaseContext::removeCurrentId);
      }
    }

    @Test
    void afterCompletionWithExceptionStillRemovesCurrentId() {
      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        interceptor.afterCompletion(
            request, response, new Object(), new RuntimeException("handler error"));

        baseContext.verify(BaseContext::removeCurrentId);
      }
    }
  }
}

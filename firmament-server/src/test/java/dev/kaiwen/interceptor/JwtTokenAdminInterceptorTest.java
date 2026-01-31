package dev.kaiwen.interceptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

/**
 * {@link JwtTokenAdminInterceptor} 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenAdminInterceptorTest {

  @Mock
  private JwtProperties jwtProperties;

  @Mock
  private JwtService jwtService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private JwtTokenAdminInterceptor interceptor;

  /**
   * 用于构造 HandlerMethod 的占位 Controller.
   */
  @SuppressWarnings("unused")
  public static class DummyHandler {

    public void handle() {
      // 测试占位方法：仅用于构造 HandlerMethod，不执行任何逻辑
    }
  }

  @BeforeEach
  void setUp() {
    interceptor = new JwtTokenAdminInterceptor(jwtProperties, jwtService);
  }

  @AfterEach
  void tearDown() {
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void whenTokenMissingThenReturnFalseAndSet401(String token) {
      given(jwtProperties.getAdminTokenName()).willReturn("token");
      given(request.getHeader("token")).willReturn(token);

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(401);
        baseContext.verify(BaseContext::removeCurrentId);
      }
    }

    @Test
    void whenTokenValidThenSetCurrentIdAndReturnTrue() {
      given(jwtProperties.getAdminTokenName()).willReturn("token");
      given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
      given(request.getHeader("token")).willReturn("valid-jwt-token");

      Claims claims = mock(Claims.class);
      given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn(100L);
      given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        baseContext.verify(() -> BaseContext.setCurrentId(100L));
      }
    }

    @Test
    void whenTokenValidWithStringEmpIdThenSetCurrentIdAndReturnTrue() {
      given(jwtProperties.getAdminTokenName()).willReturn("token");
      given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
      given(request.getHeader("token")).willReturn("valid-jwt-token");

      Claims claims = mock(Claims.class);
      given(claims.get(JwtClaimsConstant.EMP_ID)).willReturn("200");
      given(jwtService.parseJwt(eq("mock-secret-key"), anyString())).willReturn(claims);

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        baseContext.verify(() -> BaseContext.setCurrentId(200L));
      }
    }

    @Test
    void whenJwtParseThrowsThenRemoveCurrentIdAndReturnFalseAndSet401() {
      given(jwtProperties.getAdminTokenName()).willReturn("token");
      given(jwtProperties.getAdminSecretKey()).willReturn("mock-secret-key");
      given(request.getHeader("token")).willReturn("invalid-token");
      given(jwtService.parseJwt(eq("mock-secret-key"), anyString()))
          .willThrow(new RuntimeException("JWT 解析失败"));

      try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).setStatus(401);
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

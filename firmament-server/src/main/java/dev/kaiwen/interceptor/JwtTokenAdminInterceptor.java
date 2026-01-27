package dev.kaiwen.interceptor;

import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT令牌校验的拦截器.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

  private final JwtProperties jwtProperties;
  private final JwtService jwtService;

  /**
   * 校验JWT.
   *
   * @param request HTTP请求对象
   * @param response HTTP响应对象
   * @param handler 处理器对象
   * @return 是否继续处理请求
   */
  @Override
  public boolean preHandle(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull Object handler) {
    // 判断当前拦截到的是Controller的方法还是其他资源
    if (!(handler instanceof HandlerMethod)) {
      // 当前拦截到的不是动态方法，直接放行
      return true;
    }

    // 1、从请求头中获取令牌
    String token = request.getHeader(jwtProperties.getAdminTokenName());

    // 2、校验令牌
    try {
      // 检查token是否为空
      if (token == null || token.trim().isEmpty()) {
        log.warn("JWT token为空");
        BaseContext.removeCurrentId();
        response.setStatus(401);
        return false;
      }

      // 只记录token的前10位，避免完整token泄露到日志
      log.info("jwt校验: {}...", token.substring(0, Math.min(10, token.length())));
      Claims claims = jwtService.parseJwt(jwtProperties.getAdminSecretKey(), token);
      Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
      log.info("当前员工id：{}", empId);
      BaseContext.setCurrentId(empId);
      // 3、通过，放行
      return true;
    } catch (Exception ex) {
      // 验证失败时也要清理（虽然这里可能没有设置，但为了安全）
      BaseContext.removeCurrentId();
      // 4、不通过，响应401状态码
      response.setStatus(401);
      return false;
    }
  }

  /**
   * 请求处理完成后清理 ThreadLocal，防止内存泄漏和数据污染.
   *
   * @param request HTTP请求对象
   * @param response HTTP响应对象
   * @param handler 处理器对象
   * @param ex 异常对象（如果有）
   */
  @Override
  public void afterCompletion(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull Object handler,
      @Nullable Exception ex) {
    // 清理 ThreadLocal，防止线程复用导致的数据污染和内存泄漏
    BaseContext.removeCurrentId();
  }
}

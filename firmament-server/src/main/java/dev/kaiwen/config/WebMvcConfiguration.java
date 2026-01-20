package dev.kaiwen.config;

import dev.kaiwen.interceptor.JwtTokenAdminInterceptor;
import dev.kaiwen.interceptor.JwtTokenUserInterceptor;
import dev.kaiwen.json.JacksonObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置类，注册web层相关组件.
 * 包括拦截器注册和消息转换器扩展.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
// 2. 关键修改：改为 implements WebMvcConfigurer
public class WebMvcConfiguration implements WebMvcConfigurer {

  private final JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
  private final JwtTokenUserInterceptor jwtTokenUserInterceptor;

  /**
   * 注册自定义拦截器.
   * 注册管理员和用户的JWT Token拦截器，配置拦截路径和排除路径.
   *
   * @param registry 拦截器注册表
   */
  @Override
  // 3. 关键修改：改为 public (因为接口方法默认是 public)
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    log.info("开始注册自定义拦截器...");
    registry.addInterceptor(jwtTokenAdminInterceptor)
        .addPathPatterns("/admin/**")
        .excludePathPatterns(
            "/admin/employee/login",     // 登录接口
            "/admin/employee/refresh"    // 刷新Token接口（允许过期token访问）
        );

    // 补充说明：
    // 因为你的拦截路径是 "/admin/**"，而 Swagger 的路径是 "/swagger-ui/**" 和 "/v3/api-docs"
    // 它们本来就不在拦截范围内，所以这里不需要特意 exclude Swagger 的路径。
    registry.addInterceptor(jwtTokenUserInterceptor)
        .addPathPatterns("/user/**")
        .excludePathPatterns("/user/user/login")
        .excludePathPatterns("/user/user/phoneLogin")
        .excludePathPatterns("/user/shop/status")
        .excludePathPatterns("/user/category/list")
        .excludePathPatterns("/user/dish/list")
        .excludePathPatterns("/user/setmeal/list")
        .excludePathPatterns("/user/setmeal/dish/**");
  }

  /**
   * 扩展 Spring MVC 框架的消息转换器.
   * 添加自定义的Jackson消息转换器，排除Swagger相关路径.
   *
   * @param converters 消息转换器列表
   */
  @Override
  public void extendMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
    log.info("扩展消息转换器...");

    // 创建自定义消息转换器，排除 Swagger 相关路径
    MappingJackson2HttpMessageConverter customConverter = new MappingJackson2HttpMessageConverter(
        new JacksonObjectMapper()) {
      @Override
      public boolean canWrite(@NonNull Class<?> clazz, @Nullable MediaType mediaType) {
        // 排除 String 类型和 Swagger 相关路径
        if (clazz == String.class || isSwaggerPath()) {
          return false;
        }
        return super.canWrite(clazz, mediaType);
      }
    };

    converters.add(0, customConverter);
  }

  /**
   * 判断当前请求是否为 Swagger 相关路径.
   *
   * @return 如果是Swagger路径返回true，否则返回false
   */
  private boolean isSwaggerPath() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        String path = attributes.getRequest().getRequestURI();
        return path != null
            && (path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars"));
      }
    } catch (Exception e) {
      // 忽略异常，返回 false 继续使用自定义转换器
    }
    return false;
  }
}

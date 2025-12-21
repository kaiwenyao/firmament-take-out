package dev.kaiwen.config;

import dev.kaiwen.interceptor.JwtTokenAdminInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 1. 注意这里变了

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
// 2. 关键修改：改为 implements WebMvcConfigurer
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    @Override
    // 3. 关键修改：改为 public (因为接口方法默认是 public)
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");

        // 补充说明：
        // 因为你的拦截路径是 "/admin/**"，而 Swagger 的路径是 "/swagger-ui/**" 和 "/v3/api-docs"
        // 它们本来就不在拦截范围内，所以这里不需要特意 exclude Swagger 的路径。
    }

    /**
     * 配置 OpenAPI (官方 Swagger)
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("苍穹外卖项目接口文档")
                        .version("2.0")
                        .description("基于 Spring Boot 3 重构的接口文档"));
    }

    // 4. 不需要手动写 addResourceHandlers 了！
    // 因为改用了 implements WebMvcConfigurer，
    // Spring Boot 会自动帮你把 Swagger UI 的静态资源映射好。
}
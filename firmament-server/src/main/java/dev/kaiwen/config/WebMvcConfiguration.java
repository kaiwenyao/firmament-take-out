package dev.kaiwen.config;

import dev.kaiwen.interceptor.JwtTokenAdminInterceptor;
import dev.kaiwen.json.JacksonObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 1. 注意这里变了

import java.util.List;

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
                .excludePathPatterns(
                        "/admin/employee/login"
                );

        // 补充说明：
        // 因为你的拦截路径是 "/admin/**"，而 Swagger 的路径是 "/swagger-ui/**" 和 "/v3/api-docs"
        // 它们本来就不在拦截范围内，所以这里不需要特意 exclude Swagger 的路径。
    }

    /**
     * 配置 OpenAPI (官方 Swagger)
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.0.0");
        openAPI.setInfo(new Info()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("基于 Spring Boot 3 重构的苍穹外卖接口文档"));
        return openAPI;
    }

    // 4. 不需要手动写 addResourceHandlers 了！
    // 因为改用了 implements WebMvcConfigurer，
    // Spring Boot 会自动帮你把 Swagger UI 的静态资源映射好。

    /**
     * 【黑科技】全局添加请求头参数
     * 这会让 Swagger 页面上的每一个接口，都自动多出一个 Header 输入框
     */
    @Bean
    public OperationCustomizer globalHeader() {
        return (operation, handlerMethod) -> {
            // 添加一个名为 "Custom-Header" 的请求头
            operation.addParametersItem(new Parameter()
                    .in("header")          // 参数位置：header
                    .name("token") // Header 的 key (你可以改，比如叫 device-id)
                    .description("全局自定义请求头(非必填)")
                    .required(false));     // 设为 false，不想填的时候可以空着

            // 如果你还想要第二个，就再加一段：
            /*
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("Another-Header")
                    .description("另一个头")
                    .required(false));
            */

            return operation;
        };
    }

    /**
     * 扩展 Spring MVC 框架的消息转换器
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");

        // ⚠️【核心修复】创建一个自定义的消息转换器，排除 Swagger 相关路径
        MappingJackson2HttpMessageConverter customConverter = new MappingJackson2HttpMessageConverter(new JacksonObjectMapper()) {
            @Override
            public boolean canWrite(Class<?> clazz, MediaType mediaType) {
                // 🛑 关键点 1：如果返回的是 String 类型，直接跳过
                if (clazz == String.class) {
                    return false;
                }

                // 🛑 关键点 2：检查当前请求路径，如果是 Swagger 相关路径，跳过
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        String requestPath = attributes.getRequest().getRequestURI();
                        // 排除所有 Swagger/OpenAPI 相关路径
                        if (requestPath != null && (
                                requestPath.startsWith("/v3/api-docs") ||
                                requestPath.startsWith("/swagger-ui") ||
                                requestPath.startsWith("/swagger-resources") ||
                                requestPath.startsWith("/webjars") ||
                                requestPath.equals("/doc.html")
                        )) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    // 如果获取请求路径失败，继续后续判断
                    log.debug("获取请求路径失败: {}", e.getMessage());
                }

                // 🛑 关键点 3：如果是 Swagger/OpenAPI 相关的类，也跳过
                if (clazz != null && clazz.getPackage() != null) {
                    String packageName = clazz.getPackageName();
                    if (packageName.startsWith("org.springdoc") ||
                            packageName.startsWith("io.swagger.v3") ||
                            packageName.startsWith("io.swagger.core")) {
                        return false;
                    }
                }

                // 其他情况（如 DishVO, EmployeeDTO）才由我们处理
                return super.canWrite(clazz, mediaType);
            }

            @Override
            public boolean canRead(Class<?> clazz, MediaType mediaType) {
                // 同样在读取时也排除 Swagger 相关路径
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        String requestPath = attributes.getRequest().getRequestURI();
                        if (requestPath != null && (
                                requestPath.startsWith("/v3/api-docs") ||
                                requestPath.startsWith("/swagger-ui") ||
                                requestPath.startsWith("/swagger-resources") ||
                                requestPath.startsWith("/webjars") ||
                                requestPath.equals("/doc.html")
                        )) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    log.debug("获取请求路径失败: {}", e.getMessage());
                }
                return super.canRead(clazz, mediaType);
            }
        };

        // 将自定义转换器加到第一位
        converters.add(0, customConverter);
    }
}
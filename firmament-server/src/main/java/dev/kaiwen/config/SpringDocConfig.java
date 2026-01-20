package dev.kaiwen.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc配置类.
 * 用于配置Swagger/OpenAPI文档，包括API信息和安全认证配置.
 */
@Configuration
public class SpringDocConfig {

  /**
   * 配置自定义OpenAPI文档.
   * 设置API文档标题、版本、描述，并配置全局Token安全认证.
   *
   * @return OpenAPI配置对象
   */
  @Bean
  public OpenAPI customOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("苍穹外卖项目接口文档")
            .version("2.0")
            .description("基于 Spring Boot 3 + Springdoc 的外卖项目接口文档"))

        // 1. 添加全局安全校验项（让右上角的锁头生效）
        .addSecurityItem(new SecurityRequirement().addList("GlobalToken"))

        .components(new Components()
            // 2. 配置具体的 Token 模式
            .addSecuritySchemes("GlobalToken",
                new SecurityScheme()
                    // 关键点：苍穹外卖用的是自定义 Header，不是标准的 HTTP Bearer
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("token") // 这里填你后端拦截器里读取的 header 名字
            ));
  }

  // 删除了 globalHeader 方法，因为上面的配置已经涵盖了它的功能，而且更高级。
}
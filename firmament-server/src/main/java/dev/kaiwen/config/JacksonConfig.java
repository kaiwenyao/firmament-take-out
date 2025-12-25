package dev.kaiwen.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置
 * 解决 JavaScript 中 Long 类型大数精度丢失问题
 * 将 Long 类型序列化为字符串
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 把 Long 类型序列化为 String
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            // 把 Long 基本类型也序列化为 String
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}


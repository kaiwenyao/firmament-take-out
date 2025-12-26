package dev.kaiwen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 应用上下文加载测试
 * 
 * 作用：
 * 1. 验证 Spring Boot 应用能否正常启动
 * 2. 检查所有 Bean 是否能正确初始化
 * 3. 验证配置是否正确
 * 
 * 注意：
 * - 使用 RANDOM_PORT 启动真实的 Tomcat 服务器
 * - 这是必需的，因为 WebSocket 需要 ServerContainer（只有真实 Servlet 容器才提供）
 * - contextLoads() 方法体为空是正常的，它的作用就是验证 Spring 上下文能否正常加载
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@ComponentScan(basePackages = "dev.kaiwen")
class FirmamentServerApplicationTests {

    @Test
    void contextLoads() {
        // 验证应用上下文加载
        // 如果 Spring 上下文无法加载，此测试会失败
    }

}

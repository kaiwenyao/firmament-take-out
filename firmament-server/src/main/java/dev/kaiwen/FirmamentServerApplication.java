package dev.kaiwen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;
@Slf4j
@EnableTransactionManagement
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class FirmamentServerApplication {

    public static void main(String[] args) throws UnknownHostException {


        // 获取 Spring Boot 上下文
        ConfigurableApplicationContext application = SpringApplication.run(FirmamentServerApplication.class, args);

        // 获取环境变量
        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "8080"); // 默认为 8080
        // 处理 context-path (如果没配就是空字符串)
        String path = env.getProperty("server.servlet.context-path", "");
        
        // 检查 Swagger 是否启用
        boolean swaggerEnabled = Boolean.parseBoolean(
                env.getProperty("springdoc.swagger-ui.enabled", "false"));

        // 构建日志信息
        StringBuilder logInfo = new StringBuilder();
        logInfo.append("\n----------------------------------------------------------\n\t");
        logInfo.append("Application Firmament-Server is running! Access URLs:\n\t");
        logInfo.append("Local: \t\thttp://localhost:").append(port).append(path).append("/\n\t");
        logInfo.append("External: \thttp://").append(ip).append(":").append(port).append(path).append("/\n\t");
        
        // 只有在 Swagger 启用时才显示 Swagger 地址
        if (swaggerEnabled) {
            logInfo.append("Swagger文档: \thttp://localhost:").append(port).append(path).append("/swagger-ui/index.html\n\t");
            logInfo.append("OpenAPI JSON: \thttp://localhost:").append(port).append(path).append("/v3/api-docs\n\t");
        } else {
            logInfo.append("Swagger文档: \t已关闭（生产环境安全考虑）\n\t");
        }
        
        logInfo.append("----------------------------------------------------------");
        log.info(logInfo.toString());
    }

}

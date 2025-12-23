package dev.kaiwen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;
@Slf4j
@EnableTransactionManagement
@SpringBootApplication
public class FirmamentServerApplication {

    public static void main(String[] args) throws UnknownHostException {

//        SpringApplication.run(FirmamentServerApplication.class, args);

        // 获取 Spring Boot 上下文
        ConfigurableApplicationContext application = SpringApplication.run(FirmamentServerApplication.class, args);

        // 获取环境变量
        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "8080"); // 默认为 8080
        // 处理 context-path (如果没配就是空字符串)
        String path = env.getProperty("server.servlet.context-path", "");

        // 打印提示信息
        log.info("\n----------------------------------------------------------\n\t" +
                "Application Firmament-Server is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:" + port + path + "/\n\t" +
                "External: \thttp://" + ip + ":" + port + path + "/\n\t" +
                "Swagger文档: \thttp://localhost:" + port + path + "/swagger-ui/index.html\n\t" +
                "OpenAPI JSON: \thttp://localhost:" + port + path + "/v3/api-docs\n" +
                "----------------------------------------------------------");
    }

}

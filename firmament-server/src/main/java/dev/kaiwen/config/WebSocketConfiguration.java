package dev.kaiwen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类，用于注册WebSocket的Bean.
 */
@Configuration
public class WebSocketConfiguration {

  /**
   * 注册WebSocket端点导出器.
   * 用于扫描和注册使用@ServerEndpoint注解的WebSocket端点.
   *
   * @return ServerEndpointExporter实例
   */
  @Bean
  public ServerEndpointExporter serverEndpointExporter() {
    return new ServerEndpointExporter();
  }

}

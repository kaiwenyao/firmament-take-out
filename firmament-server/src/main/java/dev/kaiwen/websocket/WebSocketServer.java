package dev.kaiwen.websocket;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket服务.
 * 用于处理WebSocket连接、消息接收和群发消息功能.
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

  // 存放会话对象（使用ConcurrentHashMap保证线程安全）
  private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

  /**
   * 连接建立成功调用的方法.
   *
   * @param session WebSocket会话对象
   * @param sid     客户端标识
   */
  @OnOpen
  @SuppressWarnings("unused")
  public void onOpen(Session session, @PathParam("sid") String sid) {
    log.info("客户端：{} 建立连接", sid);
    sessionMap.put(sid, session);
  }

  /**
   * 收到客户端消息后调用的方法.
   *
   * @param message 客户端发送过来的消息
   * @param sid     客户端标识
   */
  @OnMessage
  @SuppressWarnings("unused")
  public void onMessage(String message, @PathParam("sid") String sid) {
    log.info("收到来自客户端：{} 的信息: {}", sid, message);
  }

  /**
   * 连接关闭调用的方法.
   *
   * @param sid 客户端标识
   */
  @OnClose
  @SuppressWarnings({"unused"})
  public void onClose(@PathParam("sid") String sid) {
    log.info("连接断开: {}", sid);
    Session session = sessionMap.remove(sid);
    // Session由WebSocket容器管理，不需要手动关闭
    if (session != null && session.isOpen()) {
      try {
        session.close();
      } catch (Exception e) {
        log.warn("关闭Session失败", e);
      }
    }
  }

  /**
   * 群发消息给所有客户端.
   *
   * @param message 要发送的消息内容
   */
  public void sendToAllClient(String message) {
    Collection<Session> sessions = sessionMap.values();
    for (Session session : sessions) {
      try {
        // 服务器向客户端发送消息
        session.getBasicRemote().sendText(message);
      } catch (Exception e) {
        log.error("向客户端发送消息失败", e);
      }
    }
  }

}

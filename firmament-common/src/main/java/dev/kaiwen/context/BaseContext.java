package dev.kaiwen.context;

/**
 * 基于ThreadLocal封装工具类，用于保存和获取当前登录用户id.
 */
public class BaseContext {

  public static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

  /**
   * 设置当前登录用户id.
   *
   * @param id 用户id
   */
  public static void setCurrentId(Long id) {
    THREAD_LOCAL.set(id);
  }

  /**
   * 获取当前登录用户id.
   *
   * @return 用户id
   */
  public static Long getCurrentId() {
    return THREAD_LOCAL.get();
  }

  /**
   * 移除当前登录用户id.
   */
  public static void removeCurrentId() {
    THREAD_LOCAL.remove();
  }

  private BaseContext() {
    // 工具类，禁止实例化
  }
}

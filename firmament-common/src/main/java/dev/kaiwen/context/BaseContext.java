package dev.kaiwen.context;

/**
 * 基于ThreadLocal封装工具类，用于保存和获取当前登录用户id.
 */
public class BaseContext {

  public static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

  /**
   * 设置当前登录用户id.
   *
   * @param id 用户id
   */
  public static void setCurrentId(Long id) {
    threadLocal.set(id);
  }

  /**
   * 获取当前登录用户id.
   *
   * @return 用户id
   */
  public static Long getCurrentId() {
    return threadLocal.get();
  }

  /**
   * 移除当前登录用户id.
   */
  public static void removeCurrentId() {
    threadLocal.remove();
  }

  private BaseContext() {
    // 工具类，禁止实例化
  }
}

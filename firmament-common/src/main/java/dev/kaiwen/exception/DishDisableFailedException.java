package dev.kaiwen.exception;

/**
 * 菜品停售失败异常.
 */
public class DishDisableFailedException extends BaseException {

  /**
   * 构造菜品停售失败异常.
   *
   * @param msg 异常消息
   */
  public DishDisableFailedException(String msg) {
    super(msg);
  }
}


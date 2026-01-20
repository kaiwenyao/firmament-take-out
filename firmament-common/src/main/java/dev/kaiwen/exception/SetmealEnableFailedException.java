package dev.kaiwen.exception;

/**
 * 套餐启用失败异常.
 */
public class SetmealEnableFailedException extends BaseException {

  /**
   * 构造套餐启用失败异常.
   *
   * @param msg 异常消息
   */
  public SetmealEnableFailedException(String msg) {
    super(msg);
  }
}

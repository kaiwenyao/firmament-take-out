package dev.kaiwen.exception;

/**
 * 业务异常.
 */
public class BaseException extends RuntimeException {

  /**
   * 构造业务异常.
   */
  public BaseException() {
  }

  /**
   * 构造业务异常.
   *
   * @param msg 异常消息
   */
  public BaseException(String msg) {
    super(msg);
  }

}

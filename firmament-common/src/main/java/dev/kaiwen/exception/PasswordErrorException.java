package dev.kaiwen.exception;

/**
 * 密码错误异常.
 */
public class PasswordErrorException extends BaseException {

  /**
   * 构造密码错误异常.
   *
   * @param msg 异常消息
   */
  public PasswordErrorException(String msg) {
    super(msg);
  }

}

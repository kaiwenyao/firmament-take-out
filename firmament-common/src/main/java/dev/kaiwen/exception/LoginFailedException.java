package dev.kaiwen.exception;

/**
 * 登录失败异常.
 */
public class LoginFailedException extends BaseException {

  /**
   * 构造登录失败异常.
   *
   * @param msg 异常消息
   */
  public LoginFailedException(String msg) {
    super(msg);
  }
}

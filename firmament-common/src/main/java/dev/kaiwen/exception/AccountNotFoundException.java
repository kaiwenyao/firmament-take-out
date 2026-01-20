package dev.kaiwen.exception;

/**
 * 账号不存在异常.
 */
public class AccountNotFoundException extends BaseException {

  /**
   * 构造账号不存在异常.
   *
   * @param msg 异常消息
   */
  public AccountNotFoundException(String msg) {
    super(msg);
  }

}

package dev.kaiwen.exception;

/**
 * 账号被锁定异常.
 */
public class AccountLockedException extends BaseException {

  /**
   * 构造账号被锁定异常.
   *
   * @param msg 异常消息
   */
  public AccountLockedException(String msg) {
    super(msg);
  }

}

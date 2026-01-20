package dev.kaiwen.exception;

/**
 * 密码修改失败异常.
 */
public class PasswordEditFailedException extends BaseException {

  /**
   * 构造密码修改失败异常.
   *
   * @param msg 异常消息
   */
  public PasswordEditFailedException(String msg) {
    super(msg);
  }

}

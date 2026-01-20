package dev.kaiwen.exception;

/**
 * 删除不允许异常.
 */
public class DeletionNotAllowedException extends BaseException {

  /**
   * 构造删除不允许异常.
   *
   * @param msg 异常消息
   */
  public DeletionNotAllowedException(String msg) {
    super(msg);
  }

}

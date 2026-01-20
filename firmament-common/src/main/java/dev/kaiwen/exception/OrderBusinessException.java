package dev.kaiwen.exception;

/**
 * 订单业务异常.
 */
public class OrderBusinessException extends BaseException {

  /**
   * 构造订单业务异常.
   *
   * @param msg 异常消息
   */
  public OrderBusinessException(String msg) {
    super(msg);
  }

}

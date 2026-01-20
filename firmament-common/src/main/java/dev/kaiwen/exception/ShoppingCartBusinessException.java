package dev.kaiwen.exception;

/**
 * 购物车业务异常.
 */
public class ShoppingCartBusinessException extends BaseException {

  /**
   * 构造购物车业务异常.
   *
   * @param msg 异常消息
   */
  public ShoppingCartBusinessException(String msg) {
    super(msg);
  }

}

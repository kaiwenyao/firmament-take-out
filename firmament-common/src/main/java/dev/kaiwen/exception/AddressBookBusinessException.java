package dev.kaiwen.exception;

/**
 * 地址簿业务异常.
 */
public class AddressBookBusinessException extends BaseException {

  /**
   * 构造地址簿业务异常.
   *
   * @param msg 异常消息
   */
  public AddressBookBusinessException(String msg) {
    super(msg);
  }

}

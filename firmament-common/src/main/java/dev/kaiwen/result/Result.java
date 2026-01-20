package dev.kaiwen.result;

import lombok.Data;

/**
 * 后端统一返回结果.
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> {

  // 编码：1成功，0和其它数字为失败
  private Integer code;
  // 错误信息
  private String msg;
  // 数据
  private T data;

  /**
   * 成功响应，无返回数据.
   *
   * @param <T> 数据类型
   * @return 成功结果
   */
  public static <T> Result<T> success() {
    Result<T> result = new Result<>();
    result.code = 1;
    return result;
  }

  /**
   * 成功响应，返回数据.
   *
   * @param <T>    数据类型
   * @param object 返回的数据
   * @return 成功结果
   */
  public static <T> Result<T> success(T object) {
    Result<T> result = new Result<>();
    result.data = object;
    result.code = 1;
    return result;
  }

  /**
   * 错误响应.
   *
   * @param <T> 数据类型
   * @param msg 错误信息
   * @return 错误结果
   */
  public static <T> Result<T> error(String msg) {
    Result<T> result = new Result<>();
    result.msg = msg;
    result.code = 0;
    return result;
  }

}

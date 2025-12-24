package dev.kaiwen.exception;

/**
 * 菜品停售失败异常
 */
public class DishDisableFailedException extends BaseException {

    public DishDisableFailedException(){}

    public DishDisableFailedException(String msg){
        super(msg);
    }
}


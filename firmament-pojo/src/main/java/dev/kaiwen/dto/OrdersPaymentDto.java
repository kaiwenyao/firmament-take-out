package dev.kaiwen.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrdersPaymentDto implements Serializable {
    //订单号
    private String orderNumber;

    //付款方式
    private Integer payMethod;

}

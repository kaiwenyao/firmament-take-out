package dev.kaiwen.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ShoppingCartDto implements Serializable {

    private Long dishId;
    private Long setmealId;
    private String dishFlavor;

}

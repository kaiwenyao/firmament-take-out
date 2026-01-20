package dev.kaiwen.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户登录
 */
@Data
public class UserLoginDto implements Serializable {

    private String code;

}

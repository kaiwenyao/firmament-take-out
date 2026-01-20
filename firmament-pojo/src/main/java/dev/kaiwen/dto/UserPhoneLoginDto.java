package dev.kaiwen.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户手机号密码登录DTO
 */
@Data
public class UserPhoneLoginDto implements Serializable {

    //手机号
    private String phone;

    //密码
    private String password;

}


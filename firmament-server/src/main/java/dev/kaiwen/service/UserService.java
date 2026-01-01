package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.UserLoginDTO;
import dev.kaiwen.dto.UserPhoneLoginDTO;
import dev.kaiwen.entity.User;
import dev.kaiwen.vo.UserInfoVO;

public interface UserService extends IService<User> {


    User wxLogin(UserLoginDTO userLoginDTO);

    /**
     * 用户手机号密码登录
     * @param userPhoneLoginDTO 手机号和密码
     * @return 用户信息
     */
    User phoneLogin(UserPhoneLoginDTO userPhoneLoginDTO);

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    UserInfoVO getUserInfo();
}


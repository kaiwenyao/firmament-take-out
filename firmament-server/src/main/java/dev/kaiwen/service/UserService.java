package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.UserLoginDTO;
import dev.kaiwen.entity.User;

public interface UserService extends IService<User> {


    User wxLogin(UserLoginDTO userLoginDTO);
}


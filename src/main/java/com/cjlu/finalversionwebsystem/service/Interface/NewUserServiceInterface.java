package com.cjlu.finalversionwebsystem.service.Interface;

import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.entity.User;

public interface NewUserServiceInterface {
    //用户登录
    Result login(String userName, String password);
    //用户注册
    Result register(User user);
}

package com.cjlu.finalversionwebsystem.service.Interface;


import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.entity.User;

public interface UserServiceInterface
{
    //用户登录
    Result login(String userName,String password);
    //用户注册
    Result register(User user, String confirmPassword);
    //用户登出
    Result logout(String token);
    //刷新JWT令牌
    Result  refreshToken(String token);
    //获取当前登录用户信息
    Result getUserInfo(String token);
    //修改用户密码
    Result changePassword(String userName, String email, String newPassword);
    //通过邮箱重置用户密码
    Result resetPassword(String email,String userName);
    //验证用户权限
    Result verifyPermission(String token, String requiredPermission);
    //检查用户名是否已存在
    Result checkUserNameExists(String userName);
    //检查邮箱是否已注册
    Result checkEmailExists(String email);
    //发送邮箱验证码
    Result sendVerificationCode(String email);
    //验证邮箱验证码
    Result verifyEmailCode(String email, String code);
}

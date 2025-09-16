package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.entity.User;
import com.cjlu.finalversionwebsystem.mapper.UserMapper;
import com.cjlu.finalversionwebsystem.service.Interface.NewUserServiceInterface;
import com.cjlu.finalversionwebsystem.utils.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NewUserServiceImpl implements NewUserServiceInterface {

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result login(String userName, String password) {
        try {
            // 从数据库中获取密码
            String storedPassword = userMapper.findPasswordByUsername(userName);

            // 比较密码
            if (storedPassword != null && storedPassword.equals(password)) {
                log.info("用户登录成功: {}", userName);

                // 生成token
                String token = JWTUtils.createToken(userName, null);

                // 返回带有token的数据
                return Result.success(token);
            } else {
                log.warn("用户登录失败: 用户名或密码错误: {}", userName);
                return Result.error("用户名或密码错误");
            }
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage(), e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    @Override
    public Result register(User user) {
        try {
            // 检查用户名是否已存在
            if (userMapper.checkIfUsernameExists(user.getUserName())) {
                log.warn("用户已存在: {}", user.getUserName());
                return Result.error("用户已存在");
            }

            // 保存用户信息到数据库
            userMapper.insertUser(user);
            log.info("用户注册成功: {}", user.getUserName());
            return Result.success("注册成功");
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage(), e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }
}

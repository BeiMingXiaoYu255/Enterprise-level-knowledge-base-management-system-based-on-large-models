package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.utils.EmailVerificationCodeUtils;
import com.cjlu.finalversionwebsystem.service.Interface.EmailVerificationCodeCacheServiceInterface;
import com.cjlu.finalversionwebsystem.service.Interface.EmailVerificationCodeServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 导入 Map 类
import java.util.Map;

// 标记该类为 REST 控制器
@RestController
// 设置请求映射路径
@RequestMapping("/verificationCode")
public class EmailVerificationCodeController {
    // 自动注入 EmailVerificationCodeServiceInterface 实例
    @Autowired
    private EmailVerificationCodeServiceInterface emailService;

    // 自动注入 EmailVerificationCodeCacheServiceInterface 实例
    @Autowired
    private EmailVerificationCodeCacheServiceInterface codeCache;

    // 处理发送验证码的 POST 请求
    @PostMapping("/sendCode")
    public Result sendVerificationCode(@RequestBody Map<String, Object> request) {
        // 从请求体中获取邮箱地址
        String email = (String) request.get("email");

        // 生成6位验证码
        String code = EmailVerificationCodeUtils.generateCode(6);

        try {
            // 发送验证码到指定邮箱
            emailService.sendVerificationCode(email, code);
            // 将验证码保存到缓存中
            codeCache.saveCode(email, code);
        } catch (Exception e) {
            // 捕获异常并打印堆栈跟踪
            e.printStackTrace();
            // 返回错误结果
            return Result.error("验证码发送失败");
        }
        // 返回成功结果
        return Result.success();
    }

    // 处理验证验证码的 POST 请求
    @PostMapping("/verifyCode")
    public Result verifyCode(@RequestBody Map<String, Object> request) {
        // 从请求体中获取邮箱地址
        String email = (String) request.get("email");
        // 从请求体中获取验证码
        String code = (String) request.get("code");

        // 验证验证码是否正确
        if (codeCache.validateCode(email, code)) {
            // 如果验证码正确，返回成功结果
            return Result.success();
        } else {
            // 如果验证码不正确或已过期，返回错误结果
            return Result.error("验证码错误或者已过期");
        }
    }
}

